# APK Update Mechanism

Kone supports two upgrade paths:

| Path | Trigger | Source |
|---|---|---|
| **Nano remote upgrade** | "检查 Nano 升级" button in Settings → System Functions → API Test | Waven Nano backend (`/api/kino-upgrade`) |
| **U-disk / file manager** | "U盘升级" button in Settings → After-Sale | Local file via system file manager |

---

## 1. Nano Remote Upgrade (Primary)

### 1.1 Where it lives

`Settings → System Functions → API Test` (`SysFunApiTest.kt`).

The upgrade section is entirely inline on this screen. There is no navigation to a separate upgrade screen — all state (checking, progress, errors) is displayed here.

### 1.2 Flow

```
User taps "检查 Nano 升级"
    │
    ▼
NanoApi.checkUpgrade()
    GET /api/kino-upgrade
    Authorization: Bearer <token>
    │
    ▼
Compare remote version vs deviceConfig.software
(VersionUtils.isLessThan)
    │
    ├─ Up to date          → UpgradeCheckState.UpToDate (green badge)
    ├─ New version found   → UpgradeCheckState.Available(version, url) (amber badge + button)
    ├─ Empty response      → UpgradeCheckState.Error("Nano 暂无可用版本")
    └─ Null response       → UpgradeCheckState.Error("无法连接 Nano 升级接口")

User taps "立即升级"
    │
    ▼
upgradeVm.onUpgradeFromUrl(url, version)      ← AfterSaleVersionUpgradeViewModel
    │  sets isDownloading = true
    │  emits EVT_DOWNLOADING
    │
    ▼
downloadAndInstallApkSync(url, version, configBean)
    │
    ▼
downloadApkWithProgressSync(url, version)     ← DownloadManager
    │  polls every 1 s → emits EVT_DOWNLOADING "正在下载新版本... X%"
    │  on STATUS_SUCCESSFUL → returns File
    │
    ▼
installApkSync(apkFile, remoteConfigBean)
    │  saves new version to SysConfigService
    │  launches system package installer (Intent.ACTION_VIEW + FileProvider)
    │
    └─ on failure → EVT_ERROR
```

### 1.3 State machine (`SysFunApiTest.kt`)

```kotlin
sealed class UpgradeCheckState {
    object Idle       : UpgradeCheckState()
    object Checking   : UpgradeCheckState()
    object UpToDate   : UpgradeCheckState()
    data class Available(val version: String, val url: String) : UpgradeCheckState()
    data class Upgrading(val msg: String)  : UpgradeCheckState()   // download/install in progress
    data class Error(val msg: String)      : UpgradeCheckState()
}
```

`upgradeState` is a local Compose `var` in `SysFunApiTest`. It is updated by:
- The check button coroutine (Idle → Checking → Available / UpToDate / Error)
- The upgrade button lambda (Available → Upgrading)
- A `LaunchedEffect(upgradeVm)` that collects `upgradeVm.actionState` via `collectLatest` and mirrors ViewModel events into local state:

| ViewModel event | Local state |
|---|---|
| `EVT_DOWNLOADING` | `Upgrading(msg)` |
| `EVT_INSTALLING` | `Upgrading(msg)` |
| `EVT_ERROR` | `Error(msg)` |
| `EVT_DOWNLOAD_FAILED` | `Error(msg)` |

### 1.4 ViewModel method — `onUpgradeFromUrl`

`AfterSaleVersionUpgradeViewModel.onUpgradeFromUrl(url, version)` is the entry point for Nano-sourced upgrades. It bypasses the normal version-check flow entirely, using the URL and version string already returned by `/api/kino-upgrade`.

The older `onCheckVersion()` method is retained for the legacy after-sale screen but is not used by the Nano upgrade path.

### 1.5 Version comparison

`VersionUtils.isLessThan(local, remote)` — returns `true` when the remote version is strictly greater than the local version. The local version comes from `ConfigInfoV2Bean.software` (loaded from `ConfigInfoBean.PREFIX` in `SysConfigService`), not from `ConfigSysBean`.

### 1.6 Download

Uses Android's `DownloadManager`. The app polls every 1 second and emits progress via `actionState`. The destination file is:
- Android 10+: `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)/app_update_{version}.apk`
- Older: `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)/app_update_{version}.apk`

The pre-download HEAD check (`checkUrlAvailability`) has been removed. OSS presigned GET URLs are method-specific and reject HEAD requests, causing false failures before the download even starts.

### 1.7 Installation

`FileProvider` is used (authority: `${packageName}.fileprovider`) with paths defined in `res/xml/device_upgrade_config.xml`. The system package installer is launched via `Intent.ACTION_VIEW`. After a successful install launch, `SysConfigService` is updated with the new version.

---

## 2. U-Disk / File Manager Upgrade

`AfterSaleVersionUpgradeViewModel.onUDiskUpgrade()` attempts to launch the system file manager (`com.mediatek.filemanager`) for manual APK selection. This is an offline fallback for scenarios where the device cannot reach the Nano backend.

---

## 3. API Endpoint

`GET /api/kino-upgrade` on the Nano worker returns the active release:

```json
{ "version": "0.2.1", "url": "https://kone-apk.fros.cc/apk/15e3c862.apk?..." }
```

Empty strings (not `null`) are returned when no release is active, matching the non-nullable `String` fields in `NanoUpgradeResp`.

The download URL uses the `kone-apk.fros.cc` custom CNAME domain — Aliyun OSS blocks APK distribution via the default `*.oss-cn-shanghai.aliyuncs.com` endpoint.

See `nano/docs/architecture/kone-apk-upgrade.md` for the full backend design (DB schema, OSS setup, admin panel upload flow).

---

## 4. Relevant Files

| File | Purpose |
|---|---|
| `ui/sysfun/SysFunApiTest.kt` | Upgrade UI — check button, state display, ViewModel wiring |
| `ui/aftersale/AfterSaleVersionUpgradeViewModel.kt` | `onUpgradeFromUrl()`, download/install logic |
| `thirdparty/NanoApi.kt` | `checkUpgrade()` — `GET /api/kino-upgrade` |
| `thirdparty/model/nano/NanoModels.kt` | `NanoUpgradeResp` data class |
| `utils/app/VersionUtils.kt` | `isLessThan()` for version comparison |
| `res/values/strings.xml` | `sys_fun_api_upgrade_*` string resources |
| `res/xml/device_upgrade_config.xml` | FileProvider paths for APK install |
