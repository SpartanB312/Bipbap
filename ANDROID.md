# Android Gradle Plugin Usage

[Back to README](https://github.com/spartanb312/bipbap/blob/main/README.md)

Bipbap supports Android obfuscation through the Gradle plugin:

```kotlin
id("net.spartanb312.bipbap.android")
```

The plugin transforms project classes before R8/ProGuard. This lets Bipbap provide an encryption layer, then lets R8/ProGuard continue shrinking, optimizing, and renaming the final Android build.

## Local Plugin Setup

When using Bipbap from a local checkout, add it to the Android project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("../Bipbap")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Apply the plugin in an Android app or library module:

```kotlin
plugins {
    id("com.android.application")
    id("net.spartanb312.bipbap.android")
}
```

## Configuration

```kotlin
bipbap {
    // Defaults to low.
    preset.set("low")

    // Use -1 for Runtime.availableProcessors().
    threads.set(-1)

    // Enabled by default. Disables Android-unsafe transformers.
    safeMode.set(true)

    // Enabled by default. Adds Bipbap keep rules and uses the R8/ProGuard-safe transform set.
    proguardCompatible.set(true)

    // Defaults to release only. Use emptyList() to transform all build types.
    buildTypes.set(listOf("release"))
}
```

By default, only `release` variants are transformed. Android Studio's Run button usually builds `debug`, so Bipbap will not run there unless you add `debug`:

```kotlin
buildTypes.set(listOf("debug", "release"))
```

For normal development, keeping Bipbap release-only is recommended.

## R8/ProGuard Integration

For Android release builds, keep your normal R8/ProGuard setup:

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

When `proguardCompatible` is enabled, Bipbap generates and attaches:

```text
build/generated/bipbap/proguard/bipbap-keep-rules.pro
```

The generated rule keeps Bipbap's `$Constants` classes intact so R8/ProGuard does not optimize away the constant encryption layer.

## Android Safe Defaults

Android safe mode is enabled by default. In this mode:

| Transformer | Android default |
|-------------|-----------------|
| ConstantEncryptor | Enabled |
| MembersRenamer local variables | Enabled |
| MembersRenamer fields/methods | Disabled |
| CodeOptimizer | Disabled |
| InvokeDynamics | Disabled |
| HWIDAuthenticator | Disabled |
| Miscellaneous crasher/hidecode/watermark | Disabled |

These defaults are conservative and designed to work before R8/ProGuard.

## Verify Plugin Usage

Check whether Bipbap is part of the release task graph:

```shell
./gradlew :app:assembleRelease --dry-run
```

You should see:

```text
:app:releaseBipbapAndroidClasses
:app:minifyReleaseWithR8
```

After a release build, check the Bipbap report:

```shell
cat app/build/reports/bipbap/release.txt
```

Example:

```text
[Bipbap] Enabled for release
[Bipbap] Preset: low
[Bipbap] Threads: 12
[Bipbap] Safe mode: true
[Bipbap] ProGuard/R8 compatible: true
[Bipbap] Output transformed classes for R8: .../releaseBipbapAndroidClasses/classes.jar
```

The intermediate jar transformed by Bipbap is written to:

```text
app/build/intermediates/classes/release/PROJECT/releaseBipbapAndroidClasses/classes.jar
```

The final Android artifact is still produced by the Android build:

```text
app/build/outputs/apk/release/app-release.apk
```
