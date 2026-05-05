# APK Update Mechanism

This document describes the technical implementation of the self-update process for the Kone Android application.

## Overview
The Kone application supports autonomous self-updates via a remote server. The process is managed by `AfterSaleVersionUpgradeViewModel.kt` and follows a **Check -> Validate -> Download -> Install** lifecycle.

## 1. Version Discovery
The update process begins when the user triggers a version check or when the app checks for updates automatically.

- **Service Endpoint:** `SbEdgeFunc.getDeviceConfig(deviceId)`
- **Logic:** The app retrieves the remote `apkVersion` from the cloud and compares it with the local version stored in the system configuration.
- **Comparison:** Uses `VersionUtils.isLessThan(local, remote)` to determine if a newer version exists.

## 2. Download Preparation
Before initiating the download, the app performs safety and connectivity checks:

- **URL Construction:** `https://poct-upgrade.virtualhealth.cn/apk/{version}.apk`
- **Dns/Connectivity Check:** `checkUrlAvailability(url)` performs an HTTP `HEAD` request to ensure the file exists and the server is reachable before committing resources to a full download.
- **State Management:** Sets `isDownloading = true` to prevent concurrent download attempts.

## 3. Background Download
The app leverages the Android system's `DownloadManager` for robust file transfer.

- **Request Configuration:**
  - Title/Description: "应用更新" / "正在下载新版本..."
  - Visibility: `VISIBILITY_VISIBLE_NOTIFY_COMPLETED`
  - MIME Type: `application/vnd.android.package-archive`
- **Storage Path:** 
  - Android 10+: `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`
  - Older: `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`
- **Progress Tracking:** The ViewModel polls the `DownloadManager` every second to update the UI with percentage progress.

## 4. Installation
Once the download is complete (`STATUS_SUCCESSFUL`), the installation phase begins.

### FileProvider Security
To comply with Android security standards (especially Android 7.0+), the app uses a `FileProvider` to share the APK with the system installer.
- **Authority:** `${applicationId}.fileprovider`
- **Paths:** Defined in `res/xml/device_upgrade_config.xml`.

### Execution
The app launches the system package installer via an `Intent`:
- **Action:** `Intent.ACTION_VIEW`
- **Data:** `Uri` from `FileProvider.getUriForFile`
- **Flags:** 
    - `Intent.FLAG_GRANT_READ_URI_PERMISSION`
    - `Intent.FLAG_ACTIVITY_NEW_TASK`

## 5. Manual/U-Disk Update
An alternative update path exists for offline scenarios:
- **Location:** `AfterSaleVersionUpgradeViewModel.onUDiskUpgrade()`
- **Method:** Attempts to launch the system file manager (`com.mediatek.filemanager`) to allow manual APK selection and installation.

## Relevant Files
- `app/src/main/java/poct/device/app/ui/aftersale/AfterSaleVersionUpgradeViewModel.kt`: Core logic.
- `app/src/main/AndroidManifest.xml`: Permissions and FileProvider declaration.
- `app/src/main/res/xml/device_upgrade_config.xml`: Storage path configuration for updates.
