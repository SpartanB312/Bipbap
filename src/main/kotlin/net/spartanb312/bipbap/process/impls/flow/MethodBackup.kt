package net.spartanb312.bipbap.process.impls.flow

import org.objectweb.asm.tree.MethodNode

class MethodBackup(methodNode: MethodNode) {
    private val instructions = methodNode.instructions.toArray()
    private val localVariables = methodNode.localVariables?.toList()
    private val visibleLocalVariableAnnotations = methodNode.visibleLocalVariableAnnotations?.toList()
    private val invisibleLocalVariableAnnotations = methodNode.invisibleLocalVariableAnnotations?.toList()
    private val maxLocals = methodNode.maxLocals
    private val maxStack = methodNode.maxStack

    fun restore(methodNode: MethodNode) {
        methodNode.instructions.clear()
        instructions.forEach { methodNode.instructions.add(it) }
        methodNode.localVariables = localVariables?.toMutableList()
        methodNode.visibleLocalVariableAnnotations = visibleLocalVariableAnnotations?.toMutableList()
        methodNode.invisibleLocalVariableAnnotations = invisibleLocalVariableAnnotations?.toMutableList()
        methodNode.maxLocals = maxLocals
        methodNode.maxStack = maxStack
    }
}
