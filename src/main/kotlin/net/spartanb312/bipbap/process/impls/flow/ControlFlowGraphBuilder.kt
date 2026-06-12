package net.spartanb312.bipbap.process.impls.flow

import groovyjarjarasm.asm.Opcodes
import net.spartanb312.bipbap.process.resource.WorkContext
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import java.util.TreeSet

class ControlFlowGraphBuilder(
    private val context: WorkContext,
    private val classNode: ClassNode
) {

    fun build(methodNode: MethodNode): ControlFlowGraph? {
        val analyzer = FlatteningAnalyzer(WorkspaceSimpleVerifier(context, classNode))
        val frames = try {
            analyzer.analyze(classNode.name, methodNode)
        } catch (_: Throwable) {
            return null
        }
        if (analyzer.exceptionEdges.isNotEmpty()) return null

        val instructions = methodNode.instructions.toArray()
        if (instructions.isEmpty()) return null

        val labelIndices = instructions
            .mapIndexedNotNull { index, insn -> (insn as? LabelNode)?.let { it to index } }
            .toMap()
        val leaders = collectLeaders(instructions, labelIndices) ?: return null
        val allBlocks = leaders.mapIndexed { index, start ->
            Block(index, start, leaders.getOrNull(index + 1)?.minus(1) ?: instructions.lastIndex)
        }
        val blockByIndex = arrayOfNulls<Block>(instructions.size)
        allBlocks.forEach { block ->
            for (index in block.start..block.end) blockByIndex[index] = block
        }

        val entry = blockByIndex[0] ?: return null
        if (entry.entryFrame(frames) == null) return null

        allBlocks.forEach {
            it.successors.clear()
            it.predecessors.clear()
        }

        val reachable = linkedSetOf<Block>()
        val queue = ArrayDeque<Block>()
        queue.add(entry)
        while (queue.isNotEmpty()) {
            val block = queue.removeFirst()
            if (block in reachable) continue
            if (block.entryFrame(frames) == null) return null
            reachable.add(block)

            val successors = resolveSuccessors(block, instructions, labelIndices, blockByIndex) ?: return null
            successors.forEach { edge ->
                if (edge.target.entryFrame(frames) == null) return null
                block.successors.add(edge)
                edge.target.predecessors.add(edge)
                queue.add(edge.target)
            }
        }

        val blocks = allBlocks.filter { it in reachable }
        return ControlFlowGraph(instructions, frames, blocks, entry, blockByIndex, labelIndices)
    }

    private fun collectLeaders(
        instructions: Array<AbstractInsnNode>,
        labelIndices: Map<LabelNode, Int>
    ): List<Int>? {
        val leaders = TreeSet<Int>()
        leaders.add(0)

        fun addLabel(labelNode: LabelNode): Boolean {
            val index = labelIndices[labelNode] ?: return false
            leaders.add(index)
            return true
        }

        fun addNext(index: Int) {
            if (index + 1 < instructions.size) leaders.add(index + 1)
        }

        instructions.forEachIndexed { index, insn ->
            when (insn) {
                is JumpInsnNode -> {
                    if (!addLabel(insn.label)) return null
                    addNext(index)
                }

                is TableSwitchInsnNode -> {
                    if (!addLabel(insn.dflt)) return null
                    insn.labels.forEach { if (!addLabel(it)) return null }
                    addNext(index)
                }

                is LookupSwitchInsnNode -> {
                    if (!addLabel(insn.dflt)) return null
                    insn.labels.forEach { if (!addLabel(it)) return null }
                    addNext(index)
                }

                else -> if (insn.opcode.isReturnOrThrow()) addNext(index)
            }
        }

        return leaders.filter { it in instructions.indices }
    }

    private fun resolveSuccessors(
        block: Block,
        instructions: Array<AbstractInsnNode>,
        labelIndices: Map<LabelNode, Int>,
        blockByIndex: Array<Block?>
    ): List<Edge>? {
        fun blockAt(index: Int): Block? = blockByIndex.getOrNull(index)

        fun blockFor(labelNode: LabelNode): Block? {
            return labelIndices[labelNode]?.let { blockAt(it) }
        }

        fun fallThrough(index: Int): Block? = blockAt(index + 1)

        val terminalIndex = block.terminalIndex(instructions)
        val terminal = terminalIndex?.let { instructions[it] }
        return when (terminal) {
            is JumpInsnNode -> {
                val target = blockFor(terminal.label) ?: return null
                if (terminal.opcode == Opcodes.GOTO) {
                    listOf(Edge(block, target, EdgeKind.JUMP))
                } else {
                    val falseTarget = fallThrough(terminalIndex) ?: return null
                    listOf(
                        Edge(block, target, EdgeKind.TRUE_BRANCH),
                        Edge(block, falseTarget, EdgeKind.FALSE_BRANCH)
                    )
                }
            }

            is TableSwitchInsnNode -> {
                val edges = mutableListOf<Edge>()
                val defaultTarget = blockFor(terminal.dflt) ?: return null
                edges.add(Edge(block, defaultTarget, EdgeKind.SWITCH_DEFAULT))
                terminal.labels.forEachIndexed { index, labelNode ->
                    val target = blockFor(labelNode) ?: return null
                    edges.add(Edge(block, target, EdgeKind.SWITCH_CASE, terminal.min + index))
                }
                edges
            }

            is LookupSwitchInsnNode -> {
                val edges = mutableListOf<Edge>()
                val defaultTarget = blockFor(terminal.dflt) ?: return null
                edges.add(Edge(block, defaultTarget, EdgeKind.SWITCH_DEFAULT))
                terminal.labels.forEachIndexed { index, labelNode ->
                    val target = blockFor(labelNode) ?: return null
                    edges.add(Edge(block, target, EdgeKind.SWITCH_CASE, terminal.keys[index]))
                }
                edges
            }

            else -> {
                if (terminal != null && terminal.opcode.isReturnOrThrow()) {
                    emptyList()
                } else {
                    fallThrough(block.end)?.let { listOf(Edge(block, it, EdgeKind.FALL_THROUGH)) } ?: emptyList()
                }
            }
        }
    }

}