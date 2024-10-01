package net.spartanb312.bipbap.process.impls

import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.resource.NameGenerator
import net.spartanb312.bipbap.process.resource.ResourceCache
import net.spartanb312.bipbap.utils.*
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.util.*

object MembersRenamer : Transformer("MembersRenamer") {

    var localVar by setting("LocalVariable", true)
    var field by setting("Field", false)
    var method by setting("Method", false)
    private val exclusion by setting(
        "Exclusion", listOf(
            "net/spartanb312/Example.field",
            "net/spartanb312/Example.method()V"
        )
    )

    private val blackListFields = listOf("INSTANCE", "Companion")

    override fun ResourceCache.transform() {
        Logger.info(" - Encrypting members...")
        var localCount = 0
        var fieldCount = 0
        var methodCount = 0
        if (localVar) {
            nonExcluded.forEach { classNode ->
                classNode.methods.forEach { methodNode ->
                    methodNode.localVariables?.forEach { localVarNode ->
                        localVarNode.name = NameGenerator.nextName()
                        localCount++
                    }
                }
            }
            Logger.info("    Renamed $localCount local variables")
        }
        if (field) {
            val mappings = HashMap<String, String>()
            val fields: MutableList<Pair<FieldNode, ClassNode>> = ArrayList()
            nonExcluded.asSequence()
                .filter { it.checkMixin }
                .forEach { fields.addAll(it.fields.map { field -> field to it }) }
            fields.shuffle()
            for ((fieldNode, owner) in fields) {
                if (fieldNode.name.inList(blackListFields)) continue
                if (!fieldNode.checkMixin) continue
                val name = NameGenerator.nextName()
                val stack: Stack<ClassNode> = Stack()
                stack.add(owner)
                while (stack.size > 0) {
                    val classNode = stack.pop()
                    val key = classNode.name + "." + fieldNode.name
                    if (key.notInList(exclusion)) mappings[key] = name
                    classes.values.forEach {
                        if (it.superName == classNode.name || it.interfaces.contains(classNode.name)) stack.add(it)
                    }
                }
                fieldCount++
            }
            applyRemap(mappings)
            Logger.info("    Renamed $fieldCount fields")
        }
        if (method) {
            val mappings = HashMap<String, String>()
            nonExcluded.asSequence()
                .filter { !it.isInterface && !it.isEnum && !it.isAnnotation }
                .filter { it.checkMixin }
                .forEach { classNode ->
                    for (methodNode in classNode.methods) {
                        if (!methodNode.isPrivate) continue
                        if (!methodNode.checkMixin) continue
                        if (methodNode.isInitializer) continue
                        if (methodNode.isMainMethod) continue
                        if (methodNode.isNative) continue
                        val combined = classNode.name + "." + methodNode.name + methodNode.desc
                        if (combined.inList(exclusion)) continue
                        mappings[combined] = NameGenerator.nextName()
                        methodCount++
                    }
                }
            applyRemap(mappings)
            Logger.info("    Renamed $methodCount methods")
        }
    }

}