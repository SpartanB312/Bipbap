# Bipbap

[![CodeFactor](https://www.codefactor.io/repository/github/spartanb312/bipbap/badge)](https://www.codefactor.io/repository/github/spartanb312/bipbap)

This is a ready-to-use lightweight obfuscator without configuration and dependency requirements. Bipbap aims for those developers without obfuscation experience. It will automatically figure and exclude most in-jar dependencies and mixin classes. And its easy-to-use HardwareID authenticator injector will automatically insert authenticator in your jar.

## Usage

In command lines:

Open UI: java -jar bipbap.jar `-ui`

Generate a config: java -jar bipbap.jar `-genconfig` `config`

- `config` The config JSON file that will be generated

Use your config: java -jar bipbap.jar `config`

- `config` The specified config JSON file that will be used

Use our presets: java -jar bipbap.jar `preset` `input.jar` `output.jar` `threads` `authentication`

- `preset` (Optional) Select one preset in -low -mid -high. If no preset is selected, -low will be used
- `input.jar` (Optional) The jar file that will be processed. If not specified, input.jar will be used
- `output.jar` (Optional) The jar file that will be written. If input is specified but output is not, input-obf.jar will be used
- `threads` (Optional) format: -mt:4. If not specified, one thread will be used. Use -mt or -mt:-1 to use all available processors
- `authentication` (Optional) format: -auth=https://authentication.com

## Android Gradle Plugin

Apply Bipbap after the Android application or library plugin:

```kotlin
plugins {
    id("com.android.application")
    id("net.spartanb312.bipbap.android")
}

bipbap {
    // Defaults to low.
    preset.set("low")

    // Use -1 for Runtime.availableProcessors().
    threads.set(-1)

    // Enabled by default. Disables Android-unsafe transformers.
    safeMode.set(true)

    // Enabled by default. Adds Bipbap keep rules and uses the ProGuard/R8-safe transform set.
    proguardCompatible.set(true)

    // Defaults to release only. Use emptyList() to transform all build types.
    buildTypes.set(listOf("release"))
}
```

The Android plugin transforms release project classes through the Android Gradle Plugin Variant API before R8/ProGuard. Safe mode and ProGuard compatibility are enabled by default and keep the default Android transform conservative: ConstantEncryptor and local variable renaming stay enabled, while HWIDAuthenticator, CodeOptimizer, InvokeDynamics, field/method renaming, and Miscellaneous are disabled.

When `proguardCompatible` is enabled, the plugin also generates `build/generated/bipbap/proguard/bipbap-keep-rules.pro` and attaches it to enabled Android variants. The generated rule keeps Bipbap's `$Constants` classes intact so R8/ProGuard does not optimize away the constant encryption layer.

## Presets

| Preset  | Activated features                                                                              |
|---------|-------------------------------------------------------------------------------------------------|
| `-low`  | CodeOptimize, ConstEncrypt, Renamer(LocalVar), HideCode                                         |
| `-mid`  | CodeOptimize, ConstEncrypt, Renamer(LocalVar, Field, Method), InvokeDynamics, HideCode          |
| `-high` | CodeOptimize, ConstEncrypt, Renamer(LocalVar, Field, Method), InvokeDynamics, Crasher, HideCode |

## Features

### Obfuscation

* [X] HWIDAuthenticator
* [X] FieldRename
* [X] MethodRename
* [X] LocalVarRename
* [X] NumberEncrypt
* [X] StringEncrypt
* [X] InvokeDynamics
* [X] CodeHider
* [X] Watermark
* [X] Crasher

### Optimization

* [X] RemoveSource
* [X] RemoveInnerClass
* [X] RemoveDeadCodes
* [X] KotlinOptimize

## License: MIT

This is a free and open source software under MIT license

