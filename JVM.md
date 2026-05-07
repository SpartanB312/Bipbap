# JVM Gradle Plugin Usage

[Back to README](https://github.com/spartanb312/bipbap/blob/main/README.md)

Bipbap supports desktop Java/Kotlin JVM application obfuscation through the Gradle plugin:

```kotlin
id("net.spartanb312.bipbap.jvm")
```

The plugin reads the current project's `jar` task output and writes a separate obfuscated jar. It does not replace the original jar by default.

## Local Plugin Setup

When using Bipbap from a local checkout, add it to the JVM project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    includeBuild("../Bipbap")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
```

Apply the plugin in a Java or Kotlin JVM module:

```kotlin
plugins {
    application
    id("net.spartanb312.bipbap.jvm")
}
```

For a plain Java library/application module:

```kotlin
plugins {
    java
    id("net.spartanb312.bipbap.jvm")
}
```

## Configuration

```kotlin
bipbap {
    // Defaults to low.
    preset.set("low")

    // Defaults to one thread. Use -1 for Runtime.availableProcessors().
    threads.set(-1)

    // Defaults to true, so assemble/build also creates the obfuscated jar.
    attachToAssemble.set(true)

    // Defaults to true. Runtime dependency jars are used as libraries for class hierarchy resolution.
    includeRuntimeClasspath.set(true)

    // Optional extra library jars or directories containing jars.
    libraries.from(files("libs/some-library.jar"))

    // Optional extra package/class prefixes to skip.
    exclusions.add("com/example/generated/")

    // Optional HWID authentication URL.
    authUrl.set("https://example.com/auth.txt")
}
```

## Output

By default, if the normal jar is:

```text
build/libs/my-app.jar
```

Bipbap writes:

```text
build/libs/my-app-obf.jar
```

A build report is also written to:

```text
build/reports/bipbap/jvm.txt
```

Example report:

```text
[Bipbap] Enabled for JVM jar
[Bipbap] Preset: low
[Bipbap] Threads: 8
[Bipbap] Libraries: 12
[Bipbap] Input jar: .../build/libs/my-app.jar
[Bipbap] Output jar: .../build/libs/my-app-obf.jar
```

## Custom Input Or Output

You can override the input and output jars:

```kotlin
bipbap {
    inputJar.set(layout.buildDirectory.file("libs/custom.jar"))
    outputJar.set(layout.buildDirectory.file("libs/custom-obf.jar"))
}
```

## Using A JSON Config

For full transformer configuration, point the plugin at a Bipbap JSON config:

```kotlin
bipbap {
    configFile.set(layout.projectDirectory.file("bipbap.json"))
}
```

When `configFile` is set, the config controls transformer settings. The Gradle task still overrides input jar, output jar, threads, libraries, exclusions, and optional `authUrl`.

## Tasks

The plugin registers:

```text
bipbapObfuscateJar
```

Run it directly:

```shell
./gradlew bipbapObfuscateJar
```

Or run the normal build:

```shell
./gradlew build
```

When `attachToAssemble` is `true`, `assemble` and `build` will also create the obfuscated jar.
