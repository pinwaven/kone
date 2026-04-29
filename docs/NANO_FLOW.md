# Nano Flow Integration

The **Nano Flow** is a high-speed, streamlined alternative to the legacy clinical workflow in the Kone (Kino-One) application. It bridges the physical biomarker analyzer with the Waven Nano AI cloud platform (`nano.fros.cc`), enabling real-time biological age assessment and personalized nutrition recommendations.

## 1. Activation
The Nano Flow is toggled via the system configuration:
- **Setting:** `ConfigSysBean.flow = "nano"`
- **Requirement:** `ConfigSysBean.nanoDeviceId` must match a registered `serial_number` in the `kino_devices` table on the backend.
- **UI:** When active, the system displays "Nano" in the flow selector and replaces the standard report view with the Nano-specific results overlay.

## 2. End-to-End Sequence

### A. Chip Identification & Configuration
When a Kino Chip is scanned (QR code) or inserted:
1. The device calls **`GET /api/kino-chip?chip_id={chip_code}`**.
2. **Backend Response:**
   - Linked user profile (`openid`, `nickname`, `birth_date`).
   - Physical scan driver (`chip_config`: `top_list`, `var_list`, `scan_ppmm`).
   - Expected biomarkers (`biomarker_keys`: e.g., `["hsCRP"]`).
   - Onboarding assets (`guide_video`, `guide_text`).
3. **Logic:** The device automatically populates the patient info and configures the hardware laser/motion parameters using the server-authoritative `chip_config`.

### B. Physical Scan & Analysis
The device performs the physical scan as described in [CHIP_SCANNING_LOGIC.md](CHIP_SCANNING_LOGIC.md):
1. **Physical Scan:** Motor movement and ADC sampling.
2. **Signal Processing:** Signal normalization and peak area integration.
3. **Concentration Calculation:** Applying the `var_list` polynomial to determine raw biomarker concentrations.

### C. AI Ingestion & BioAge Calculation
Unlike the clinical flow which relies on manual reporting, the Nano flow is fully automated:
1. **Upload:** Device calls **`POST /api/biomarkers`** with raw concentrations.
2. **AI Processing:** The backend runs the `BioAgeCalculator` to determine:
   - **Combined BioAge**
   - **Four Sub-Ages:** Resilience, Cellular, Metabolic, Micro-Vascular.
   - **Biomarker Estimates:** AI estimates missing markers (e.g., IL-6, GDF-15) based on the measured CRP and user age.
3. **Response:** The API returns a full `bioage_profile` and the augmented biomarker set.

### D. Finalization
1. **Persist:** Device calls **`POST /api/kino-result`** to mark the chip as `used` in the database.
2. **Display:** The device shows the **Nano Results Overlay** (see UI section below).
3. **Notify:** The backend automatically generates a 7-day nutrition plan (Dots) and sends a notification to the user's WeChat Mini Program.

## 3. GUI Implementation

### Nano Results Overlay (`WorkActionNanoReportBlock.kt`)
A specialized UI component mirroring the WeChat Mini Program design:
- **Bio Age Status:** A circular "Status Face" indicating whether the user is "Accelerating" (Red), "Stable" (Amber), or "Decelerating" (Green).
- **Sub-Age Breakdown:** Interactive grid showing the four biological dimensions.
- **Biomarkers Tab:** Lists measured (Color) vs. AI-estimated (Grey) biomarkers with units.

### Diagnostic Tools
- **Nano API Test:** Accessible in system functions to verify connectivity to `nano.fros.cc` and validate the `API_BEARER_TOKEN`.

## 4. Key Differences from Clinical Flow

| Feature | Clinical Flow (Legacy) | Nano Flow (New) |
|---|---|---|
| **User Data** | Fetched from `fros-api` | Fetched from `nano.fros.cc` |
| **Config Source** | `SbEdgeFunc.getCardInfo` | `NanoApi.getChip` (Unified) |
| **AI Integration** | Limited / Async | Real-time / Inline |
| **Final Result** | hsCRP Concentration | BioAge + Sub-Ages |
| **Follow-up** | Static Report | Dynamic Nutrition Plan (Dots) |

## 5. Technical References
- **Android UI:** `WorkActionNanoReportBlock.kt`
- **Android Logic:** `WorkMainViewModel.kt` (search for `isNanoFlow()`)
- **API Definition:** [Worker API Endpoints](../../nano/docs/api/worker-endpoints.md)
- **Backend Calculator:** `src/lib/bioage/BioAgeCalculator.js` (Nano project)
