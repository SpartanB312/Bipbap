package net.spartanb312.bipbap.process.impls

import com.google.gson.JsonObject
import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.config.Configs.saveToFile
import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.resource.NameGenerator
import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.utils.checkMixin
import net.spartanb312.bipbap.utils.dot
import net.spartanb312.bipbap.utils.inList
import net.spartanb312.bipbap.utils.logging.Logger
import net.spartanb312.bipbap.utils.notInList
import net.spartanb312.bipbap.utils.splash
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object ClassRenamer : Transformer("ClassRenamer") {

    private val parent by setting("Package", "net/spartanb312/obf/")
    private val saveMapping by setting("SaveMapping", true)
    private val manifestReplace by setting(
        "ManifestReplace", listOf(
            "Main-Class:",
            "Launch-Entry:"
        )
    )
    private val projectPackages by setting(
        "ProjectPackages",
        listOf("net/spartanb312/example")
    )
    private val exclusion by setting(
        "Exclusion", listOf(
            "net/spartanb312/example/",
            "net/spartanb312/test/Test",
        )
    )

    override fun WorkContext.transform() {
        Logger.info(" - Renaming classes...")
        nonExcluded.asSequence()
            .filter { it.checkMixin }
            .forEach {
                if (it.name.inList(exclusion) || it.name.notInList(projectPackages)) return@forEach
                val newName = parent + NameGenerator.nextName(15)
                mapping[it.name] = newName
                revMapping[newName] = it.name
            }

        Logger.info("    Applying mappings for classes...")
        applyRemap(mapping)
        if (saveMapping) {
            val obj = JsonObject()
            mapping.forEach { (prev, new) -> obj.addProperty(prev, new) }
            val dir =
                "mappings/${SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(Date())}" +
                        " ${File(Configs.Settings.input).name}/"
            obj.saveToFile(File("${dir}classes.json"))
        }
        processManifest()
        Logger.info("    Renamed ${mapping.size} classes")
    }

    private fun WorkContext.processManifest() {
        val manifestFile = resources["META-INF/MANIFEST.MF"] ?: return
        Logger.info("    Processing MANIFEST.MF...")
        val manifest = mutableListOf<String>()
        manifestFile.decodeToString().split("\n").forEach { line ->
            var final = line
            manifestReplace.forEach { prefixRaw ->
                val prefix = prefixRaw.removeSuffix(" ")
                if (line.startsWith(prefix)) {
                    val remaining = line.substringAfter(prefix)
                        .substringAfter(" ")
                        .replace("\r", "")
                        .splash
                    val obfName = mapping.getOrDefault(remaining, null)
                    if (obfName != null) {
                        final = "$prefix ${obfName.dot}"
                        Logger.info("    Replaced manifest $final")
                    }
                }
            }
            manifest.add(final)
        }
        resources["META-INF/MANIFEST.MF"] = manifest.joinToString("\n").toByteArray()
    }
}