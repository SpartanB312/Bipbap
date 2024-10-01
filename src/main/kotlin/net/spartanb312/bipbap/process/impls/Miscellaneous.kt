package net.spartanb312.bipbap.process.impls

import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.resource.ResourceCache
import net.spartanb312.bipbap.utils.*
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

object Miscellaneous : Transformer("Miscellaneous") {

    var crasher by setting("Crasher", false)
    private val hideCode by setting("HideCode", true)
    private val watermark by setting("Watermark", true)
    private val watermarks by setting("Watermarks", listOf("PROTECTED BY EVERETT", "PROTECTED BY SPARTAN 1186"))
    private val exclusion by setting("Exclusion", listOf())

    override fun ResourceCache.transform() {
        Logger.info(" - Miscellaneous transformers...")
        if (hideCode) {
            val count = count {
                nonExcluded.asSequence()
                    .filter { !it.isAnnotation && it.name.notInList(exclusion) && it.checkMixin }
                    .forEach { classNode ->
                        pushSynthetic(classNode)
                        pushBridge(classNode)
                    }
            }.get()
            Logger.info("    Hided $count members")
        }
        if (crasher) {
            val count = count {
                nonExcluded.asSequence()
                    .filter { it.name.notInList(exclusion) && it.checkMixin }
                    .forEach { classNode ->
                        classNode.methods.forEach { methodNode ->
                            methodNode.signature = methodNode.signature.bigBrainSignature
                        }
                        classNode.fields.forEach { fieldNode ->
                            fieldNode.signature = fieldNode.signature.bigBrainSignature
                        }
                        classNode.signature = classNode.signature.bigBrainSignature
                        add()
                    }
            }.get()
            Logger.info("    Inserted $count crashers")
        }
        if (watermark) {
            val count = count {
                nonExcluded.asSequence()
                    .filter { !it.isInterface && it.name.notInList(exclusion) && it.checkMixin }
                    .forEach { classNode ->
                        classNode.fields = classNode.fields ?: arrayListOf()
                        val marker = watermarks.random()
                        when ((0..2).random()) {
                            0 -> classNode.fields.add(
                                FieldNode(
                                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                                    watermarks.random(),
                                    "Ljava/lang/String;",
                                    null,
                                    marker
                                )
                            )

                            1 -> classNode.fields.add(
                                FieldNode(
                                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                                    "_$marker _",
                                    "I",
                                    null,
                                    listOf(114514, 1919810, 69420, 911, 8964).random()
                                )
                            )

                            2 -> classNode.fields.add(
                                FieldNode(
                                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                                    watermarks.random(),
                                    "Ljava/lang/String;",
                                    null,
                                    marker
                                )
                            )
                        }
                        add(1)
                    }
            }.get()
            Logger.info("    Added $count watermarks")
        }
    }

    private val String?.bigBrainSignature get() = if (isNullOrEmpty()) massiveString else this

    private fun Counter.pushSynthetic(classNode: ClassNode) {
        if (Opcodes.ACC_SYNTHETIC and classNode.access == 0 && !classNode.hasAnnotations) {
            classNode.access = classNode.access or Opcodes.ACC_SYNTHETIC
        }

        classNode.methods.asSequence()
            .filter { Opcodes.ACC_SYNTHETIC and it.access == 0 }
            .forEach {
                it.access = it.access or Opcodes.ACC_SYNTHETIC
                add(1)
            }

        classNode.fields.asSequence()
            .filter { Opcodes.ACC_SYNTHETIC and it.access == 0 && !it.hasAnnotations }
            .forEach { fieldNode: FieldNode ->
                fieldNode.access = fieldNode.access or Opcodes.ACC_SYNTHETIC
                add(1)
            }
    }

    private fun Counter.pushBridge(classNode: ClassNode) {
        classNode.methods.asSequence()
            .filter { !it.isInitializer && !it.isAbstract && it.access and Opcodes.ACC_BRIDGE == 0 }
            .forEach {
                if (Opcodes.ACC_BRIDGE and it.access == 0) {
                    it.access = it.access or Opcodes.ACC_BRIDGE
                    add(1)
                }
            }
    }

}