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

1.  **Parsing:** `AppCardUtils.parseData(queryResult)` converts the binary buffer into a structured `ScanData` object, extracting the raw signal points.
2.  **Conversion:** `convertToPointListV2` maps these points into a list of `CasePoint` objects (X/Y coordinates).
3.  **Peak Calculation:** `AppWorkCalcUtils.calculatePeakArea(...)` uses the `start` and `end` positions from the `top_list` to calculate the area under specific peaks (e.g., T1, T2, C).

## 5. Interpretation: Result Generation
The calculated peak areas are converted into clinical results.

- **Calculation:** `AppCardUtils.genResultForBACRP(...)` (or other type-specific functions) applies the `var_list` calibration logic.
- **Upload:** The results are uploaded back to the server via `FrosApi.uploadPatientReportDataToServer(bean)`.

## Summary of Key Files
| File | Responsibility |
| :--- | :--- |
| `WorkMainViewModel.kt` | Orchestrates the state machine (Scan -> Read -> Process). |
| `CtlCommandsV2.kt` | Generates low-level serial commands (`0x44`, `0x45`). |
| `AppCardUtils.kt` | Parses binary signal data and interprets results. |
| `AppWorkCalcUtils.kt` | Performs mathematical calculations (Peak Area, Integration). |
