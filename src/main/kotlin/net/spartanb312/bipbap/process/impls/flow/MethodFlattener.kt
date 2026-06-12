package net.spartanb312.bipbap.process.impls.flow

import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.utils.toInsnNode
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import kotlin.random.Random

class MethodFlattener(
    private val context: WorkContext,
    private val classNode: ClassNode,
    private val minBlocks: Int
) {

    fun flatten(methodNode: MethodNode): Boolean {
        val graph = ControlFlowGraphBuilder(context, classNode).build(methodNode) ?: return false
        if (!graph.isSafeForFlattening(minBlocks)) return false

        val backup = MethodBackup(methodNode)
        applyFlattening(methodNode, graph)

        if (!validate(methodNode)) {
            backup.restore(methodNode)
            return false
        }

        methodNode.localVariables?.clear()
        methodNode.visibleLocalVariableAnnotations?.clear()
        methodNode.invisibleLocalVariableAnnotations?.clear()
        return true
    }

    private fun applyFlattening(methodNode: MethodNode, graph: ControlFlowGraph) {
        val stateLocal = methodNode.maxLocals
        val states = createStateKeys(graph.blocks)
        val caseLabels = graph.blocks.associateWith { LabelNode() }
        val dispatcher = LabelNode()
        val defaultCase = LabelNode()
        val flattened = InsnList()

        flattened.setState(states.getValue(graph.entry), stateLocal)
        flattened.add(JumpInsnNode(Opcodes.GOTO, dispatcher))

        graph.blocks.forEach { block ->
            flattened.add(caseLabels.getValue(block))
            appendBlock(flattened, graph, block, states, stateLocal, dispatcher)
        }

        val orderedCases = graph.blocks
            .map { states.getValue(it) to caseLabels.getValue(it) }
            .sortedBy { it.first }
        flattened.add(dispatcher)
        flattened.add(VarInsnNode(Opcodes.ILOAD, stateLocal))
        flattened.add(
            LookupSwitchInsnNode(
                defaultCase,
                orderedCases.map { it.first }.toIntArray(),
                orderedCases.map { it.second }.toTypedArray()
            )
        )
        flattened.add(defaultCase)
        flattened.add(TypeInsnNode(Opcodes.NEW, "java/lang/IllegalStateException"))
        flattened.add(InsnNode(Opcodes.DUP))
        flattened.add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/IllegalStateException", "<init>", "()V", false))
        flattened.add(InsnNode(Opcodes.ATHROW))

        methodNode.instructions.clear()
        methodNode.instructions.add(flattened)
        methodNode.maxLocals = stateLocal + 1
    }

    private fun appendBlock(
        output: InsnList,
        graph: ControlFlowGraph,
        block: Block,
        states: Map<Block, Int>,
        stateLocal: Int,
        dispatcher: LabelNode
    ) {
        val terminalIndex = block.terminalIndex(graph.instructions)
        val terminal = terminalIndex?.let { graph.instructions[it] }

        for (index in block.start..block.end) {
            val insn = graph.instructions[index]
            if (insn is FrameNode) continue
            if (index == terminalIndex && terminal.isReplacedTerminal()) continue
            output.add(insn)
        }

        when (terminal) {
            is JumpInsnNode -> appendJumpReplacement(output, graph, block, terminal, states, stateLocal, dispatcher)
            is TableSwitchInsnNode -> appendTableSwitchReplacement(output, graph, terminal, states, stateLocal, dispatcher)
            is LookupSwitchInsnNode -> appendLookupSwitchReplacement(output, graph, terminal, states, stateLocal, dispatcher)
            else -> {
                if (terminal == null || !terminal.opcode.isReturnOrThrow()) {
                    val successor = block.successors.firstOrNull { it.kind == EdgeKind.FALL_THROUGH }?.target
                    if (successor != null) output.setStateAndJump(states.getValue(successor), stateLocal, dispatcher)
                }
            }
        }
    }

    private fun appendJumpReplacement(
        output: InsnList,
        graph: ControlFlowGraph,
        block: Block,
        jumpInsnNode: JumpInsnNode,
        states: Map<Block, Int>,
        stateLocal: Int,
        dispatcher: LabelNode
    ) {
        val target = graph.blockFor(jumpInsnNode.label) ?: return
        if (jumpInsnNode.opcode == Opcodes.GOTO) {
            output.setStateAndJump(states.getValue(target), stateLocal, dispatcher)
            return
        }

        val trueBranch = LabelNode()
        val falseBranch = block.successors.firstOrNull { it.kind == EdgeKind.FALSE_BRANCH }?.target ?: return
        output.add(JumpInsnNode(jumpInsnNode.opcode, trueBranch))
        output.setStateAndJump(states.getValue(falseBranch), stateLocal, dispatcher)
        output.add(trueBranch)
        output.setStateAndJump(states.getValue(target), stateLocal, dispatcher)
    }

    private fun appendTableSwitchReplacement(
        output: InsnList,
        graph: ControlFlowGraph,
        switchInsnNode: TableSwitchInsnNode,
        states: Map<Block, Int>,
        stateLocal: Int,
        dispatcher: LabelNode
    ) {
        val defaultCase = LabelNode()
        val labels = switchInsnNode.labels.map { LabelNode() }
        output.add(TableSwitchInsnNode(switchInsnNode.min, switchInsnNode.max, defaultCase, *labels.toTypedArray()))

        labels.forEachIndexed { index, label ->
            val target = graph.blockFor(switchInsnNode.labels[index]) ?: return@forEachIndexed
            output.add(label)
            output.setStateAndJump(states.getValue(target), stateLocal, dispatcher)
        }

        val defaultTarget = graph.blockFor(switchInsnNode.dflt) ?: return
        output.add(defaultCase)
        output.setStateAndJump(states.getValue(defaultTarget), stateLocal, dispatcher)
    }

    private fun appendLookupSwitchReplacement(
        output: InsnList,
        graph: ControlFlowGraph,
        switchInsnNode: LookupSwitchInsnNode,
        states: Map<Block, Int>,
        stateLocal: Int,
        dispatcher: LabelNode
    ) {
        val defaultCase = LabelNode()
        val labels = switchInsnNode.labels.map { LabelNode() }
        output.add(LookupSwitchInsnNode(defaultCase, switchInsnNode.keys.toIntArray(), labels.toTypedArray()))

        labels.forEachIndexed { index, label ->
            val target = graph.blockFor(switchInsnNode.labels[index]) ?: return@forEachIndexed
            output.add(label)
            output.setStateAndJump(states.getValue(target), stateLocal, dispatcher)
        }

        val defaultTarget = graph.blockFor(switchInsnNode.dflt) ?: return
        output.add(defaultCase)
        output.setStateAndJump(states.getValue(defaultTarget), stateLocal, dispatcher)
    }

    private fun InsnList.setStateAndJump(state: Int, stateLocal: Int, dispatcher: LabelNode) {
        setState(state, stateLocal)
        add(JumpInsnNode(Opcodes.GOTO, dispatcher))
    }

    private fun InsnList.setState(state: Int, stateLocal: Int) {
        addStateExpression(state)
        add(VarInsnNode(Opcodes.ISTORE, stateLocal))
    }

    private fun InsnList.addStateExpression(state: Int) {
        when (Random.nextInt(5)) {
            0 -> {
                val mask = Random.nextInt()
                add((state xor mask).toInsnNode())
                add(mask.toInsnNode())
                add(InsnNode(Opcodes.IXOR))
            }

            1 -> {
                val delta = Random.nextInt()
                add((state - delta).toInsnNode())
                add(delta.toInsnNode())
                add(InsnNode(Opcodes.IADD))
            }

            2 -> {
                val delta = Random.nextInt()
                add((state + delta).toInsnNode())
                add(delta.toInsnNode())
                add(InsnNode(Opcodes.ISUB))
            }

            3 -> {
                val mask = Random.nextInt()
                val delta = Random.nextInt()
                add(((state xor mask) - delta).toInsnNode())
                add(delta.toInsnNode())
                add(InsnNode(Opcodes.IADD))
                add(mask.toInsnNode())
                add(InsnNode(Opcodes.IXOR))
            }

            else -> {
                val mask = Random.nextInt()
                val delta = Random.nextInt()
                add(((state + delta) xor mask).toInsnNode())
                add(mask.toInsnNode())
                add(InsnNode(Opcodes.IXOR))
                add(delta.toInsnNode())
                add(InsnNode(Opcodes.ISUB))
            }
        }
    }

    private fun createStateKeys(blocks: List<Block>): Map<Block, Int> {
        val used = mutableSetOf<Int>()
        return blocks.associateWith {
            var key: Int
            do {
                key = Random.nextInt()
            } while (!used.add(key))
            key
        }
    }

    private fun validate(methodNode: MethodNode): Boolean {
        return try {
            Analyzer(WorkspaceSimpleVerifier(context, classNode)).analyze(classNode.name, methodNode)
            true
        } catch (_: Throwable) {
            false
        }
    }

}
