# Hardware Communication

The Kone device communicates with the biomarker analyzer hardware via a serial port.

## Core Components
- **`SerialPort.kt`**: Handles the low-level serial stream (Input/Output).
- **`CtlCommands.kt`**: Defines the application-level protocol. 
  - **Synchronization:** Uses fixed bytes (`0x5A`, `0xA5`) for frame detection.
  - **Operations:** `startCheck`, `scanTest`, `upgrade` (firmware), and peripheral control.

## Data Sampling
- **ADC Sampling:** High-frequency 13-bit ADC data is retrieved.
- **Parsing:** Implemented in `AppCardUtils.kt`, mirroring the logic found in the Python prototypes (`design/data/analyzer.py`).

