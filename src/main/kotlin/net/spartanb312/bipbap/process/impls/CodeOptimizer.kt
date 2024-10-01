package net.spartanb312.bipbap.process.impls

import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.resource.ResourceCache
import net.spartanb312.bipbap.utils.*
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

object CodeOptimizer : Transformer("CodeOptimizer") {

    private val removeSource by setting("RemoveSource", true)
    private val removeInnerClass by setting("RemoveInnerClass", true)
    private val removeDeadCodes by setting("RemoveDeadCodes", true)
    private val kotlinOptimize by setting("KotlinOptimize", true)
    private val exclusion by setting("Exclusion", listOf())

    override fun ResourceCache.transform() {
        Logger.info(" - Optimizing bytecodes...")
        if (removeSource) {
            val sourceCount = count {
                nonExcluded.asSequence()
                    .filter { it.name.notInList(exclusion) && it.checkMixin }
                    .forEach { classNode ->
                        classNode.sourceDebug = null
                        classNode.sourceFile = null
                        add(2)
                        classNode.methods.forEach { methodNode ->
                            methodNode.instructions.toList().forEach { insn ->
                                if (insn is LineNumberNode) {
                                    methodNode.instructions.remove(insn)
                                    add()
                                }
                            }
                        }
                    }
            }.get()
            Logger.info("    Removed $sourceCount source debug infos")
        }
        if (removeInnerClass) {
            val innerClassCount = count {
                nonExcluded.asSequence()
                    .filter { it.name.notInList(exclusion) && it.checkMixin }
                    .forEach { classNode ->
                        classNode.outerClass = null
                        classNode.outerMethod = null
                        classNode.outerMethodDesc = null
                        add(classNode.innerClasses.size)
                        classNode.innerClasses.clear()
                    }
            }.get()
            Logger.info("    Removed $innerClassCount inner classes")
        }
        if (removeDeadCodes) {
            val deadCodesCount = count {
                nonExcluded.asSequence()
                    .filter { it.name.notInList(exclusion) }
                    .forEach { classNode ->
                        classNode.methods.toList().asSequence()
                            .filter { !it.isNative && !it.isAbstract }
                            .forEach { methodNode ->
                                for (it in methodNode.instructions.toList()) {
                                    if (it is InsnNode) {
                                        if (it.opcode == Opcodes.POP) {
                                            val pre = it.previous ?: continue
                                            if (pre.opcode == Opcodes.ILOAD
                                                || pre.opcode == Opcodes.FLOAD
                                                || pre.opcode == Opcodes.ALOAD
                                            ) {
                                                methodNode.instructions.remove(pre)
                                                methodNode.instructions.remove(it)
                                                add(2)
                                            }
                                        } else if (it.opcode == Opcodes.POP2) {
                                            val pre = it.previous ?: continue
                                            if (pre.opcode == Opcodes.DLOAD
                                                || pre.opcode == Opcodes.LLOAD
                                            ) {
                                                methodNode.instructions.remove(pre)
                                                methodNode.instructions.remove(it)
                                                add(2)
                                            } else if (pre.opcode == Opcodes.ILOAD
                                                || pre.opcode == Opcodes.FLOAD
                                                || pre.opcode == Opcodes.ALOAD
                                            ) {
                                                val prePre = pre.previous ?: continue
                                                if (prePre.opcode == Opcodes.ILOAD
                                                    || prePre.opcode == Opcodes.FLOAD
                                                    || prePre.opcode == Opcodes.ALOAD
                                                ) {
                                                    methodNode.instructions.remove(prePre)
                                                    methodNode.instructions.remove(pre)
                                                    methodNode.instructions.remove(it)
                                                    add(3)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                    }
            }.get()
            Logger.info("    Removed $deadCodesCount dead codes")
        }
        if (kotlinOptimize) {
            val intrinsicsCount = count {
                nonExcluded.asSequence()
                    .filter { it.name.notInList(exclusion) }
                    .forEach { classNode ->
                        classNode.methods.forEach { methodNode ->
                            val replace = mutableListOf<AbstractInsnNode>()
                            methodNode.instructions.forEach { insnNode ->
                                if (
                                    insnNode is MethodInsnNode
                                    && insnNode.opcode == Opcodes.INVOKESTATIC
                                    && insnNode.owner == "kotlin/jvm/internal/Intrinsics"
                                ) {
                                    val removeSize = intrinsicsRemoveMethods[insnNode.name + insnNode.desc] ?: 0
                                    if (removeSize > 0 && intrinsicsRemoval.contains(insnNode.name)) {
                                        replace.removeLast()
                                        repeat(removeSize) {
                                            replace.add(InsnNode(Opcodes.POP))
                                        }
                                        add()
                                    } else {
                                        if (intrinsicsReplaceMethods.contains(insnNode.name + insnNode.desc)) {
                                            val ldc = replace.last()
                                            if (ldc is LdcInsnNode) {
                                                ldc.cst = "REMOVED BY BIPBAP"
                                                add(1)
                                            }
                                        }
                                        replace.add(insnNode)
                                    }
                                } else {
                                    replace.add(insnNode)
                                }
                            }
                            methodNode.instructions.clear()
                            replace.forEach { methodNode.instructions.add(it) }
                        }
                    }
            }.get()
            Logger.info("    Removed $intrinsicsCount kotlin intrinsics checks")
            val annotationCount = count {
                nonExcluded.asSequence()
                    .filter { it.visibleAnnotations != null && it.name.notInList(exclusion) }
                    .forEach { classNode ->
                        fun MutableList<AnnotationNode>.removeCheck() {
                            toList().forEach {
                                if (
                                    it.desc.startsWith("Lkotlin/jvm/internal/SourceDebugExtension") ||
                                    it.desc.startsWith("Lkotlin/Metadata") ||
                                    it.desc.startsWith("Lkotlin/coroutines/jvm/internal/DebugMetadata")
                                ) {
                                    remove(it)
                                    add()
                                }
                            }
                        }
                        classNode.visibleAnnotations?.removeCheck()
                        classNode.invisibleAnnotations?.removeCheck()
                    }
            }.get()
            Logger.info("    Removed $annotationCount kotlin debug annotations")
        }
    }

    private val intrinsicsRemoval = listOf(
        "checkExpressionValueIsNotNull",
        "checkNotNullExpressionValue",
        "checkReturnedValueIsNotNull",
        "checkFieldIsNotNull",
        "checkParameterIsNotNull",
        "checkNotNullParameter"
    )

    private val intrinsicsRemoveMethods = mapOf(
        "checkExpressionValueIsNotNull(Ljava/lang/Object;Ljava/lang/String;)V" to 1,
        "checkNotNullExpressionValue(Ljava/lang/Object;Ljava/lang/String;)V" to 1,
        "checkReturnedValueIsNotNull(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V" to 2,
        "checkReturnedValueIsNotNull(Ljava/lang/Object;Ljava/lang/String;)V" to 1,
        "checkFieldIsNotNull(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V" to 2,
        "checkFieldIsNotNull(Ljava/lang/Object;Ljava/lang/String;)V" to 1,
        "checkParameterIsNotNull(Ljava/lang/Object;Ljava/lang/String;)V" to 1,
        "checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V" to 1
    )

    private val intrinsicsReplaceMethods = listOf(
        "checkNotNull(Ljava/lang/Object;Ljava/lang/String;)V",
        "throwNpe(Ljava/lang/String;)V",
        "throwJavaNpe(Ljava/lang/String;)V",
        "throwUninitializedProperty(Ljava/lang/String;)V",
        "throwUninitializedPropertyAccessException(Ljava/lang/String;)V",
        "throwAssert(Ljava/lang/String;)V",
        "throwIllegalArgument(Ljava/lang/String;)V",
        "throwIllegalState(Ljava/lang/String;)V",
        "throwUndefinedForReified(Ljava/lang/String;)V",
    )

}