package net.spartanb312.bipbap

import net.spartanb312.bipbap.cli.applyPreset
import net.spartanb312.bipbap.cli.printUsage
import net.spartanb312.bipbap.cli.readCliArgs
import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.process.Transformers
import net.spartanb312.bipbap.process.impls.HWIDAuthenticator
import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.ui.launchUi
import net.spartanb312.bipbap.utils.logging.Logger
import kotlin.system.measureTimeMillis

const val VERSION = "1.1.0"
const val SUBTITLE = "build 260505"
const val GITHUB = "https://github.com/SpartanB312/Bipbap"

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        launchUi()
        return
    }

    // Splash
    println(
        """
         ________  ___  ________  ________  ________  ________
        |\   __  \|\  \|\   __  \|\   __  \|\   __  \|\   __  \
        \ \  \|\ /\ \  \ \  \|\  \ \  \|\ /\ \  \|\  \ \  \|\  \
         \ \   __  \ \  \ \   ____\ \   __  \ \   __  \ \   ____\
          \ \  \|\  \ \  \ \  \___|\ \  \|\  \ \  \ \  \ \  \___|
           \ \_______\ \__\ \__\    \ \_______\ \__\ \__\ \__\
            \|_______|\|__|\|__|     \|_______|\|__|\|__|\|__|
        """.trimIndent()
    )
    println("==========================================================")
    println(" Bipbap $VERSION [$SUBTITLE]")
    println(" Github: $GITHUB")
    println("==========================================================")

    val cliArgs = args.readCliArgs()
    cliArgs.errors.forEach { Logger.info(it) }
    cliArgs.warnings.forEach { Logger.info(it) }
    if (cliArgs.showHelp || cliArgs.errors.isNotEmpty()) {
        printUsage()
        return
    }

    if (cliArgs.showUi) {
        launchUi()
        return
    }

    cliArgs.genConfig?.let { config ->
        Configs.resetConfig()
        Configs.saveConfig(config)
        Logger.info("Generated config: $config")
        return
    }

    // Read config
    var loadedConfig = false
    cliArgs.config?.let { config ->
        Logger.info("Using config: $config")
        try {
            Configs.resetConfig()
            Configs.loadConfig(config)
            Configs.saveConfig(config) // Clean up the config
            loadedConfig = true
        } catch (_: Exception) {
            Logger.info("Failed to read config $config! Using preset config.")
        }
    }

    if (!loadedConfig) {
        cliArgs.input?.let { input ->
            Configs.Settings.input = input
            Configs.Settings.output = cliArgs.output ?: (input.dropLast(4) + "-obf.jar")
        }
    }

    cliArgs.threads?.let { threads ->
        Configs.Settings.threads = threads
    }
    Logger.info("Using ${Configs.Settings.threads} thread(s)")

    // Loading presets
    if (!loadedConfig) {
        applyPreset(cliArgs.preset)
        Logger.info("Using presets: ${cliArgs.preset.displayName}")
    }

    // Authenticator injector
    cliArgs.authUrl?.let { url ->
        HWIDAuthenticator.enabled = true
        HWIDAuthenticator.onlineMode = true
        HWIDAuthenticator.onlineURL = url
        HWIDAuthenticator.encryptKey = "1186118611861186"
        HWIDAuthenticator.pools = 5
        HWIDAuthenticator.showHWIDWhenFailed = true
    }

    // Run process
    runInstance()
}

// Start obfuscation
fun runInstance() {
    Transformers.resetTransformers()
    val time = measureTimeMillis {
        WorkContext(Configs.Settings.input, Configs.Settings.libraries).apply {
            readJar()
            val obfTime = measureTimeMillis {
                Logger.info("Processing...")
                Transformers.forEach { if (it.enabled) with(it) { transform() } }
            }
            Logger.info("Took $obfTime ms to process!")
            Logger.info("Dumping to ${Configs.Settings.output}")
        }.dumpJar(Configs.Settings.output)
    }
    Logger.info("Finished in $time ms!")
}
