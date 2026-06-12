package net.spartanb312.bipbap.process.impls.flow

import groovyjarjarasm.asm.Opcodes
import net.spartanb312.bipbap.process.resource.WorkContext
import net.spartanb312.bipbap.utils.isInterface
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.analysis.SimpleVerifier

class WorkspaceSimpleVerifier(
    private val context: WorkContext,
    private val classNode: ClassNode
) : SimpleVerifier(
    Opcodes.ASM9,
    Type.getObjectType(classNode.name),
    classNode.superName?.let { Type.getObjectType(it) },
    classNode.interfaces.map { Type.getObjectType(it) },
    classNode.isInterface
) {

    override fun isInterface(type: Type): Boolean {
        if (type.sort != Type.OBJECT) return false
        return context.getClassNode(type.internalName)?.isInterface
            ?: runtimeClass(type)?.isInterface
            ?: false
    }

    override fun getSuperClass(type: Type): Type? {
        if (type.sort == Type.ARRAY) return Type.getObjectType("java/lang/Object")
        if (type.sort != Type.OBJECT || type.internalName == "java/lang/Object") return null
        val node = context.getClassNode(type.internalName)
        if (node?.superName != null) return Type.getObjectType(node.superName)
        return runtimeClass(type)?.superclass?.let { Type.getType(it) }
    }

    override fun isAssignableFrom(type1: Type, type2: Type): Boolean {
        if (type1 == type2) return true
        if (type2.isNullType()) return type1.sort == Type.OBJECT || type1.sort == Type.ARRAY
        if (type1.sort == Type.OBJECT && type1.internalName == "java/lang/Object") {
            return type2.sort == Type.OBJECT || type2.sort == Type.ARRAY
        }
        if (type1.sort == Type.ARRAY || type2.sort == Type.ARRAY) {
            return isArrayAssignableFrom(type1, type2)
        }
        if (type1.sort != Type.OBJECT || type2.sort != Type.OBJECT) return false
        if (isInterface(type1) && implementsInterface(type2, type1, linkedSetOf())) return true

        var cursor: Type? = type2
        while (cursor != null) {
            if (cursor == type1) return true
            if (isInterface(type1) && implementsInterface(cursor, type1, linkedSetOf())) return true
            cursor = getSuperClass(cursor)
        }
        return false
    }

    private fun isArrayAssignableFrom(type1: Type, type2: Type): Boolean {
        if (type1 == type2) return true
        if (type2.sort != Type.ARRAY) return false
        if (type1.sort == Type.OBJECT) {
            return type1.internalName == "java/lang/Cloneable" || type1.internalName == "java/io/Serializable"
        }
        if (type1.sort != Type.ARRAY || type1.dimensions != type2.dimensions) return false

        val element1 = type1.elementType
        val element2 = type2.elementType
        val primitiveElement = element1.sort != Type.OBJECT && element1.sort != Type.ARRAY
                || element2.sort != Type.OBJECT && element2.sort != Type.ARRAY
        return if (primitiveElement) element1 == element2 else isAssignableFrom(element1, element2)
    }

    private fun implementsInterface(type: Type, iface: Type, visited: MutableSet<String>): Boolean {
        if (type.sort == Type.ARRAY) {
            return iface.internalName == "java/lang/Cloneable" || iface.internalName == "java/io/Serializable"
        }
        if (type.sort != Type.OBJECT || !visited.add(type.internalName)) return false
        val node = context.getClassNode(type.internalName) ?: return runtimeClass(type)?.interfaces?.any {
            isAssignableFrom(iface, Type.getType(it))
        } == true

        if (node.interfaces.any { it == iface.internalName }) return true
        if (node.interfaces.any { implementsInterface(Type.getObjectType(it), iface, visited) }) return true
        return node.superName?.let { implementsInterface(Type.getObjectType(it), iface, visited) } ?: false
    }

    private fun runtimeClass(type: Type): Class<*>? {
        return try {
            Class.forName(type.className, false, javaClass.classLoader)
        } catch (_: Throwable) {
            null
        }
    }

}