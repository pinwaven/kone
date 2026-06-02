# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android project named `poct-device-app`. Main Kotlin/Java code lives in `app/src/main/java/poct/device/app/`. UI screens are under `ui/`, state under `state/`, Room entities/services under `entity/`, serial and hardware communication under `serial/`, API clients under `thirdparty/`, and PDF/chart helpers under `pdf/` and `chart/`.

Resources are in `app/src/main/res/`; bundled fonts, PDFs, and runtime assets are in `app/src/main/assets/`. Unit tests belong in `app/src/test/`, instrumentation tests in `app/src/androidTest/`, documentation in `docs/`, design fixtures and analysis scripts in `design/`, and device/release helpers in `scripts/`.

## Build, Test, and Development Commands

- `./gradlew assembleDebug` builds a debug APK for local installation.
- `./gradlew assembleRelease` builds the signed release APK using `keys/kone-release-key.jks` and Gradle signing settings.
- `./gradlew test` runs JVM unit tests.
- `./gradlew connectedAndroidTest` runs instrumentation tests on a connected device or emulator.
- `./gradlew clean` removes Gradle build outputs.
- `scripts/setup.sh`, `scripts/upload_apk.sh`, `scripts/uninstall.sh` support device setup and APK management.

## Coding Style & Naming Conventions

Use Kotlin for new Android code unless an existing Java integration requires Java. Target Java 17 and Kotlin JVM target 17. Keep feature code near the matching `ui`, `entity`, `serial`, or `thirdparty` package.

Use 4-space indentation. Name types in `PascalCase`, functions/properties in `camelCase`, constants in `UPPER_SNAKE_CASE`, and Compose screens/components with descriptive `PascalCase` names such as `ReportMain`. Prefer `ViewModel` suffixes for screen state classes.

## Testing Guidelines

Use JUnit 4 for local tests and AndroidX test/Espresso for instrumentation. Add tests for business logic, parsers, database behavior, and serial/protocol transformations. Mirror production packages under `app/src/test/` and use `*Test.kt` or `*Test.java`. Run `./gradlew test` before PRs; run `./gradlew connectedAndroidTest` for UI, device, or hardware-adjacent changes.

## Commit & Pull Request Guidelines

Recent history uses short, imperative subjects, sometimes Conventional Commit style such as `feat(build): update version to 0.3.0`. Prefer `type(scope): summary` (`feat`, `fix`, `docs`, `test`, `refactor`, `build`) and keep it specific.

Pull requests should include a concise description, linked issue or context, test results, and screenshots or videos for UI changes. Note device requirements, affected serial ports, APK version changes, and release signing impact.

## Security & Configuration Tips

Do not add new secrets or private keys. Existing signing material and passwords are sensitive; avoid copying them into docs, logs, or tickets. Keep integrations configurable, validate inputs at system boundaries, and review `git diff` before pushing.
