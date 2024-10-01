package net.spartanb312.bipbap.utils

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier

// Class
inline val ClassNode.isPublic get() = Modifier.isPublic(access)

inline val ClassNode.isPrivate get() = Modifier.isPrivate(access)

inline val ClassNode.isStatic get() = Modifier.isStatic(access)

inline val ClassNode.isAbstract get() = Modifier.isAbstract(access)

inline val ClassNode.isInterface get() = Modifier.isInterface(access)

inline val ClassNode.isAnnotation get() = access and Opcodes.ACC_ANNOTATION != 0

inline val ClassNode.isEnum get() = access and Opcodes.ACC_ENUM != 0

val ClassNode.hasAnnotations: Boolean
    get() {
        return (visibleAnnotations != null && visibleAnnotations.isNotEmpty()) || (invisibleAnnotations != null && invisibleAnnotations.isNotEmpty())
    }

// Field
inline val FieldNode.isPublic get() = Modifier.isPublic(access)

inline val FieldNode.isPrivate get() = Modifier.isPrivate(access)

inline val FieldNode.isProtected get() = Modifier.isProtected(access)

inline val FieldNode.isStatic get() = Modifier.isStatic(access)

inline val FieldNode.isAbstract get() = Modifier.isAbstract(access)

val FieldNode.hasAnnotations: Boolean
    get() {
        return (visibleAnnotations != null && visibleAnnotations.isNotEmpty()) || (invisibleAnnotations != null && invisibleAnnotations.isNotEmpty())
    }

// Method
inline val MethodNode.isPublic get() = Modifier.isPublic(access)

inline val MethodNode.isPrivate get() = Modifier.isPrivate(access)

inline val MethodNode.isProtected get() = Modifier.isProtected(access)

inline val MethodNode.isStatic get() = Modifier.isStatic(access)

inline val MethodNode.isNative get() = Modifier.isNative(access)

inline val MethodNode.isAbstract get() = Modifier.isAbstract(access)

inline val MethodNode.isMainMethod get() = name == "main" && desc == "([Ljava/lang/String;)V"

inline val MethodNode.isInitializer get() = name == "<init>" || name == "<clinit>"

val MethodNode.hasAnnotations: Boolean
    get() {
        return (visibleAnnotations != null && visibleAnnotations.isNotEmpty()) || (invisibleAnnotations != null && invisibleAnnotations.isNotEmpty())
    }

// Insn
fun Int.toInsnNode(): AbstractInsnNode =
    when (this) {
        in -1..5 -> InsnNode(this + 0x3)
        in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(Opcodes.BIPUSH, this)
        in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(Opcodes.SIPUSH, this)
        else -> LdcInsnNode(this)
    }

fun Long.toInsnNode(): AbstractInsnNode =
    if (this in 0..1) InsnNode((this + 9).toInt())
    else LdcInsnNode(this)

fun Float.toInsnNode(): AbstractInsnNode =
    if (this in 0.0..2.0) InsnNode((this + 11).toInt())
    else LdcInsnNode(this)

fun Double.toInsnNode(): AbstractInsnNode =
    if (this in 0.0..1.0) InsnNode((this + 14).toInt())
    else LdcInsnNode(this)
