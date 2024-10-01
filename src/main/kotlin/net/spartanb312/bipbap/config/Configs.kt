package net.spartanb312.bipbap.config

import com.google.gson.*
import net.spartanb312.bipbap.config.Configs.Settings.exclusions
import net.spartanb312.bipbap.process.Transformers
import org.objectweb.asm.tree.ClassNode
import java.io.*

object Configs {

    private val configs = mutableListOf<Configurable>()
    private val gsonPretty: Gson = GsonBuilder().setPrettyPrinting().create()

    object Settings : Configurable("Settings") {
        var input by setting("Input", "input.jar")
        var output by setting("Output", "output.jar")
        var libraries by setting("Libraries", listOf())
        var exclusions by setting(
            "Exclusions", listOf(
                "assets/",
                "baritone/",
                "club/minnced",
                "com/github/",
                "com/google/",
                "com/mojang/",
                "io/netty/",
                "io/github/",
                "it/unimi/",
                "javassist/",
                "javax/",
                "javafx/",
                "kotlin/",
                "kotlinx/",
                "org/jetbrains/",
                "org/lwjgl/",
                "org/spongepowered/",
                "org/intellij/",
                "org/joml/",
                "org/apache/",
                "org/ow2/",
                "net/fabricmc/",
                "net/minecraft/",
                "net/minecraftforge/",
                "net/java/"
            )
        )
        var fileRemovePrefix by setting("FileRemovePrefix", listOf())
        var fileRemoveSuffix by setting("FileRemoveSuffix", listOf())
    }

    init {
        configs.add(Settings)
        Transformers.forEach {
            configs.add(it)
        }
    }

    fun resetConfig() {
        configs.forEach { config ->
            config.getValues().forEach { value ->
                value.reset()
            }
        }
    }

    fun loadConfig(path: String) {
        val map = path.jsonMap
        configs.forEach {
            map[it.name]?.asJsonObject?.let { jo -> it.getValue(jo) }
        }
    }

    fun saveConfig(path: String) {
        val configFile = File(path)
        if (!configFile.exists()) {
            configFile.parentFile?.mkdirs()
            configFile.createNewFile()
        }
        JsonObject().apply {
            configs.forEach {
                add(it.name, it.saveValue())
            }
        }.saveToFile(configFile)
    }

    private val String.jsonMap: Map<String, JsonElement>
        get() {
            val loadJson = BufferedReader(FileReader(this))
            val map = mutableMapOf<String, JsonElement>()
            JsonParser.parseReader(loadJson).asJsonObject.entrySet().forEach {
                map[it.key] = it.value
            }
            loadJson.close()
            return map
        }

    fun JsonObject.saveToFile(file: File) {
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        val saveJSon = PrintWriter(FileWriter(file))
        saveJSon.println(gsonPretty.toJson(this))
        saveJSon.close()
    }

    inline val String.isExcluded get() = exclusions.any { this.startsWith(it) }
    inline val ClassNode.isExcluded get() = exclusions.any { name.startsWith(it) }
    inline val String.shouldRemove
        get() = Settings.fileRemovePrefix.any { startsWith(it) }
                || Settings.fileRemoveSuffix.any { endsWith(it) }

}