package net.spartanb312.bipbap.process.impls.encrypt

import net.spartanb312.bipbap.utils.toInsnNode
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import kotlin.random.Random

object NumberEncryptor {

    fun <T : Number> encrypt(value: T): InsnList {
        return when (value) {
            is Int -> encrypt(value.toInt())
            is Long -> encrypt(value.toLong())
            is Float -> encrypt(value.toFloat())
            is Double -> encrypt(value.toDouble())
            else -> throw Exception("Not yet implemented")
        }
    }

    private fun encrypt(value: Float): InsnList {
        val intBits = value.asInt()
        val key = Random.nextInt()
        val encryptedIntBits = intBits xor key
        return InsnList().apply {
            add(encryptedIntBits.toInsnNode())
            add(key.toInsnNode())
            add(InsnNode(Opcodes.IXOR))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F"))
        }
    }

    private fun encrypt(value: Double): InsnList {
        val longBits = value.asLong()
        val key = Random.nextLong()
        val encryptedLongBits = longBits xor key
        return InsnList().apply {
            add(encryptedLongBits.toInsnNode())
            add(key.toInsnNode())
            add(InsnNode(Opcodes.LXOR))
            add(MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D"))
        }
    }

    private fun encrypt(value: Int): InsnList {
        val random = Random.nextInt(Int.MAX_VALUE)
        val negative = (if (Random.nextBoolean()) random else -random) + value
        val obfuscated = value xor negative
        return InsnList().apply {
            if (Random.nextBoolean()) {
                add(negative.toInsnNode())
                add(InsnNode(Opcodes.I2L))
                add(obfuscated.toInsnNode())
                add(InsnNode(Opcodes.I2L))
                add(InsnNode(Opcodes.LXOR))
                add(InsnNode(Opcodes.L2I))
            } else {
                add(negative.toLong().toInsnNode())
                add(InsnNode(Opcodes.L2I))
                add(obfuscated.toInsnNode())
                add(InsnNode(Opcodes.IXOR))
            }
        }
    }

    private fun encrypt(value: Long): InsnList {
        val random = Random.nextLong()
        val obfuscated = value xor random
        return InsnList().apply {
            add(obfuscated.toInsnNode())
            add(random.toInsnNode())
            add(InsnNode(Opcodes.LXOR))
        }
    }

    private fun Double.asLong(): Long = java.lang.Double.doubleToRawLongBits(this)

    private fun Float.asInt(): Int = java.lang.Float.floatToRawIntBits(this)

}