package net.spartanb312.bipbap.utils

import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import kotlin.random.Random

fun String.notInList(list: List<String>): Boolean =
    !inList(list)

fun String.inList(list: List<String>): Boolean {
    return list.any { this.startsWith(it) }
}

fun getRandomString(length: Int): String {
    val charSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    var str = ""
    repeat(length) {
        str += charSet[(charSet.length * Random.nextInt(0, 100) / 100f).toInt()]
    }
    return str
}

val massiveString = buildString { repeat(Short.MAX_VALUE.toInt() - 1) { append(" ") } }

val (Collection<AnnotationNode>)?.checkMixin
    get() = this?.none { it.desc.startsWith("Lorg/spongepowered/asm/mixin") } ?: true
val ClassNode.checkMixin get() = visibleAnnotations.checkMixin && invisibleAnnotations.checkMixin
val MethodNode.checkMixin get() = visibleAnnotations.checkMixin && invisibleAnnotations.checkMixin
val FieldNode.checkMixin get() = visibleAnnotations.checkMixin && invisibleAnnotations.checkMixin