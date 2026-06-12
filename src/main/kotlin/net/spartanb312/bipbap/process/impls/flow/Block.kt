package net.spartanb312.bipbap.process.impls.flow

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame

class Block(
    val id: Int,
    val start: Int,
    val end: Int
) {
    val successors = mutableListOf<Edge>()
    val predecessors = mutableListOf<Edge>()

    fun entryFrame(frames: Array<Frame<BasicValue>?>): Frame<BasicValue>? {
        for (index in start..end) {
            frames.getOrNull(index)?.let { return it }
        }
        return null
    }

    fun terminalIndex(instructions: Array<AbstractInsnNode>): Int? {
        for (index in end downTo start) {
            if (instructions[index].opcode >= 0) return index
        }
        return null
    }
}