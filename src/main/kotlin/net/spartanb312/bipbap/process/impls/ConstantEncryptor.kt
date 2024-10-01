package net.spartanb312.bipbap.process.impls

import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.impls.encrypt.NumberEncryptor
import net.spartanb312.bipbap.process.impls.encrypt.StringEncryptor
import net.spartanb312.bipbap.process.resource.ResourceCache
import net.spartanb312.bipbap.utils.*
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import kotlin.random.Random

object ConstantEncryptor : Transformer("ConstantEncryptor") {

    private val integer by setting("Integer", true)
    private val long by setting("Long", true)
    private val float by setting("Float", true)
    private val double by setting("Double", true)
    private val string by setting("String", true)
    private val exclusion by setting("Exclusion", listOf())

    override fun ResourceCache.transform() {
        Logger.info(" - Encrypting constants...")
        val companions = mutableMapOf<ClassNode, MutableList<ConstRef<*>>>()
        val filtered = nonExcluded.filter { it.name.notInList(exclusion) && it.checkMixin }
        filtered.forEach {
            companions[
                ClassNode().apply {
                    visit(
                        it.version,
                        Opcodes.ACC_PUBLIC,
                        "${it.name}\$Constants",
                        null,
                        "java/lang/Object",
                        null
                    )
                }
            ] = mutableListOf()
        }
        var intCount = 0
        var longCount = 0
        var floatCount = 0
        var doubleCount = 0
        var stringCount = 0
        filtered.forEach { classNode ->
            classNode.methods.forEach { methodNode ->
                if (!methodNode.isAbstract && !methodNode.isNative) {
                    val insnList = InsnList().apply {
                        methodNode.instructions.forEach { insn ->
                            if (insn is LdcInsnNode) {
                                val owner = companions.keys.random()
                                val list = companions[owner]!!
                                val cst = insn.cst
                                when {
                                    cst is Int && integer -> ConstRef.IntRef(cst).let {
                                        list.add(it)
                                        add(
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                owner.name,
                                                it.field.name,
                                                it.field.desc
                                            )
                                        )
                                        intCount++
                                    }

                                    cst is Long && long -> ConstRef.LongRef(cst).let {
                                        list.add(it)
                                        add(
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                owner.name,
                                                it.field.name,
                                                it.field.desc
                                            )
                                        )
                                        longCount++
                                    }

                                    cst is Float && float -> ConstRef.FloatRef(cst).let {
                                        list.add(it)
                                        add(
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                owner.name,
                                                it.field.name,
                                                it.field.desc
                                            )
                                        )
                                        floatCount++
                                    }

                                    cst is Double && double -> ConstRef.DoubleRef(cst).let {
                                        list.add(it)
                                        add(
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                owner.name,
                                                it.field.name,
                                                it.field.desc
                                            )
                                        )
                                        doubleCount++
                                    }

                                    cst is String && string -> ConstRef.StringRef(cst).let {
                                        list.add(it)
                                        add(
                                            FieldInsnNode(
                                                Opcodes.GETSTATIC,
                                                owner.name,
                                                it.field.name,
                                                it.field.desc
                                            )
                                        )
                                        stringCount++
                                    }

                                    else -> add(insn)
                                }
                            } else add(insn)
                        }
                    }
                    methodNode.instructions = insnList
                }
            }
        }
        companions.forEach { (clazz, refList) ->
            if (refList.isNotEmpty()) {
                classes[clazz.name] = clazz
                val clinit = MethodNode(
                    Opcodes.ACC_STATIC,
                    "<clinit>",
                    "()V",
                    null,
                    null
                ).apply {
                    instructions = InsnList().apply {
                        refList.forEach {
                            it.field.value = null
                            clazz.fields.add(it.field)
                            when (it) {
                                is ConstRef.NumberRef -> {
                                    add(NumberEncryptor.encrypt(it.value as Number))
                                    add(FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, it.field.name, it.field.desc))
                                }

                                is ConstRef.StringRef -> {
                                    val key = Random.nextInt(0x8, 0x800)
                                    val methodName = getRandomString(10)
                                    val decryptMethod = StringEncryptor.createDecryptMethod(methodName, key)
                                    clazz.methods.add(decryptMethod)
                                    add(LdcInsnNode(StringEncryptor.encrypt(it.value, key)))
                                    add(
                                        MethodInsnNode(
                                            Opcodes.INVOKESTATIC,
                                            clazz.name,
                                            methodName,
                                            "(Ljava/lang/String;)Ljava/lang/String;",
                                            false
                                        )
                                    )
                                    add(FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, it.field.name, it.field.desc))
                                }
                            }
                        }
                        add(InsnNode(Opcodes.RETURN))
                    }
                }
                clazz.methods.add(clinit)
            }
        }
        if (integer) Logger.info("    Encrypted $intCount integers")
        if (long) Logger.info("    Encrypted $longCount longs")
        if (float) Logger.info("    Encrypted $floatCount floats")
        if (double) Logger.info("    Encrypted $doubleCount doubles")
        if (string) Logger.info("    Encrypted $stringCount strings")
    }

    interface ConstRef<T> {
        val field: FieldNode
        val value: T

        interface NumberRef<T : Number> : ConstRef<T>

        class IntRef(override val value: Int) : NumberRef<Int> {
            override val field = FieldNode(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                "const_${getRandomString(15)}",
                "I",
                null,
                value
            )
        }

        class LongRef(override val value: Long) : NumberRef<Long> {
            override val field = FieldNode(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                "const_${getRandomString(15)}",
                "J",
                null,
                value
            )
        }

        class FloatRef(override val value: Float) : NumberRef<Float> {
            override val field = FieldNode(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                "const_${getRandomString(15)}",
                "F",
                null,
                value
            )
        }

        class DoubleRef(override val value: Double) : NumberRef<Double> {
            override val field = FieldNode(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                "const_${getRandomString(15)}",
                "D",
                null,
                value
            )
        }

        class StringRef(override val value: String) : ConstRef<String> {
            override val field = FieldNode(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                "const_${getRandomString(15)}",
                "Ljava/lang/String;",
                null,
                value
            )
        }
    }

}