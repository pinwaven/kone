# Software and Firmware Upgrades

This document outlines how the Kone project currently handles software (APK) and hardware firmware upgrades over Wi-Fi.

## Software (APK) Upgrades

The application has a fully implemented flow for self-updating via Wi-Fi.

### Process Flow
For a detailed technical breakdown of the implementation, see [APK Update Mechanism](APK_UPDATE_MECHANISM.md).

1. **Version Checking**: The app retrieves the remote device configuration using `SbEdgeFunc.getDeviceConfig`.
2. **Comparison**: It compares the remote `software` version with the local version using `VersionUtils.isLessThan`.
3. **Download**: If a new version is available, it constructs a download URL (e.g., `https://poct-upgrade.virtualhealth.cn/apk/{version}.apk`) and uses the Android `DownloadManager` to download the APK.
4. **Installation**: Once the download is complete, the app triggers a system intent (`Intent.ACTION_VIEW` with the APK URI) to prompt the user to install the update.

### Key Components
- **`AfterSaleVersionUpgradeViewModel.kt`**: Manages the UI state, version checking, and the download/installation lifecycle.
- **`DownloadManager`**: Standard Android service used for background file downloads.

---

## Hardware Firmware Upgrades

While the system can identify the need for hardware updates, the execution logic is currently incomplete and "stubbed out."

### Current Implementation State
- **Task Identification**: The app can query for pending tasks (`CommService.instance().getTaskByType(CommEnsTaskType.UPGRADE)`) and correctly identifies hardware updates (`TYPE_SYS`).
- **Execution Stubs**: The `AppUpgradeUtils.kt` file contains the entry point for upgrades but lacks the actual implementation:
  - `downloadFile(version)`: Returns hardcoded paths and contains `TODO` comments for downloading and unzipping.
  - `upgradeHardware0(file)` and `upgradeHardware1(file)`: Empty functions.
- **Serial Commands**: Low-level serial commands for firmware transmission exist in `CtlCommands.kt` (using `CMD_CTL_UPGRADE` / `0xF0`), but they are not currently integrated into the upgrade flow.

### Gap Analysis
The application is capable of updating itself, but it cannot yet push firmware updates to the physical analyzer hardware. The bridge between the downloaded firmware payload and the serial communication layer is not yet implemented.
