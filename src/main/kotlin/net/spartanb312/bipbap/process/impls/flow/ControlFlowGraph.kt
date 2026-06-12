package net.spartanb312.bipbap.process.impls.flow

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame

class ControlFlowGraph(
    val instructions: Array<AbstractInsnNode>,
    val frames: Array<Frame<BasicValue>?>,
    val blocks: List<Block>,
    val entry: Block,
    private val blockByIndex: Array<Block?>,
    private val labelIndices: Map<LabelNode, Int>
) {

    fun blockFor(labelNode: LabelNode): Block? {
        return labelIndices[labelNode]?.let { blockByIndex.getOrNull(it) }
    }

    fun isSafeForFlattening(minBlocks: Int): Boolean {
        if (blocks.size < minBlocks) return false
        val blockSet = blocks.toSet()
        val locals = entry.entryFrame(frames)?.localSignature() ?: return false
        return blocks.all { block ->
            val frame = block.entryFrame(frames) ?: return@all false
            frame.stackSize == 0
                    && frame.localSignature() == locals
                    && block.successors.all { it.target in blockSet }
        }
    }

}