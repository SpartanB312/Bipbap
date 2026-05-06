package net.spartanb312.bipbap.process.resource

import net.spartanb312.bipbap.config.Configs.isExcluded
import net.spartanb312.bipbap.config.Configs.shouldRemove
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WorkContext(private val input: String, private val libs: List<String>) {

    val classes = mutableMapOf<String, ClassNode>()
    val libraries = mutableMapOf<String, ClassNode>()
    val resources = mutableMapOf<String, ByteArray>()

    val nonExcluded get() = classes.filter { !it.key.isExcluded }.values
    val allClasses
        get() = mutableListOf<ClassNode>().apply {
            addAll(classes.values)
            addAll(libraries.values)
        }

    fun readJar() {
        readInput()
        readLibs()
    }

    fun dumpJar(targetFile: String) = ZipOutputStream(File(targetFile).outputStream()).apply {
        Logger.info("Writing classes...")
        val hierarchy = Hierarchy(this@WorkContext)
        hierarchy.build()
        for (classNode in classes.values) {
            if (classNode.name == "module-info" || classNode.name.shouldRemove) continue
            val byteArray = try {
                ClassDumper(this@WorkContext, hierarchy, true).apply {
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

    private fun readLibs() {
        Logger.info("Reading Libraries...")
        libs.map { File(it) }.forEach { file ->
            if (file.isDirectory) {
                readDirectory(file)
            } else {
                readJar(JarFile(file))
            }
        }
    }

    private fun readDirectory(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                readDirectory(file)
            } else {
                readJar(JarFile(file))
            }
        }
    }

    private fun readJar(jar: JarFile) {
        Logger.info("  - ${jar.name}")
        jar.entries().asSequence().filter { !it.isDirectory }.forEach {
            if (it.name.endsWith(".class")) {
                kotlin.runCatching {
                    ClassReader(jar.getInputStream(it)).apply {
                        val classNode = ClassNode()
                        accept(classNode, ClassReader.EXPAND_FRAMES)
                        libraries[classNode.name] = classNode
                    }
                }
            }
        }
    }

    fun addClass(classNode: ClassNode) {
        classes[classNode.name] = classNode
    }

    fun removeClass(classNode: ClassNode) {
        classes.remove(classNode.name)
    }

    fun getClassNode(name: String): ClassNode? {
        return classes[name] ?: libraries[name] ?: readInRuntime(name)
    }

    fun readInRuntime(name: String): ClassNode? {
        return try {
            val classNode = ClassNode()
            ClassReader(name).apply {
                accept(classNode, ClassReader.EXPAND_FRAMES)
                libraries[classNode.name] = classNode
            }
            classNode
        } catch (ignore: Exception) {
            null
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