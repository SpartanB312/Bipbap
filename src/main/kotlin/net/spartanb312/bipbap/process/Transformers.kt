package net.spartanb312.bipbap.process

import net.spartanb312.bipbap.process.impls.*

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
object Transformers : Collection<Transformer> by mutableListOf(
    HWIDAuthenticator,
    CodeOptimizer,
    ConstantEncryptor,
    MembersRenamer,
    InvokeDynamics,
    Miscellaneous
) {

    fun resetTransformers() = forEach { it.reset() }

}