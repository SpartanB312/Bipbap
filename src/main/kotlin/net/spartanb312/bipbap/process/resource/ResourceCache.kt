package net.spartanb312.bipbap.process.resource

import com.google.gson.JsonObject
import net.spartanb312.bipbap.config.Configs.isExcluded
import net.spartanb312.bipbap.config.Configs.shouldRemove
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ResourceCache(private val input: String, private val libs: List<String>) {

    val classes = mutableMapOf<String, ClassNode>()
    val resources = mutableMapOf<String, ByteArray>()

    val nonExcluded get() = classes.filter { !it.key.isExcluded }.values

    fun readJar() {
        readInput()
    }

    fun dumpJar(targetFile: String) = ZipOutputStream(File(targetFile).outputStream()).apply {
        Logger.info("Writing classes...")
        for (classNode in classes.values) {
            if (classNode.name == "module-info" || classNode.name.shouldRemove) continue
            val byteArray = try {
                ClassDumper().apply {
                    classNode.accept(this)
                }.toByteArray()
            } catch (exception: Exception) {
                Logger.error("Failed to dump class ${classNode.name}.")
                exception.printStackTrace()
                continue
            }
            putNextEntry(ZipEntry(classNode.name + ".class"))
            write(byteArray)
            closeEntry()
        }

        Logger.info("Writing resources...")
        for ((name, bytes) in resources) {
            if (name.shouldRemove) continue
            putNextEntry(ZipEntry(name))
            write(bytes)
            closeEntry()
        }
        close()
    }

    private fun readInput() {
        Logger.info("Reading $input")
        JarFile(File(input)).apply {
            entries().asSequence()
                .filter { !it.isDirectory }
                .forEach {
                    if (it.name.endsWith(".class")) {
                        kotlin.runCatching {
                            ClassReader(getInputStream(it)).apply {
                                val classNode = ClassNode()
                                accept(classNode, ClassReader.EXPAND_FRAMES)
                                classes[classNode.name] = classNode
                            }
                        }
                    } else resources[it.name] = getInputStream(it).readBytes()
                }
        }
    }

    fun applyRemap(mappings: Map<String, String>) {
        val remapper = SimpleRemapper(mappings)
        for ((name, node) in classes.toMutableMap()) {
            val copy = ClassNode()
            val adapter = ClassRemapper(copy, remapper)
            node.accept(adapter)
            classes[name] = copy
        }
    }

}

class ClassDumper : ClassWriter(COMPUTE_MAXS)