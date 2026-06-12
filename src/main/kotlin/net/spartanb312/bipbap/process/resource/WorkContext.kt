package net.spartanb312.bipbap.process.resource

import net.spartanb312.bipbap.config.Configs
import net.spartanb312.bipbap.config.Configs.isExcluded
import net.spartanb312.bipbap.config.Configs.shouldRemove
import net.spartanb312.bipbap.utils.ConcurrentWorker
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import org.objectweb.asm.tree.ClassNode
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WorkContext(
    private val input: String,
    private val libs: List<String>
) : ConcurrentWorker(resolveThreadCount()), Closeable {

    val classes = ConcurrentHashMap<String, ClassNode>()
    val libraries = ConcurrentHashMap<String, ClassNode>()
    val resources = ConcurrentHashMap<String, ByteArray>()

    val nonExcluded get() = classes.filter { !it.key.isExcluded }.values
    val allClasses
        get() = mutableListOf<ClassNode>().apply {
            addAll(classes.values)
            addAll(libraries.values)
        }

    val mapping = mutableMapOf<String, String>()
    val revMapping = mutableMapOf<String, String>()
    var computeFrames = false

    fun getPrevName(obfName: String): String {
        return revMapping.getOrDefault(obfName, obfName)
    }

    fun readJar() {
        readInput()
        readLibs()
    }

    fun dumpJar(targetFile: String) = ZipOutputStream(File(targetFile).outputStream()).apply {
        Logger.info("Writing classes...")
        val hierarchy = Hierarchy(this@WorkContext)
        hierarchy.build()
        val bytes = ConcurrentHashMap<ZipEntry, ByteArray>()
        parallelForEach(classes.values) { classNode ->
            if (classNode.name == "module-info" || classNode.name.shouldRemove) return@parallelForEach
            val byteArray = try {
                ClassDumper(this@WorkContext, hierarchy, computeFrames).apply {
                    classNode.accept(this)
                }.toByteArray()
            } catch (exception: Exception) {
                Logger.error("Failed to dump class ${classNode.name}.")
                exception.printStackTrace()
                if (computeFrames) ClassDumper(this@WorkContext, hierarchy, false).apply {
                    classNode.accept(this)
                }.toByteArray() else return@parallelForEach
            }
            bytes[ZipEntry(classNode.name + ".class")] = byteArray
        }
        for ((entry, byteArray) in bytes) {
            putNextEntry(entry)
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
        } catch (_: Exception) {
            null
        }
    }

    fun applyRemap(mappings: Map<String, String>) {
        val remapper = SimpleRemapper(Opcodes.ASM9, mappings)
        for ((name, node) in classes.toMutableMap()) {
            val copy = ClassNode()
            val adapter = ClassRemapper(copy, remapper)
            node.accept(adapter)
            classes[name] = copy
        }
    }

    override fun close() {
        cancelAndClose()
    }

}

private fun resolveThreadCount(): Int {
    val threads = Configs.Settings.threads
    return if (threads == -1) Runtime.getRuntime().availableProcessors()
    else threads.coerceAtLeast(1)
}
