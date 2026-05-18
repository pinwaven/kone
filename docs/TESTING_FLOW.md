# Kone (Kino-One) Core Testing Flow & Logic Documentation

This document describes the complete testing process, business logic, and backend interactions of the Kone biomarker analyzer. It is intended to serve as a blueprint for porting the system to other hardware platforms (e.g., ESP32/STM32).

---

## 1. System Architecture Overview

The Kone application is a Kiosk-mode Android app built with **Jetpack Compose** and **Kotlin**.

- **UI Layer:** Manages user input, scanning progress, and result visualization.
- **Orchestration Layer (`WorkMainViewModel`):** Controls the state machine of the testing process.
- **Execution Layer (`WorkFlowV2`):** Defines a sequence of hardware and UI actions based on the reagent card type.
- **Hardware Layer (`SerialPort` & `CtlCommandsV2`):** Handles serial communication with the underlying hardware.
- **Service Layer (`BAAService` & `AppCardUtils`):** Performs signal processing, result calculation, and biological age analysis.
- **Backend Integration (`FrosApi` & `SbEdgeFunc`):** Manages patient reports, card configurations, and synchronization.

---

## 2. Testing Lifecycle (State Machine)

The testing flow is managed by the `action` state in `WorkMainViewModel`.

### Phase 1: Preparation & Identification
1.  **State `ACTION_CASE_SAMPLE`:** Initial state. Prompt user to prepare the sample and insert the reagent card.
2.  **Card Detection:** The system polls GPIO (`gpioRead`) to detect if a card is inserted.
3.  **QR Scanning:** Once detected, the system triggers the hardware QR scanner (`readQR`).
4.  **Metadata Acquisition:**
    - The QR code is sent to the backend (`SbEdgeFunc.getCardInfo`) to retrieve the **Reagent Card Configuration** (`CardConfig`).
    - Patient information and existing reports are fetched from `FrosApi.getPatientCaseReport`.
5.  **Workflow Generation:** A `WorkFlowV2` sequence is generated based on the test type (e.g., CRP, IGE, BAA).

### Phase 2: Reaction & Scanning (Hardware Execution)
The `WorkFlowV2` executes a series of actions (UI waits or Hardware commands):
1.  **Homing:** Reset the tray position.
2.  **Reaction Wait (`ACTION_CASE_WAIT`):** UI shows a countdown for the initial reaction (e.g., `ft0` time).
3.  **Suction (`absorb`):** Hardware opens the suction valve for a specified time (`xt1`).
4.  **Scanning (`scan`):** Hardware moves the laser/sensor over the card. The scanning duration is typically 16 seconds.
5.  **Data Retrieval (`queryData`):** After scanning, the raw ADC sampling data is pulled from the hardware buffer.

### Phase 3: Processing & Finalization
1.  **Data Parsing:** Raw data is parsed into 13-bit ADC values (see Section 4).
2.  **Result Calculation:** `AppCardUtils` calculates the peak areas and applies polynomial formulas to determine the biomarker concentration.
3.  **BAA Analysis (Optional):** If the test is for Biological Age, `BAAService` is used to calculate the "Physiological Age Acceleration" based on multiple biomarker inputs.
4.  **Report Generation:** Results are saved to the local Room database and uploaded to the server (`FrosApi.uploadPatientReportDataToServer`).
5.  **State `ACTION_REPORT1`:** Display the final results to the user.

---

## 3. Hardware Communication Protocol

### Low-Level Serial Frame Structure
For ESP32/STM32 porting, the serial protocol follows a specific framing format:

| Field | Size | Description |
| :--- | :--- | :--- |
| **Header** | 1 byte | Fixed value `0x21`. |
| **Command** | 1 byte | Command identifier (e.g., `CMD_SCAN`, `CMD_ABSORB`). |
| **Payload** | Variable | String-encoded parameters (e.g., `SID=123,SPEED=600`). |
| **Checksum** | 1 byte | **CRC-8** calculated over [Header + Command + Payload]. |

### Core Commands (`CtlCommandsV2`)
| Command | Purpose |
| :--- | :--- |
| `homing()` | Moves the tray to the home/eject position. |
| `moveToSs(pos, speed, mode)` | Moves the tray to a specific internal position. |
| `gpioRead()` | Reads hardware pin status (e.g., card sensor). |
| `readQR()` | Triggers the QR/Barcode scanner. |
| `absorb(timeMs)` | Opens the suction valve for a defined duration. |
| `scan(start, duration)` | Starts the laser scanning process. |
| `queryData()` | Retrieves the sampled ADC data (binary). |

---

## 4. Signal Processing & Data Parsing

### Raw Data Format (ADC Sampling)
The hardware returns a binary packet with a 10-byte header:
1.  `dataLen` (2 bytes, Little Endian): Number of ADC data points.
2.  `laserCurr` (2 bytes): Raw laser current.
3.  `fixed5a` (1 byte): `0x5A`.
4.  `dataBias` (2 bytes): ADC baseline bias.
5.  `laserCurrBias` (2 bytes): Laser current bias.
6.  `fixedA5` (1 byte): `0xA5`.

**ADC Values:** Following the header are `dataLen` points. Each point is a 2-byte integer representing a 13-bit ADC value.

### Calculation Logic (`AppCardUtils`)
1.  **Peak Refinement (`findRealMinAndMaxTopList`):** The system doesn't just use the static windows from `CardConfig`. It performs a refinement to find the true boundaries of the biological signal:
    - **Start Boundary Search:** From the config `start` index, move backwards while the signal is increasing (searching for the local minimum).
    - **End Boundary Search:** From the config `end` index, move forwards while the signal is increasing.
    - **Stability Check:** It uses a "gradient counter" (default 3-5 points) to ignore minor noise fluctuations and ensure it has reached a stable baseline.
2.  **Peak Area:** Calculate the integral of the signal above the baseline within the refined start/end bounds.
3.  **Slope Check:** Verify the validity of the peak by calculating the slope to ensure it's not noise.
4.  **Formula Application:**
    - **Polynomial:** $Result = \sum (coeff_n \cdot x^n)$ where $x$ is the calculated area or ratio.
    - **Logarithmic:** Used for certain tests like CRP.
    - **Ratio:** $T/C$ ratio (Test Peak / Control Peak) is often used to normalize results.

---

## 5. Biological Age Analysis (BAAService)

For tests categorized as Biological Age Analysis (BAA), the system calculates a "Physiological Age Acceleration" score.

### Calculation Principle
The Biological Age is derived from the Chronological Age and a calculated "Acceleration" value (BAA).
$$BiologicalAge = ChronologicalAge + BAA$$

### BAA Formula
The BAA is a weighted sum of normalized biomarker values:
$$BAA = \sum_{i=1}^{n} (coeffENET_i - coeffSexAge_i) \cdot (Value_i - Mean_i) \cdot 10$$
Where:
- $coeffENET_i$: Elastic Net coefficient for biomarker $i$.
- $coeffSexAge_i$: Sex-Age interaction coefficient.
- $Value_i$: The measured value (processed if `useLogValue` is true).
- $Mean_i$: Population mean for biomarker $i$.

### Key Biomarkers Supported
The system supports 25+ biomarkers, including:
- **Albumin:** (Mean: 45.12, ENET: -0.0113)
- **Cystatin C:** (Mean: 0.90, ENET: 1.8595)
- **HbA1c:** (Mean: 35.47, ENET: 0.0181)
- **CRP:** (Log-normalized, Mean: 0.30, ENET: 0.0791)
- **Glucose:** (Mean: 4.95, ENET: 0.0321)

---

## 6. End-to-End API Sequence

The following sequence must be followed for a valid test session:

1.  **Device Activation (Once):** `POST business/poct/device/activate` (SbEdge).
2.  **Card Identification:** `POST api/service/poct/device/queryByCode` (Fros) using the scanned QR code to get patient info.
3.  **Fetch Specs:** `POST business/poct/card/getInfo` (SbEdge) using QR code to get `CardConfig` (peaks, formulas).
4.  **Set Status:** `POST business/poct/card/updateStatus` (SbEdge) to `checking`.
5.  **Scan & Calculate:** (Hardware & Local Algorithm Phase).
6.  **Upload Results:** `POST api/service/poct/device/uploadCheckData` (Fros).
7.  **Finalize Status:** `POST business/poct/card/updateStatus` (SbEdge) to `success` or `failed`.
8.  **BAA Result (Optional):** `POST business/poct/baa/result` (SbEdge) for bio-age computation.

---

## 7. Backend API Reference & JSON Schemas

### Fros API
**Base URL:** `https://fros-api.gyyyhospital.com`

#### `queryByCode` (Get Patient/Report Info)
- **Request:** `{"code": "QR_STRING"}`
- **Key Response Fields:** `result` (list of previous results), `patient` (object with `objectId`, `name_`, `gender`, `birthDate`), `type` (e.g., "ige").

#### `uploadCheckData` (Upload Results)
- **Request Schema:**
  ```json
  {
    "code": "QR_STRING",
    "status": "completed",
    "date": "YYYY-MM-DD HH:mm:ss",
    "result": [
      {
        "name": "Item Name",
        "result": "Final Value (e.g., 5.2)",
        "radioValue": "Ratio if applicable",
        "refer": "Reference Range",
        "t1Value": "Raw Area T1",
        "cValue": "Raw Area C",
        "t1ValueName": "T1",
        "t1ValueStr": "Display String"
      }
    ]
  }
  ```

### SbEdge API
**Base URL:** `https://supabase.virtualhealth.cn/functions/v1`

#### `getInfo` (Get Reagent Specs)
- **Request:** `{"cardBatchCode": "", "cardCode": "QR_STRING"}`
- **Response:** Contains `cardConfig` with `topList` (peak bounds), `varList` (polynomial coefficients), and `scanPPMM`.

#### `updateStatus` (Lifecycle Management)
- **Request:** `{"cardCode": "QR_STRING", "status": "checking | success | failed"}`

#### `baaResult` (Bio-Age Analysis)
- **Request:** `{"code": "QR_STRING", "dmsPatientId": "PATIENT_OBJECT_ID"}`
- **Response:** Returns `bio_age`, `chrono_age`, and `scores`.

---

## 8. Porting Considerations for MCU (ESP32/STM32)

### Data Mapping Nuances
Note the mandatory case conversion for response parsing (Android uses GSON reflection to handle this):
- `objectId` -> `object_id`
- `birthDate` -> `birth_date`
- `BioAgeProfile` -> `bio_age_profile`
- `ChronoAge` -> `chrono_age`
- `BioAge` -> `bio_age`
- `AgeDifference` -> `age_difference`
- `Scores` -> `scores`

### Resource & Hardware Requirements
- **Memory Management:** A single scan generates up to **130KB** of raw binary data.
- **Network Stack:** All API endpoints require **HTTPS (TLS 1.2+)**.
- **Device Authentication:** Devices are identified by `deviceId` in headers or body as per specific endpoint requirements.

---

## 9. Key Data Models

### `CardConfig`
Contains the "DNA" of a reagent card:
- `ft0`, `ft1`: Incubation/Reaction times (seconds).
- `xt1`: Suction time (seconds).
- `scanPPMM`: Points per millimeter for scanning.
- `topList`: List of expected peak locations (start/end in mm).
- `varList`: Coefficients for result calculation formulas.
- `cMin`, `cMax`: Validity ranges for the Control (C) peak.

### `CaseBean`
Represents a single test instance, including patient demographics, test type, raw data points, and calculated results.
