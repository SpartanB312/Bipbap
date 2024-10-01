package net.spartanb312.bipbap.process.impls

import net.spartanb312.bipbap.config.setting
import net.spartanb312.bipbap.process.Transformer
import net.spartanb312.bipbap.process.impls.encrypt.StringEncryptor
import net.spartanb312.bipbap.process.resource.ResourceCache
import net.spartanb312.bipbap.utils.*
import net.spartanb312.bipbap.utils.logging.Logger
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.random.Random

object InvokeDynamics : Transformer("InvokeDynamics") {

    private val rate by setting("ReplacePercentage", 30)
    private val invokeStatic by setting("InvokeStatic", true)
    private val invokeVirtual by setting("InvokeVirtual", true)
    private val exclusion by setting("Exclusion", listOf())

    override fun ResourceCache.transform() {
        Logger.info(" - Replacing invokes to InvokeDynamics...")

        var staticCount = 0
        var virtualCount = 0
        fun shouldApply(classNode: ClassNode, bootstrapName: String, decryptValue: Int): Boolean {
            var shouldApply = false
            classNode.methods
                .filter { !it.isAbstract && !it.isNative && it.checkMixin }
                .forEach { methodNode ->
                    methodNode.instructions.filter { it is MethodInsnNode && it.opcode != Opcodes.INVOKESPECIAL }
                        .forEach { insnNode ->
                            if (insnNode is MethodInsnNode && (0..99).random() < rate && !insnNode.owner.startsWith("[")) {
                                val invokeStaticFlag = invokeStatic && insnNode.opcode == Opcodes.INVOKESTATIC
                                val invokeVirtualFlag = invokeVirtual && insnNode.opcode == Opcodes.INVOKEVIRTUAL
                                if (invokeStaticFlag || invokeVirtualFlag) {
                                    val handle = Handle(
                                        Opcodes.H_INVOKESTATIC,
                                        classNode.name,
                                        bootstrapName,
                                        MethodType.methodType(
                                            CallSite::class.java,
                                            MethodHandles.Lookup::class.java,
                                            String::class.java,
                                            MethodType::class.java,
                                            String::class.java,
                                            String::class.java,
                                            String::class.java,
                                            Integer::class.java
                                        ).toMethodDescriptorString(),
                                        false
                                    )
                                    val invokeDynamicInsnNode = InvokeDynamicInsnNode(
                                        bootstrapName,
                                        if (insnNode.opcode == Opcodes.INVOKESTATIC) insnNode.desc
                                        else insnNode.desc.replace("(", "(Ljava/lang/Object;"),
                                        handle,
                                        StringEncryptor.encrypt(insnNode.owner.replace("/", "."), decryptValue),
                                        StringEncryptor.encrypt(insnNode.name, decryptValue),
                                        StringEncryptor.encrypt(insnNode.desc, decryptValue),
                                        if (insnNode.opcode == Opcodes.INVOKESTATIC) 0 else 1
                                    )
                                    methodNode.instructions.insertBefore(insnNode, invokeDynamicInsnNode)
                                    methodNode.instructions.remove(insnNode)
                                    shouldApply = true
                                    if (invokeStaticFlag) staticCount++
                                    if (invokeVirtualFlag) virtualCount++
                                }
                            }
                        }

                }
            return shouldApply
        }

        nonExcluded.filter {
            !it.isInterface && it.version >= Opcodes.V1_7 && it.name.notInList(exclusion) && it.checkMixin
        }.forEach { classNode ->
            val bootstrapName = massiveString
            val decryptName = massiveString
            val decryptValue = Random.nextInt(0x8, 0x800)
            if (shouldApply(classNode, bootstrapName, decryptValue)) {
                val decrypt = StringEncryptor.createDecryptMethod(decryptName, decryptValue)
                val bsm = createBootstrap(classNode.name, bootstrapName, decryptName)
                classNode.methods.add(decrypt)
                classNode.methods.add(bsm)
            }
        }

        if (invokeStatic) Logger.info("    Replaced $staticCount InvokeStatics")
        if (invokeVirtual) Logger.info("    Replaced $virtualCount InvokeVirtuals")
    }

    private fun createBootstrap(className: String, methodName: String, decryptName: String) =
        MethodNode(
            Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_BRIDGE,
            methodName,
            MethodType.methodType(
                CallSite::class.java,
                MethodHandles.Lookup::class.java,
                String::class.java,
                MethodType::class.java,
                String::class.java,
                String::class.java,
                String::class.java,
                Integer::class.java
            ).toMethodDescriptorString(),
            null,
            null
        ).apply {
            val label1 = Label()
            val label2 = Label()
            val label3 = Label()
            val label4 = Label()
            val label5 = Label()

            visitTryCatchBlock(label1, label2, label3, "java/lang/Exception")
            visitTryCatchBlock(label4, label5, label3, "java/lang/Exception")

            visitVarInsn(Opcodes.ALOAD, 3)
            visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
            visitVarInsn(Opcodes.ASTORE, 7)
            visitVarInsn(Opcodes.ALOAD, 4)
            visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
            visitVarInsn(Opcodes.ASTORE, 8)
            visitVarInsn(Opcodes.ALOAD, 5)
            visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String")
            visitVarInsn(Opcodes.ASTORE, 9)
            visitVarInsn(Opcodes.ALOAD, 6)
            visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
            visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false)
            visitVarInsn(Opcodes.ISTORE, 10)
            visitVarInsn(Opcodes.ALOAD, 9)
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                className,
                decryptName,
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            )
            visitLdcInsn(Type.getType(("L$className").toString() + ";"))
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/Class",
                "getClassLoader",
                "()Ljava/lang/ClassLoader;",
                false
            )
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/invoke/MethodType",
                "fromMethodDescriptorString",
                "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;",
                false
            )
            visitVarInsn(Opcodes.ASTORE, 11)

            visitLabel(label1)
            visitVarInsn(Opcodes.ILOAD, 10)
            visitInsn(Opcodes.ICONST_1)
            visitJumpInsn(Opcodes.IF_ICMPNE, label4)
            visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite")
            visitInsn(Opcodes.DUP)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(Opcodes.ALOAD, 7)
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                className,
                decryptName,
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            )
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false
            )
            visitVarInsn(Opcodes.ALOAD, 8)
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                className,
                decryptName,
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            )
            visitVarInsn(Opcodes.ALOAD, 11)
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles\$Lookup",
                "findVirtual",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            )
            visitVarInsn(Opcodes.ALOAD, 2)
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "asType",
                "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            )
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/invoke/ConstantCallSite",
                "<init>",
                "(Ljava/lang/invoke/MethodHandle;)V",
                false
            )

            visitLabel(label2)
            visitInsn(Opcodes.ARETURN)

            visitLabel(label4)
            visitFrame(
                Opcodes.F_FULL,
                12,
                arrayOf<Any>(
                    "java/lang/invoke/MethodHandles\$Lookup", "java/lang/String",
                    "java/lang/invoke/MethodType", "java/lang/Object", "java/lang/Object", "java/lang/Object",
                    "java/lang/Object", "java/lang/String", "java/lang/String", "java/lang/String", Opcodes.INTEGER,
                    "java/lang/invoke/MethodType"
                ),
                0,
                arrayOf<Any>()
            )
            visitTypeInsn(Opcodes.NEW, "java/lang/invoke/ConstantCallSite")
            visitInsn(Opcodes.DUP)
            visitVarInsn(Opcodes.ALOAD, 0)
            visitVarInsn(Opcodes.ALOAD, 7)
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                className,
                decryptName,
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            )
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/lang/Class",
                "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;",
                false
            )
            visitVarInsn(Opcodes.ALOAD, 8)
            visitMethodInsn(
                Opcodes.INVOKESTATIC,
                className,
                decryptName,
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
            )
            visitVarInsn(Opcodes.ALOAD, 11)
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandles\$Lookup",
                "findStatic",
                "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            )
            visitVarInsn(Opcodes.ALOAD, 2)
            visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/invoke/MethodHandle",
                "asType",
                "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;",
                false
            )
            visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "java/lang/invoke/ConstantCallSite",
                "<init>",
                "(Ljava/lang/invoke/MethodHandle;)V",
                false
            )

            visitLabel(label5)
            visitInsn(Opcodes.ARETURN)

            visitLabel(label3)
            visitFrame(Opcodes.F_SAME1, 0, null, 1, arrayOf<Any>("java/lang/Exception"))
            visitVarInsn(Opcodes.ASTORE, 12)
            visitInsn(Opcodes.ACONST_NULL)
            visitInsn(Opcodes.ARETURN)
            visitMaxs(6, 13)
        }

}