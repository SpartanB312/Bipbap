package net.spartanb312.bipbap.cli

import net.spartanb312.bipbap.process.impls.CodeOptimizer
import net.spartanb312.bipbap.process.impls.ConstantEncryptor
import net.spartanb312.bipbap.process.impls.InvokeDynamics
import net.spartanb312.bipbap.process.impls.MembersRenamer
import net.spartanb312.bipbap.process.impls.Miscellaneous

internal enum class Preset(val displayName: String) {
    LOW("Low intensity"),
    MID("Medium intensity"),
    HIGH("High intensity")
}

internal data class CliArgs(
    val input: String? = null,
    val output: String? = null,
    val config: String? = null,
    val genConfig: String? = null,
    val threads: Int? = null,
    val preset: Preset = Preset.LOW,
    val authUrl: String? = null,
    val showUi: Boolean = false,
    val showHelp: Boolean = false,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

internal fun Array<String>.readCliArgs(): CliArgs {
    var input: String? = null
    var output: String? = null
    var config: String? = null
    var genConfig: String? = null
    var threads: Int? = null
    var preset = Preset.LOW
    var authUrl: String? = null
    var showUi = false
    var showHelp = false
    val errors = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    var index = 0
    while (index < size) {
        val arg = this[index]
        val lower = arg.lowercase()
        when {
            lower == "-h" || lower == "--help" || lower == "help" -> showHelp = true
            lower == "-ui" || lower == "--ui" -> showUi = true
            lower == "-low" -> preset = Preset.LOW
            lower == "-mid" -> preset = Preset.MID
            lower == "-high" -> preset = Preset.HIGH
            lower == "-mt" || lower == "--mt" -> threads = Runtime.getRuntime().availableProcessors()
            lower.startsWith("-mt:") || lower.startsWith("--mt:") -> {
                threads = arg.substringAfter(':').readThreadCount(arg, errors)
            }

            lower.startsWith("-mt=") || lower.startsWith("--mt=") -> {
                threads = arg.substringAfter('=').readThreadCount(arg, errors)
            }

            lower == "-genconfig" || lower == "--genconfig" -> {
                genConfig = getOrNull(++index)?.takeIf { it.isNotBlank() && !it.startsWith("-") }
                    ?: run {
                        errors.add("Missing path after $arg.")
                        null
                    }
            }

            lower.startsWith("-genconfig=") || lower.startsWith("--genconfig=") -> {
                genConfig = arg.substringAfter('=').takeIf { it.isNotBlank() }
                    ?: run {
                        errors.add("Missing path after ${arg.substringBefore('=')}=.")
                        null
                    }
            }

            lower == "-auth" || lower == "--auth" -> {
                authUrl = getOrNull(++index)?.takeIf { it.isNotBlank() && !it.startsWith("-") }
                    ?: run {
                        errors.add("Missing URL after $arg.")
                        authUrl
                    }
            }

            lower.startsWith("-auth=") || lower.startsWith("--auth=") -> {
                authUrl = arg.substringAfter('=').takeIf { it.isNotBlank() }
                    ?: run {
                        errors.add("Missing URL after ${arg.substringBefore('=')}=.")
                        authUrl
                    }
            }

            lower.endsWith(".jar") -> {
                when {
                    input == null -> input = arg
                    output == null -> output = arg
                    else -> warnings.add("Ignored extra jar argument: $arg")
                }
            }

            lower.endsWith(".json") -> {
                if (config != null) warnings.add("Multiple config files were supplied; using $arg.")
                config = arg
            }

            else -> warnings.add("Unknown argument: $arg")
        }
        index++
    }

    if (config != null && (input != null || output != null)) {
        warnings.add("Config file was supplied; jar arguments will be used only if config loading fails.")
    }

    if (genConfig != null && (config != null || input != null || output != null || authUrl != null || threads != null || showUi || preset != Preset.LOW)) {
        warnings.add("Generating config only; run arguments will be ignored.")
    }

    return CliArgs(input, output, config, genConfig, threads, preset, authUrl, showUi, showHelp, errors, warnings)
}

private fun String.readThreadCount(arg: String, errors: MutableList<String>): Int? {
    val count = toIntOrNull()
    return when {
        count == null -> {
            errors.add("Invalid thread count in $arg.")
            null
        }

        count == -1 -> Runtime.getRuntime().availableProcessors()
        count > 0 -> count
        else -> {
            errors.add("Thread count in $arg must be greater than 0 or -1.")
            null
        }
    }
}

internal fun printUsage() {
    println("Usage:")
    println("  java -jar bipbap.jar -ui")
    println("  java -jar bipbap.jar -genconfig <config.json>")
    println("  java -jar bipbap.jar <config.json>")
    println("  java -jar bipbap.jar [-low|-mid|-high] [input.jar] [output.jar] [-mt|-mt:<threads>] [-auth <url>|-auth=<url>]")
}

internal fun applyPreset(preset: Preset) {
    when (preset) {
        Preset.HIGH -> {
            CodeOptimizer.enabled = true
            ConstantEncryptor.enabled = true
            InvokeDynamics.enabled = true
            MembersRenamer.enabled = true
            MembersRenamer.localVar = true
            MembersRenamer.field = true
            MembersRenamer.method = true
            Miscellaneous.enabled = true
            Miscellaneous.crasher = true
            Miscellaneous.hideCode = true
            Miscellaneous.watermark = true
        }

        Preset.MID -> {
            CodeOptimizer.enabled = true
            ConstantEncryptor.enabled = true
            InvokeDynamics.enabled = true
            MembersRenamer.enabled = true
            MembersRenamer.localVar = true
            MembersRenamer.field = true
            MembersRenamer.method = true
            Miscellaneous.enabled = true
            Miscellaneous.crasher = false
            Miscellaneous.hideCode = true
            Miscellaneous.watermark = true
        }

        Preset.LOW -> {
            CodeOptimizer.enabled = true
            ConstantEncryptor.enabled = true
            MembersRenamer.enabled = true
            MembersRenamer.localVar = true
            MembersRenamer.field = false
            MembersRenamer.method = false
            Miscellaneous.enabled = true
            Miscellaneous.crasher = false
            Miscellaneous.hideCode = true
            Miscellaneous.watermark = true
        }
    }
}
