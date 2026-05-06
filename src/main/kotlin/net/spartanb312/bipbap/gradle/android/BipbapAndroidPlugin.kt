package net.spartanb312.bipbap.gradle.android

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Locale

class BipbapAndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("bipbap", BipbapAndroidExtension::class.java)
        project.plugins.withId("com.android.application") { configureAndroid(project, extension) }
        project.plugins.withId("com.android.library") { configureAndroid(project, extension) }
    }

    private fun configureAndroid(project: Project, extension: BipbapAndroidExtension) {
        val keepRulesFile = project.layout.buildDirectory.file("generated/bipbap/proguard/bipbap-keep-rules.pro")
        val keepRulesTask = project.tasks.register(
            "generateBipbapAndroidProguardRules",
            BipbapAndroidProguardRulesTask::class.java
        ) { task ->
            task.outputFile.set(keepRulesFile)
        }

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            if (!isBuildTypeEnabled(variant.name, extension.buildTypes.get())) return@onVariants
            if (extension.proguardCompatible.get()) {
                variant.proguardFiles.add(keepRulesTask.flatMap { it.outputFile })
            }

            val taskProvider = project.tasks.register(
                "${variant.name}BipbapAndroidClasses",
                BipbapAndroidTransformTask::class.java
            ) { task ->
                task.variantName.set(variant.name)
                task.summaryFile.set(project.layout.buildDirectory.file("reports/bipbap/${variant.name}.txt"))
                task.enabledProperty.set(extension.enabled)
                task.preset.set(extension.preset)
                task.threads.set(extension.threads)
                task.safeMode.set(extension.safeMode)
                task.proguardCompatible.set(extension.proguardCompatible)
                task.extraExclusions.set(extension.exclusions)
            }

            variant.artifacts
                .forScope(ScopedArtifacts.Scope.PROJECT)
                .use(taskProvider)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    BipbapAndroidTransformTask::allJars,
                    BipbapAndroidTransformTask::allDirectories,
                    BipbapAndroidTransformTask::output
                )
        }
    }

    private fun isBuildTypeEnabled(variantName: String, buildTypes: List<String>): Boolean {
        val normalized = buildTypes.map { it.trim() }.filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return true
        return normalized.any { buildType ->
            val suffix = buildType.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }
            variantName.equals(buildType, ignoreCase = true) || variantName.endsWith(suffix)
        }
    }
}
