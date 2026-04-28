# GUI Flow Documentation

The Kone application follows a structured navigation flow, designed for a dedicated kiosk-style medical device.

## Application Entry & Authentication
1. **Splash Screen** (`SingleSplash`): Initial loading and system check.
2. **Login Screen** (`SingleLogin`): User authentication.

## Main Dashboard
Upon successful login, the user is directed to the **Home Screen** (`HomeMain`). This serves as the central hub for all primary functions.

## Primary Workflows

### 1. Biomarker Testing (`Work`)
- **Work Main** (`WorkMain`): The main interface for performing tests.
- **Workflow:** Selecting a test, performing the scan (interfacing with hardware), and viewing real-time progress.

### 2. Report Management (`Report`)
- **Report Main** (`ReportMain`): List of historical test results.
- **Report Detail** (`ReportDetail`): Detailed view of a specific test record.
- **Report PDF** (`ReportPDF`): Preview and print functionality for reports.
- **Report Edit** (`ReportEdit`): Modifying patient or test information if permitted.

### 3. Configuration & Settings
- **Test Configuration** (`WorkConfig`):
  - Managing reagent card types (`WorkConfigCard`).
  - Adding/Viewing card details via QR codes (`WorkConfigCardAdd`, `WorkConfigCardQrCode`).
  - Report template settings (`WorkConfigReport`).
- **System Configuration** (`SysConfig`):
  - Connectivity (WLAN).
  - Localization (Language, Date/Time).
  - Peripherals (Printer, Scanner).
  - Internal system parameters.

### 4. Maintenance & Support (`AfterSale`)
- **After-Sale Main** (`AfterSaleMain`): Maintenance utilities.
- **Version Management**: Upgrading firmware/software and viewing version history.

## Navigation Architecture
The app uses **Jetpack Compose Navigation**. All routes are defined in `RouteConfig.kt`, and the graph is orchestrated in `MainActivity.kt` using a `NavHost` with nested navigation graphs for each functional module.

