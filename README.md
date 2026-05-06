# Bipbap

[![CodeFactor](https://www.codefactor.io/repository/github/spartanb312/bipbap/badge)](https://www.codefactor.io/repository/github/spartanb312/bipbap)

Bipbap is a lightweight JVM obfuscator with ready-to-use presets, a Swing UI, JSON configuration support, and Android Gradle Plugin integration.

It can be used as:

- a command line jar obfuscator
- a desktop UI tool
- an Android Gradle Plugin that runs before R8/ProGuard

## Highlights

- Presets for quick usage: `-low`, `-mid`, `-high`
- JSON config generation and loading
- Swing UI, launched by double-clicking the jar or using `-ui`
- Android app/library obfuscation through the Android Gradle Plugin Variant API
- ProGuard/R8-compatible Android mode enabled by default
- Generated keep rules for Bipbap Android output

## Command Line Usage

Launch the UI:

```shell
java -jar bipbap.jar -ui
```

Launching without arguments also opens the UI:

```shell
java -jar bipbap.jar
```

Generate a default config:

```shell
java -jar bipbap.jar -genconfig config.json
```

Run with a config:

```shell
java -jar bipbap.jar config.json
```

Run with a preset:

```shell
java -jar bipbap.jar -low
java -jar bipbap.jar -mid input.jar
java -jar bipbap.jar -high input.jar output.jar
```

Preset input/output defaults:

| Command | Input | Output |
|---------|-------|--------|
| `-high` | `input.jar` | `output.jar` |
| `-high input.jar` | `input.jar` | `input-obf.jar` |
| `-high input.jar output.jar` | `input.jar` | `output.jar` |

Additional CLI options:

| Option | Description |
|--------|-------------|
| `-mt:4` | Use 4 worker threads |
| `-mt` or `-mt:-1` | Use `Runtime.availableProcessors()` |
| `-auth=https://example.com` | Configure HWID authentication endpoint |

## Android Obfuscation

Bipbap supports Android app/library obfuscation through its Gradle plugin. The plugin runs before R8/ProGuard, uses Android-safe defaults, and can generate keep rules for Bipbap's encrypted constant layer.

See the full Android guide: [Android Gradle Plugin Usage](https://github.com/spartanb312/bipbap/blob/main/ANDROID.md).

## Presets

| Preset | Activated features |
|--------|--------------------|
| `-low` | CodeOptimize, ConstEncrypt, Renamer(LocalVar), HideCode |
| `-mid` | CodeOptimize, ConstEncrypt, Renamer(LocalVar, Field, Method), InvokeDynamics, HideCode |
| `-high` | CodeOptimize, ConstEncrypt, Renamer(LocalVar, Field, Method), InvokeDynamics, Crasher, HideCode |

## Features

### Obfuscation

- [x] HWIDAuthenticator
- [x] FieldRename
- [x] MethodRename
- [x] LocalVarRename
- [x] NumberEncrypt
- [x] StringEncrypt
- [x] InvokeDynamics
- [x] CodeHider
- [x] Watermark
- [x] Crasher

### Optimization

- [x] RemoveSource
- [x] RemoveInnerClass
- [x] RemoveDeadCodes
- [x] KotlinOptimize

## License

MIT License.
