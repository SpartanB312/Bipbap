package net.spartanb312.bipbap.process.impls

import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.impls.flow.MethodFlattener
import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.utils.*
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

object ControlflowFlattening : Transformer("ControlflowFlattening") {

    private val minBlocks by setting("MinBlocks", 3)
    private val exclusion by setting("Exclusion", listOf())

    override fun WorkContext.transform() {
        Logger.info(" - Applying control flow flattening...")

        val context = this
        val filtered = nonExcluded.filter {
            !it.isInterface && getPrevName(it.name).notInList(exclusion) && it.checkMixin
        }
        val flattened = count {
            parallelForEach(filtered) { classNode ->
                val flattener = MethodFlattener(context, classNode, minBlocks)
                classNode.methods.asSequence()
                    .filter { it.isFlatteningCandidate() }
                    .forEach { methodNode ->
                        if (flattener.flatten(methodNode)) add()
                    }
            }
        }.get()

        if (flattened > 0) computeFrames = true
        Logger.info("    Flattened $flattened methods")
    }

    private fun MethodNode.isFlatteningCandidate(): Boolean {
        if (isAbstract || isNative || isInitializer || !checkMixin) return false
        if (instructions == null || instructions.size() == 0) return false
        if (tryCatchBlocks?.isNotEmpty() == true) return false
        return instructions.none {
            it.opcode == Opcodes.JSR
                    || it.opcode == Opcodes.RET
                    || it.opcode == Opcodes.MONITORENTER
                    || it.opcode == Opcodes.MONITOREXIT
        }
    }

}