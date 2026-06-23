# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

**Kone (Kino-One)** — Android kiosk application for Point-of-Care Testing (POCT) biomarker analysis. Controls hardware via serial port, scans reagent cards, computes biomarker results, and generates PDF reports.

## Engineering Standards

- NEVER perform automatic git commits or pushes. The user handles all version control manually.
- Documentation goes in `docs/` (Markdown). Design assets and Python analysis prototypes go in `design/`.
- Kotlin for all new Android code. Java 17 / Kotlin JVM target 17.

## Build Commands

```bash
# Debug APK
./gradlew assembleDebug

# Signed release APK → app/build/outputs/apk/release/
./gradlew app:assembleRelease

# If gradlew lacks execute permission or needs a custom Gradle home:
GRADLE_USER_HOME=.gradle_user_home bash ./gradlew app:assembleRelease

# Clean
./gradlew clean
```

Release signing uses `keys/kone-release-key.jks`. Passwords/alias are in `gradle.properties` (not for docs/logs).

Three local AAR dependencies must exist in `app/libs/`:
- `iotLib-comm-debug-*.aar`
- `iotLib-presenter-debug-*.aar`
- `iotLib-socket-debug-*.aar`

## Test Commands

```bash
# JVM unit tests
./gradlew test

# Single test class
./gradlew test --tests "poct.device.app.ui.sample.OneKeyTestResultChartLayoutTest"

# Instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

Tests mirror production packages under `app/src/test/` and `app/src/androidTest/`. Use `*Test.kt` naming.

## Architecture

### UI Layer
- **Jetpack Compose** throughout. Screens are in `ui/` sub-packages by feature.
- Navigation is centrally defined in `RouteConfig.kt` with string route constants. The nav graph is wired in `MainActivity.kt` via `MainNavHost`.
- Screen route groups: `countdown`, `single` (splash/login), `home`, `work`, `report`, `setting`, `afterSale`, `sysFun`, `workConfig`, `sysConfig`, `sample`.
- Each screen has a paired `*ViewModel` using `MutableStateFlow` for state. State sealed classes live in `state/` (`ViewState`, `ActionState`).

### Work Flow (Core Feature)
`ui/work/WorkMainViewModel.kt` orchestrates the main test workflow. The flow is modeled as `WorkFlowV2` / `WorkFlowActionV2` (beans), with step-specific composable blocks (`WorkAction*Block.kt`) rendered by `WorkMain.kt`. This is the most complex screen — changes here require understanding the full state machine.

### Hardware / Serial
Two protocol versions coexist:
- **v1** (`serial/`): `SerialPort.kt`, `CtlCommands.kt`, `CtlSerialService.kt`
- **v2** (`serial/v2/`): `SerialPortV2.kt`, `CtlCommandsV2.kt`, `CtlSerialMessageV2.kt` ← active default

Protocol uses `0x5A`/`0xA5` sync bytes for frame detection. ADC data parsing is in `AppCardUtils.kt`, mirroring `design/data/analyzer.py`.

### Data Layer
- **Room** via `AppDatabase.kt`. Entities: `Case`, `User`, `CardConfig`, `SysConfig`, `CasePoint`, `CaseResult`.
- Services in `entity/service/` wrap DAOs (`entity/dao/`).
- Beans in `bean/` are transfer objects (not Room entities).

### Third-Party Integrations
- `thirdparty/NanoApi.kt` — Nano biomarker scoring API (auth token in `NanoAuthStore`)
- `thirdparty/FrosApi.kt` — Fros API
- `thirdparty/SbEdgeFunc.kt` — edge function
- `KINO_ACTIVATION_TOKEN` build config field, sourced from `gradle.properties` or env var

### Charts & PDF
- Charts use **Vico** library. Custom chart style in `chart/ChartStyle.kt`.
- PDF generation in `pdf/`. Report PDF rendered as Compose screen (`ReportPDF.kt`) then captured.
