package net.spartanb312.bipbap.process

import net.spartanb312.bipbap.process.impls.*
import net.spartanb312.bipbap.process.impls.ControlflowFlattening

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
object Transformers : Collection<Transformer> by mutableListOf(
    HWIDAuthenticator,
    CodeOptimizer,
    ControlflowFlattening,
    ConstantEncryptor,
    MembersRenamer,
    ClassRenamer,
    InvokeDynamics,
    Miscellaneous
) {

    fun resetTransformers() = forEach { it.reset() }

}