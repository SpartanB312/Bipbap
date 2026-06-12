package net.spartanb312.bipbap.process.resource

import net.spartanb312.bipbap.utils.isInterface
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.ClassWriter

class ClassDumper(
    private val context: WorkContext,
    private val hierarchy: Hierarchy,
    computeFrames: Boolean = true
) : ClassWriter(if (computeFrames) COMPUTE_FRAMES else COMPUTE_MAXS) {

    override fun getCommonSuperClass(type1: String, type2: String): String {
        return when {
            type1 == "java/lang/Object" -> type1
            type2 == "java/lang/Object" -> type2
            hierarchy.isSubType(type1, type2) -> type2
            hierarchy.isSubType(type2, type1) -> type1

            else -> with(hierarchy) {
                val clazz1 = context.getClassNode(type1)
                val clazz2 = context.getClassNode(type2)
                if (clazz1?.isInterface == true || clazz2?.isInterface == true) return "java/lang/Object"
                hierarchy.findLeastCommonAncestor(type1, type2)?.let { return it }
                // fallback
                try {
                    super.getCommonSuperClass(type1, type2)
                } catch (_: Exception) {
                    try {
                        Class.forName(type1)
                    } catch (_: Exception) {
                        if (clazz1 == null) {
                            Logger.error("Missing dependency $type1")
                            throw Exception("Can't find common super class due to missing $type1")
                        }
                    }
                    try {
                        Class.forName(type2)
                    } catch (_: Exception) {
                        if (clazz2 == null) {
                            Logger.error("Missing dependency $type2")
                            throw Exception("Can't find common super class due to missing $type2")
                        }
                    }
                    "java/lang/Object"
                }
            }
        }
    }

}

private fun Hierarchy.findLeastCommonAncestor(type1: String, type2: String): String? {
    var current = findOrBuildClassInfo(type1) ?: return null
    if (findOrBuildClassInfo(type2) == null) return null
    val visited = mutableSetOf<String>()
    while (visited.add(current.name)) {
        if (isClassSubType(type2, current.name)) return current.name
        val superName = current.superName ?: return null
        current = findOrBuildClassInfo(superName) ?: return null
    }
    return null
}

private fun Hierarchy.isClassSubType(child: String, father: String): Boolean {
    if (child == father || father == "java/lang/Object") return true
    var current = findOrBuildClassInfo(child) ?: return false
    val visited = mutableSetOf<String>()
    while (visited.add(current.name)) {
        val superName = current.superName ?: return false
        if (superName == father) return true
        current = findOrBuildClassInfo(superName) ?: return false
    }
    return false
}

private fun Hierarchy.findOrBuildClassInfo(name: String): Hierarchy.ClassInfo? {
    return try {
        findClassInfo(name) ?: getClassInfo(name)
    } catch (_: Throwable) {
        null
    }
}
