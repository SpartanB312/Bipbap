package net.spartanb312.bipbap.process.impls.flow

import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Interpreter

class FlatteningAnalyzer(interpreter: Interpreter<BasicValue>) : Analyzer<BasicValue>(interpreter) {

    val normalEdges = mutableMapOf<Int, MutableSet<Int>>()
    val exceptionEdges = mutableMapOf<Int, MutableSet<Int>>()

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        normalEdges.getOrPut(insnIndex) { linkedSetOf() }.add(successorIndex)
        super.newControlFlowEdge(insnIndex, successorIndex)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        exceptionEdges.getOrPut(insnIndex) { linkedSetOf() }.add(successorIndex)
        return super.newControlFlowExceptionEdge(insnIndex, successorIndex)
    }

}
