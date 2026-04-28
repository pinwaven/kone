# Project Architecture

The **Kone (Kino-One)** project is a specialized Android application designed for Point of Care Testing (POCT). It operates as a kiosk-style interface for hardware biomarker analysis.

## Frameworks & Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Modern, declarative UI)
- **Database:** Room (SQLite abstraction for data persistence)
- **Dependency Injection/Management:** Standard Android Singleton patterns and Gradle.
- **Charts:** Vico library for data visualization.
- **Hardware Communication:** Custom Serial Port implementation.

## Project Structure
- `app/`: Main Android module containing UI, business logic, and hardware drivers.
- `design/`: Technical specifications, Python-based data analysis prototypes, and design assets.
- `scripts/`: Build and signing utilities (e.g., `pack.sh` for firmware/apk packaging).

## Navigation
Navigation is centrally managed in `app/src/main/java/poct/device/app/RouteConfig.kt`, using Jetpack Compose Navigation (`MainNavHost`).

