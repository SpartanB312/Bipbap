package net.spartanb312.bipbap

import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.process.Transformers
import net.spartanb312.bipbap.process.impls.*
import net.spartanb312.bipbap.process.resource.ResourceCache
import net.spartanb312.bipbap.utils.logging.Logger
import kotlin.system.measureTimeMillis

const val VERSION = "1.0.0"
const val SUBTITLE = "build 241001"
const val GITHUB = "https://github.com/SpartanB312/Bipbap"

fun main(args: Array<String>) {
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

    // Authenticator injector
    args.firstOrNull { it.startsWith("-auth=") }?.let {
        val url = it.substringAfter("-auth=")
        if (url.isNotEmpty()) {
            HWIDAuthenticator.enabled = true
            HWIDAuthenticator.onlineMode = true
            HWIDAuthenticator.onlineURL = url
            HWIDAuthenticator.encryptKey = "1186118611861186"
            HWIDAuthenticator.pools = 5
            HWIDAuthenticator.showHWIDWhenFailed = true
        }
    }

    // Preset level
    var levels = 0
    for (arg in args) {
        if (arg.lowercase() == "-high") levels = 3
        else if (arg.lowercase() == "-mid") levels = 2
        else if (arg.lowercase() == "-low") levels = 1
    }
    args.firstOrNull { it.endsWith(".jar") }?.let {
        Configs.Settings.input = it
        Configs.Settings.output = it.removeSuffix(".jar") + "-obf.jar"
    }

    // Read config
    var loadedConfig = false
    args.firstOrNull { it.endsWith(".json") }?.let { config ->
        Logger.info("Using config: $config")
        try {
            Configs.resetConfig()
            Configs.loadConfig(config)
            Configs.saveConfig(config) // Clean up the config
            loadedConfig = true
            levels = 0
        } catch (ignore: Exception) {
            Logger.info("Failed to read config $config! Using preset config.")
            levels = 1
        }
    }
    if (!loadedConfig && levels == 0) levels = 1

    // Loading presets
    when (levels) {
        3 -> {
            CodeOptimizer.enabled = true
            ConstantEncryptor.enabled = true
            InvokeDynamics.enabled = true
            MembersRenamer.enabled = true
            MembersRenamer.localVar = true
            MembersRenamer.field = true
            MembersRenamer.method = true
            Miscellaneous.enabled = true
            Miscellaneous.crasher = true
            Logger.info("Using presets: High intensity")
        }

        2 -> {
            CodeOptimizer.enabled = true
            ConstantEncryptor.enabled = true
            InvokeDynamics.enabled = true
            MembersRenamer.enabled = true
            MembersRenamer.localVar = true
            MembersRenamer.field = true
            MembersRenamer.method = true
            Miscellaneous.enabled = true
            Miscellaneous.crasher = false
            Logger.info("Using presets: Medium intensity")
        }

        1 -> {
            CodeOptimizer.enabled = true
            ConstantEncryptor.enabled = true
            MembersRenamer.enabled = true
            MembersRenamer.localVar = true
            MembersRenamer.field = false
            MembersRenamer.method = false
            Miscellaneous.enabled = true
            Miscellaneous.crasher = false
            Logger.info("Using presets: Low intensity")
        }
    }

    // Run process
    val time = measureTimeMillis {
        ResourceCache(Configs.Settings.input, Configs.Settings.libraries).apply {
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