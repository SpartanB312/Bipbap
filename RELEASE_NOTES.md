# Bipbap 1.1.0

Bipbap 1.1.0 expands from a standalone JVM obfuscator into a fuller toolchain: command line, Swing UI, Android Gradle Plugin, and JVM Gradle Plugin are now supported from the same codebase.

## Highlights

- Added Android Gradle Plugin support for app/library obfuscation before R8/ProGuard.
- Added JVM Gradle Plugin support for desktop Java/Kotlin jar obfuscation.
- Added Swing UI startup by default when launching the jar without arguments.
- Reworked CLI argument parsing with presets, config generation, thread configuration, and cleaner jar defaults.
- Added coroutine-backed `WorkContext` execution infrastructure for transformer-level parallel work.
- Expanded default exclusions for Android, Spring, Minecraft, and common third-party libraries.

## New Features

### Android Gradle Plugin

New plugin id:

```kotlin
id("net.spartanb312.bipbap.android")
```

The Android plugin transforms project classes before R8/ProGuard and defaults to a conservative Android-safe mode.

Default Android behavior:

- Uses `low` preset by default.
- Runs on `release` variants by default.
- Supports `threads.set(-1)` for `Runtime.availableProcessors()`.
- Generates Bipbap keep rules for R8/ProGuard.
- Writes a status report to `build/reports/bipbap/<variant>.txt`.
- Outputs transformed classes into the AGP class artifact pipeline before R8.

Generated keep rules:

```text
build/generated/bipbap/proguard/bipbap-keep-rules.pro
```

### JVM Gradle Plugin

New plugin id:

```kotlin
id("net.spartanb312.bipbap.jvm")
```

The JVM plugin reads the project's `jar` task output and writes a separate obfuscated jar without replacing the original artifact.

Default JVM behavior:

- Uses `low` preset by default.
- Registers `bipbapObfuscateJar`.
- Hooks into `assemble` by default.
- Includes `runtimeClasspath` jars as libraries for hierarchy resolution.
- Writes reports to `build/reports/bipbap/jvm.txt`.
- Outputs `build/libs/<name>-obf.jar` by default.

### Swing UI

- Launching `java -jar bipbap.jar` with no arguments now opens the UI.
- Added configuration, advanced transformer configuration, and console views.
- Console output is cleared for each obfuscation run.
- Added an `Obfuscate` action in the console page.
- Advanced mode can configure transformer values dynamically via `transformer.getValues()`.
- Enabling advanced mode disables preset selection.

### CLI

Added and refined CLI support:

- `-ui` opens the Swing UI.
- `-genconfig <file.json>` generates a default config.
- `-mt:<threads>` configures worker thread count.
- `-mt` and `-mt:-1` use `Runtime.availableProcessors()`.
- Preset input/output defaults:
  - `-high` uses `input.jar` -> `output.jar`
  - `-high input.jar` uses `input.jar` -> `input-obf.jar`
  - `-high input.jar output.jar` uses explicit input/output jars

## Changed

- Moved CLI-related code under `net.spartanb312.bipbap.cli`.
- Reworked UI header layout and moved console output to its own page.
- Updated `StringEncryptor` generated method flags for Android/D8 compatibility.
- Updated ASM to `9.9` for newer class file support.
- Added `kotlinx-coroutines-core`.
- Added `.gitignore` for Gradle, IDE, build, and local output files.

## Documentation

- Reworked `README.md` around CLI, UI, JVM Gradle Plugin, and Android Gradle Plugin usage.
- Added `ANDROID.md` for Android-specific setup, ProGuard/R8 integration, safe defaults, reports, and output paths.
- Added `JVM.md` for desktop Java/Kotlin Gradle plugin setup and output behavior.

## Validation

Validated locally with:

- `./gradlew compileKotlin`
- `./gradlew jar`
- Android plugin integration in a real Android project with `assembleRelease`
- JVM plugin integration in a multi-module JVM desktop project with `bipbapObfuscateJar`

## Notes

- Android Studio's Run button usually builds `debug`; Bipbap Android plugin defaults to `release` only.
- For Android, keep normal R8/ProGuard enabled. Bipbap runs before R8/ProGuard and adds a constant encryption layer.
- For JVM projects targeting very new class file versions, ASM `9.9` is required and now included.
