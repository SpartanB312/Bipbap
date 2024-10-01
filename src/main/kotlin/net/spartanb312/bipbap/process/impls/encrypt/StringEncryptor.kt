package net.spartanb312.bipbap.process.impls.encrypt

import net.spartanb312.bipbap.utils.toInsnNode
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodNode

object StringEncryptor {

    fun createDecryptMethod(methodName: String, key: Int): MethodNode = MethodNode(
        Opcodes.ASM9,
        Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_BRIDGE,
        methodName,
        "(Ljava/lang/String;)Ljava/lang/String;",
        null,
        null
    ).apply {
        val label1 = Label()
        val label2 = Label()

        visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        visitInsn(Opcodes.DUP)
        visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        visitVarInsn(Opcodes.ASTORE, 1)
        visitInsn(Opcodes.ICONST_0)
        visitVarInsn(Opcodes.ISTORE, 2)
        visitJumpInsn(Opcodes.GOTO, label2)

        visitLabel(label1)
        visitFrame(Opcodes.F_APPEND, 2, arrayOf<Any>("java/lang/StringBuilder", Opcodes.INTEGER), 0, null)
        visitVarInsn(Opcodes.ALOAD, 1)
        visitVarInsn(Opcodes.ALOAD, 0)
        visitVarInsn(Opcodes.ILOAD, 2)
        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C", false)
        instructions.add(key.toInsnNode())
        visitInsn(Opcodes.IXOR)
        visitInsn(Opcodes.I2C)
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(C)Ljava/lang/StringBuilder;",
            false
        )
        visitInsn(Opcodes.POP)
        visitIincInsn(2, 1)

        visitLabel(label2)
        visitFrame(Opcodes.F_SAME, 0, null, 0, null)
        visitVarInsn(Opcodes.ILOAD, 2)
        visitVarInsn(Opcodes.ALOAD, 0)
        visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false)
        visitJumpInsn(Opcodes.IF_ICMPLT, label1)
        visitVarInsn(Opcodes.ALOAD, 1)
        visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false
        )

        visitInsn(Opcodes.ARETURN)
        visitMaxs(3, 3)
    }

    fun encrypt(string: String, xor: Int): String {
        val stringBuilder = StringBuilder()
        for (element in string) {
            stringBuilder.append((element.code xor xor).toChar())
        }
        return stringBuilder.toString()
    }

}