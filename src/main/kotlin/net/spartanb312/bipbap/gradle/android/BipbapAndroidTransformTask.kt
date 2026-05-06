package net.spartanb312.bipbap.gradle.android

import net.spartanb312.bipbap.cli.Preset
import net.spartanb312.bipbap.cli.applyPreset
import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.process.Transformers
import net.spartanb312.bipbap.process.impls.CodeOptimizer
import net.spartanb312.bipbap.process.impls.HWIDAuthenticator
import net.spartanb312.bipbap.process.impls.InvokeDynamics
import net.spartanb312.bipbap.process.impls.MembersRenamer
import net.spartanb312.bipbap.process.impls.Miscellaneous
import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.utils.logging.Logger
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

abstract class BipbapAndroidTransformTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:OutputFile
    abstract val summaryFile: RegularFileProperty

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    abstract val enabledProperty: Property<Boolean>

    @get:Input
    abstract val preset: Property<String>

    @get:Input
    abstract val threads: Property<Int>

    @get:Input
    abstract val safeMode: Property<Boolean>

    @get:Input
    abstract val proguardCompatible: Property<Boolean>

    @get:Input
    abstract val extraExclusions: ListProperty<String>

    init {
        group = "bipbap"
        description = "Obfuscates Android project classes with Bipbap."
    }

    @TaskAction
    fun transform() {
        if (!enabledProperty.get()) {
            val outputJar = output.get().asFile
            writeSummary(
                listOf(
                    "[Bipbap] Disabled for ${variantName.get()}, passing classes through.",
                    "[Bipbap] Output transformed classes for R8: ${outputJar.absolutePath}"
                )
            )
            mergeInputs(output.get().asFile)
            return
        }

        synchronized(lock) {
            val inputJar = temporaryDir.resolve("bipbap-android-input.jar")
            val outputJar = output.get().asFile
            val threadCount = resolveThreads()
            writeSummary(summaryLines(outputJar, threadCount))
            mergeInputs(inputJar)
            configureBipbap(inputJar, outputJar, threadCount)

            val context = WorkContext(inputJar.absolutePath, emptyList()).apply {
                readJar()
            }
            excludeAndroidGeneratedClasses(context)
            Logger.info("Using Android Gradle plugin preset: ${preset.get().lowercase()}")
            Transformers.forEach { if (it.enabled) with(it) { context.transform() } }
            context.dumpJar(outputJar.absolutePath)
        }
    }

    private fun summaryLines(outputJar: File, threadCount: Int): List<String> = listOf(
        "[Bipbap] Enabled for ${variantName.get()}",
        "[Bipbap] Preset: ${preset.get().lowercase()}",
        "[Bipbap] Threads: $threadCount",
        "[Bipbap] Safe mode: ${safeMode.get()}",
        "[Bipbap] ProGuard/R8 compatible: ${proguardCompatible.get()}",
        "[Bipbap] Output transformed classes for R8: ${outputJar.absolutePath}"
    )

    private fun writeSummary(lines: List<String>) {
        lines.forEach { logger.quiet(it) }
        val file = summaryFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(lines.joinToString(System.lineSeparator(), postfix = System.lineSeparator()))
    }

    private fun configureBipbap(inputJar: File, outputJar: File, threadCount: Int) {
        Configs.resetConfig()
        Transformers.resetTransformers()
        Configs.Settings.input = inputJar.absolutePath
        Configs.Settings.output = outputJar.absolutePath
        Configs.Settings.threads = threadCount
        Configs.Settings.exclusions += extraExclusions.get()
        applyPreset(resolvePreset())

        if (proguardCompatible.get() || safeMode.get()) {
            // Disable member renamers
            MembersRenamer.field = false
            MembersRenamer.method = false
        }
        if (safeMode.get()) {
            CodeOptimizer.enabled = false
            Miscellaneous.enabled = false
        }

        Miscellaneous.crasher = false
        HWIDAuthenticator.enabled = false
        InvokeDynamics.enabled = false
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

    private fun excludeAndroidGeneratedClasses(context: WorkContext) {
        val generated = context.classes.keys.asSequence()
            .filter {
                it.endsWith("/R")
                        || it.contains("/R$")
                        || it.endsWith("/BuildConfig")
                        || it.endsWith("/Manifest")
            }
            .map {
                when {
                    it.endsWith("/R") -> it
                    it.contains("/R$") -> it.substringBefore("/R$") + "/R"
                    else -> it
                }
            }
            .distinct()
            .toList()
        Configs.Settings.exclusions = Configs.Settings.exclusions + generated
    }

    private fun mergeInputs(target: File) {
        target.parentFile.mkdirs()
        val seen = mutableSetOf<String>()
        JarOutputStream(target.outputStream()).use { output ->
            allDirectories.get().forEach { directory ->
                val root = directory.asFile
                if (root.exists()) {
                    root.walkTopDown()
                        .filter { it.isFile }
                        .forEach { file ->
                            val entryName =
                                root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                            output.putFile(entryName, file.readBytes(), seen)
                        }
                }
            }
            allJars.get().forEach { jar ->
                JarFile(jar.asFile).use { jarFile ->
                    jarFile.entries().asSequence()
                        .filter { !it.isDirectory }
                        .forEach { entry ->
                            output.putFile(entry.name, jarFile.getInputStream(entry).readBytes(), seen)
                        }
                }
            }
        }
    }

    private fun JarOutputStream.putFile(name: String, bytes: ByteArray, seen: MutableSet<String>) {
        if (!seen.add(name)) return
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    companion object {
        private val lock = Any()
    }
}
