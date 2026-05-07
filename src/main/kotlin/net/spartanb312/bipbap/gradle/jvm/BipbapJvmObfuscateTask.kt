package net.spartanb312.bipbap.gradle.jvm

import net.spartanb312.bipbap.cli.Preset
import net.spartanb312.bipbap.cli.applyPreset
import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.process.Transformers
import net.spartanb312.bipbap.process.impls.HWIDAuthenticator
import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.utils.logging.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class BipbapJvmObfuscateTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputJar: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:OutputFile
    abstract val summaryFile: RegularFileProperty

    @get:Input
    abstract val enabledProperty: Property<Boolean>

    @get:Input
    abstract val preset: Property<String>

    @get:Input
    abstract val threads: Property<Int>

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val authUrl: Property<String>

    @get:Input
    abstract val extraExclusions: ListProperty<String>

    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    init {
        group = "bipbap"
        description = "Obfuscates the JVM jar output with Bipbap."
    }

    @TaskAction
    fun obfuscate() {
        val input = inputJar.get().asFile
        val output = outputJar.get().asFile
        if (!enabledProperty.get()) {
            output.parentFile.mkdirs()
            input.copyTo(output, overwrite = true)
            writeSummary(
                listOf(
                    "[Bipbap] Disabled for JVM jar, copied input through.",
                    "[Bipbap] Input jar: ${input.absolutePath}",
                    "[Bipbap] Output jar: ${output.absolutePath}"
                )
            )
            return
        }

        synchronized(lock) {
            val threadCount = resolveThreads()
            val libraryFiles = resolveLibraryFiles()
            writeSummary(summaryLines(input, output, threadCount, libraryFiles))
            configureBipbap(input, output, threadCount, libraryFiles)

            output.parentFile.mkdirs()
            WorkContext(input.absolutePath, libraryFiles.map { it.absolutePath }).use { context ->
                context.readJar()
                Logger.info("Using JVM Gradle plugin settings: ${settingsName()}")
                Transformers.forEach { if (it.enabled) with(it) { context.transform() } }
                context.dumpJar(output.absolutePath)
            }
        }
    }

    private fun configureBipbap(input: File, output: File, threadCount: Int, libraryFiles: List<File>) {
        Configs.resetConfig()
        Transformers.resetTransformers()
        if (configFile.isPresent) {
            Configs.loadConfig(configFile.get().asFile.absolutePath)
        } else {
            applyPreset(resolvePreset())
        }
        Configs.Settings.input = input.absolutePath
        Configs.Settings.output = output.absolutePath
        Configs.Settings.threads = threadCount
        Configs.Settings.libraries = libraryFiles.map { it.absolutePath }
        Configs.Settings.exclusions += extraExclusions.get()
        authUrl.orNull?.takeIf { it.isNotBlank() }?.let {
            HWIDAuthenticator.onlineURL = it
        }
    }

    private fun resolveThreads(): Int {
        val value = threads.get()
        return if (value == -1) Runtime.getRuntime().availableProcessors()
        else value.coerceAtLeast(1)
    }

    private fun resolvePreset(): Preset {
        return when (preset.get().lowercase()) {
            "high" -> Preset.HIGH
            "mid", "medium" -> Preset.MID
            else -> Preset.LOW
        }
    }

    private fun resolveLibraryFiles(): List<File> {
        return libraries.files
            .flatMap { file ->
                when {
                    file.isFile && file.extension.equals("jar", ignoreCase = true) -> listOf(file)
                    file.isDirectory -> file.walkTopDown()
                        .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
                        .toList()

                    else -> emptyList()
                }
            }
            .distinctBy { it.absolutePath }
    }

    private fun summaryLines(input: File, output: File, threadCount: Int, libraryFiles: List<File>): List<String> {
        return listOf(
            "[Bipbap] Enabled for JVM jar",
            "[Bipbap] Settings: ${settingsName()}",
            "[Bipbap] Threads: $threadCount",
            "[Bipbap] Libraries: ${libraryFiles.size}",
            "[Bipbap] Input jar: ${input.absolutePath}",
            "[Bipbap] Output jar: ${output.absolutePath}"
        )
    }

    private fun settingsName(): String {
        return if (configFile.isPresent) "config:${configFile.get().asFile.absolutePath}"
        else "preset:${preset.get().lowercase()}"
    }

    private fun writeSummary(lines: List<String>) {
        lines.forEach { logger.quiet(it) }
        val file = summaryFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
    }

    companion object {
        private val lock = Any()
    }
}
