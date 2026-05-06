package net.spartanb312.bipbap.process.resource

import net.spartanb312.bipbap.utils.isInterface
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.ClassWriter

class ClassDumper(
    private val context: WorkContext,
    private val hierarchy: Hierarchy,
    useComputeMax: Boolean = false
) : ClassWriter(if (useComputeMax) COMPUTE_MAXS else COMPUTE_FRAMES) {

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
                //else {
                //    // search lca
                //    var info1 = hierarchy.findClassInfo(type1)
                //    val info2 = hierarchy.findClassInfo(type2)
                //    if (info1 != null && info2 != null) {
                //        do {
                //            val info1Super = info1?.superName
                //            if (info1Super != null) {
                //                val info1SuperC = hierarchy.findClassInfo(info1Super)
                //                if (info1SuperC != null) info1 = info1SuperC
                //                else break
                //            } else break
                //        } while (!hierarchy.isSubType(info2, info1))
                //        if (info1 != null) return info1.name
                //    }
                //}
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