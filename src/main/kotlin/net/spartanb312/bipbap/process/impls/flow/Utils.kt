package net.spartanb312.bipbap.process.impls.flow

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame

fun Type.isNullType(): Boolean {
    return sort == Type.OBJECT && internalName == "null"
}

fun AbstractInsnNode?.isReplacedTerminal(): Boolean {
    return this is JumpInsnNode || this is TableSwitchInsnNode || this is LookupSwitchInsnNode
}

fun Frame<BasicValue>.localSignature(): List<String> {
    return (0 until locals).map { index ->
        val value = getLocal(index)
        val type = value?.type
        when {
            value == null || value == BasicValue.UNINITIALIZED_VALUE || type == null -> "T"
            else -> type.descriptor
        }
    }
}

fun Int.isReturnOrThrow(): Boolean {
    return this in Opcodes.IRETURN..Opcodes.RETURN || this == Opcodes.ATHROW
}
