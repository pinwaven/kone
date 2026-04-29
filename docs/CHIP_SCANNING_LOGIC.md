# Chip Scanning Logic

This document describes the physical and logical flow of scanning a biomarker chip in the Kone application, from data retrieval to final interpretation.

## 1. Preparation: Data Retrieval
The process begins by fetching the chip's configuration from the server.
- **API Call:** `https://sb.fros.cc/functions/v1/business/poct/card/getInfo`
- **Result:** Provides `card_config`, which includes:
    - `scan_ppmm`: Scanning density (points per mm).
    - `top_list`: Defines the `start` and `end` positions for specific peaks (e.g., T1, C, T2).
    - `var_list`: Calibration parameters used to convert peak areas into numerical readings.

## 2. Execution: Physical Scanning
The hardware scanning is orchestrated by `WorkMainViewModel` and executed via the serial protocol.

1.  **Initiation:** `WorkMainViewModel.doScanTest(workFlowAction)` is called.
2.  **Command Sending:** `CtlCommandsV2.scan(velocity, duration)` (in `serial/v2/ctl/CtlCommandsV2.kt`) sends the physical **`CMD_ACTION_SCAN` (0x44)** command to the serial port.
3.  **Hardware Motion:** The device physically moves the sensor/laser across the chip to sample ADC values.
4.  **Completion Polling:** `CtlCommandsV2.waitScanStatusSuccess()` polls the hardware status until it reports `COMPLETED`.

## 3. Data Retrieval: Reading the Buffer
After the physical scan is complete, the raw data must be pulled from the device's memory.

1.  **Request:** `WorkMainViewModel.doReadData(workFlowAction)` calls `CtlCommandsV2.queryData()`.
2.  **Command:** Sends **`CMD_ACTION_QUERY_DATA` (0x45)**.
3.  **Reception:** `CtlCommandsV2.readAllDataByteArray(cmd)` reads the binary stream from the serial buffer.

## 4. Processing: Signal Analysis
The raw binary data is converted into meaningful readings.

### Raw Data Format (Binary)
The raw data received from the hardware is a binary stream with a fixed-size header followed by a variable-length ADC payload.

**Header Structure (10 Bytes, Little Endian):**
| Offset | Field | Type | Description |
| :--- | :--- | :--- | :--- |
| 0 | `dataLen` | `uint16` | Number of 13-bit ADC raw data points following. |
| 2 | `laserCurr` | `uint16` | 12-bit ADC value representing the laser current. |
| 4 | `fixed5a` | `uint8` | Sync byte: must be **`0x5A`**. |
| 5 | `dataBias` | `uint16` | ADC baseline noise/bias for the signal data. |
| 7 | `laserCurrBias`| `uint16` | ADC baseline bias for the laser current. |
| 9 | `fixedA5` | `uint8` | Sync byte: must be **`0xA5`**. |

**Data Payload:**
- Followed by `dataLen` points of **2-byte `uint16`** values representing 13-bit ADC samples.
- **Normalization:** The system subtracts `dataBias` from each raw sample to produce the `dataBiased` signal used for analysis.

### Processing Steps
1.  **Parsing:** `AppCardUtils.parseData(queryResult)` converts the binary buffer into a structured `ScanData` object.
2.  **Conversion:** `convertToPointListV2` maps these points into a list of `CasePoint` objects (X/Y coordinates).
3.  **Peak Calculation:** `AppWorkCalcUtils.calculatePeakArea(...)` calculates the area under specific peaks (T1, T2, C) using positions from `top_list`.
    - It identifies a local baseline and integrates only the signal **above** that baseline.

## 5. Interpretation: hsCRP Calculation
The calculated peak areas are converted into clinical concentrations (e.g., hsCRP) using a ratio-based polynomial calibration.

### Calculation Steps
1.  **Ratio Generation:**
    A normalized ratio is calculated between the Test line (T) and Control line (C):
    $$Value = \frac{Area_{T}}{Area_{C}} \times 1000$$

2.  **Polynomial Calibration:**
    The system selects the appropriate coefficients ($x_0 \dots x_4$) from the `var_list` based on the calculated $Value$ and applies a 4th-degree polynomial:
    $$Result = x_0 + (x_1 \cdot Value) + (x_2 \cdot Value^2) + (x_3 \cdot Value^3) + (x_4 \cdot Value^4)$$

3.  **Whole Blood Correction:**
    If the sample is whole blood (`caseType == 2`), the result is multiplied by the `typeScore` (whole blood coefficient):
    $$Final hsCRP = Result \times typeScore$$

## Summary of Key Files
| File | Responsibility |
| :--- | :--- |
| `WorkMainViewModel.kt` | Orchestrates the state machine (Scan -> Read -> Process). |
| `CtlCommandsV2.kt` | Generates low-level serial commands (`0x44`, `0x45`). |
| `AppCardUtils.kt` | Parses binary signal data and interprets results. |
| `AppWorkCalcUtils.kt` | Performs mathematical calculations (Peak Area, Integration). |
