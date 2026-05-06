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
        var threads by setting("Threads", 1)
        var libraries by setting("Libraries", listOf())
        var exclusions by setting(
            "Exclusions", listOf(
                "android/",
                "androidx/",
                "akka/",
                "arrow/",
                "assets/",
                "baritone/",
                "bolts/",
                "brave/",
                "butterknife/",
                "cafe/corrosion/",
                "ch/qos/",
                "clojure/",
                "club/minnced",
                "cloud/commandframework/",
                "co/elastic/",
                "com/amazon/",
                "com/amazonaws/",
                "com/airbnb/",
                "com/alibaba/",
                "com/android/",
                "com/auth0/",
                "com/azure/",
                "com/beust/",
                "com/bumptech/",
                "com/cedarsoftware/",
                "com/cleanroommc/",
                "com/ctc/",
                "com/demonwav/",
                "com/electronwill/",
                "com/esotericsoftware/",
                "com/fasterxml/",
                "com/facebook/",
                "com/flywaydb/",
                "com/formdev/",
                "com/github/",
                "com/google/",
                "com/gtnewhorizons/",
                "com/huawei/",
                "com/ibm/",
                "com/jcraft/",
                "com/jfoenix/",
                "com/llamalad7/",
                "com/maxmind/",
                "com/microsoft/",
                "com/moulberry/",
                "com/mojang/",
                "com/mongodb/",
                "com/mysql/",
                "com/nimbusds/",
                "com/oracle/",
                "com/pinterest/",
                "com/rabbitmq/",
                "com/rometools/",
                "com/squareup/",
                "com/sun/",
                "com/thoughtworks/",
                "com/typesafe/",
                "com/uber/",
                "com/unascribed/",
                "com/vladsch/",
                "com/zaxxer/",
                "cpw/mods/",
                "dagger/",
                "de/flapdoodle/",
                "de/undercouch/",
                "dev/architectury/",
                "dev/isxander/",
                "dev/latvian/",
                "dev/tr7zw/",
                "edu/umd/",
                "freemarker/",
                "io/micrometer/",
                "groovy/",
                "gnu/trove/",
                "imgui/",
                "io/arrow-kt/",
                "io/dropwizard/",
                "io/grpc/",
                "io/netty/",
                "io/github/",
                "io/jsonwebtoken/",
                "io/ktor/",
                "io/micronaut/",
                "io/opencensus/",
                "io/opentelemetry/",
                "io/prometheus/",
                "io/quarkus/",
                "io/r2dbc/",
                "io/reactivex/",
                "io/swagger/",
                "io/vertx/",
                "it/unimi/",
                "java/",
                "javassist/",
                "jakarta/",
                "javax/",
                "javafx/",
                "jdk/",
                "joptsimple/",
                "junit/",
                "kotlin/",
                "kotlinx/",
                "liquibase/",
                "lombok/",
                "me/jellysquid/",
                "me/shedaniel/",
                "mcp/",
                "mods/railcraft/",
                "net/caffeinemc/",
                "net/fabricmc/",
                "net/java/",
                "net/lenni0451/",
                "net/minecraft/",
                "net/minecraftforge/",
                "net/neoforged/",
                "net/ornithemc/",
                "net/bytebuddy/",
                "net/dv8tion/",
                "net/jpountz/",
                "net/techcable/",
                "net/thesilkminer/",
                "net/weavemc/",
                "okhttp3/",
                "okio/",
                "oracle/",
                "oshi/",
                "picocli/",
                "redis/clients/",
                "org/antlr/",
                "org/apache/",
                "org/assertj/",
                "org/aspectj/",
                "org/awaitility/",
                "org/bouncycastle/",
                "org/codehaus/",
                "org/checkerframework/",
                "org/conscrypt/",
                "org/eclipse/",
                "org/ejml/",
                "org/freemarker/",
                "org/glassfish/",
                "org/graalvm/",
                "org/hamcrest/",
                "org/h2/",
                "org/hibernate/",
                "org/intellij/",
                "org/jboss/",
                "org/jetbrains/",
                "org/jline/",
                "org/joda/",
                "org/jose4j/",
                "org/joml/",
                "org/json/",
                "org/jspecify/",
                "org/junit/",
                "org/lwjgl/",
                "org/mapstruct/",
                "org/mariadb/",
                "org/mockito/",
                "org/mongodb/",
                "org/mozilla/",
                "org/objectweb/",
                "org/objenesis/",
                "org/ow2/",
                "org/postgresql/",
                "org/projectlombok/",
                "org/quiltmc/",
                "org/rauschig/",
                "org/reflections/",
                "org/semver4j/",
                "org/slf4j/",
                "org/springframework/",
                "org/spongepowered/",
                "org/testcontainers/",
                "org/thymeleaf/",
                "org/tukaani/",
                "org/w3c/",
                "org/xml/",
                "org/yaml/",
                "team/chisel/",
                "retrofit2/",
                "reactor/",
                "scala/",
                "software/amazon/",
                "software/bernie/",
                "software/coley/",
                "zone/rong/",
                "sun/"
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
