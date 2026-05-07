package net.spartanb312.bipbap.gradle.jvm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class BipbapJvmPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("bipbap", BipbapJvmExtension::class.java)
        project.plugins.withId("java") {
            configureJvm(project, extension)
        }
    }

    private fun configureJvm(project: Project, extension: BipbapJvmExtension) {
        val jarTask = project.tasks.named("jar", Jar::class.java)
        val defaultOutput = jarTask.flatMap { jar ->
            project.layout.buildDirectory.file("libs/${jar.archiveFileName.get().removeSuffix(".jar")}-obf.jar")
        }
        val runtimeClasspath = project.configurations.findByName("runtimeClasspath")
        val runtimeLibraries = project.files({
            if (extension.includeRuntimeClasspath.get()) runtimeClasspath?.files ?: emptySet<Any>()
            else emptySet<Any>()
        })

        val obfuscateTask = project.tasks.register("bipbapObfuscateJar", BipbapJvmObfuscateTask::class.java) { task ->
            task.dependsOn(jarTask)
            task.inputJar.set(extension.inputJar.orElse(jarTask.flatMap { it.archiveFile }))
            task.outputJar.set(extension.outputJar.orElse(defaultOutput))
            task.summaryFile.set(project.layout.buildDirectory.file("reports/bipbap/jvm.txt"))
            task.enabledProperty.set(extension.enabled)
            task.preset.set(extension.preset)
            task.threads.set(extension.threads)
            task.configFile.set(extension.configFile)
            task.authUrl.set(extension.authUrl)
            task.extraExclusions.set(extension.exclusions)
            task.libraries.from(runtimeLibraries)
            task.libraries.from(extension.libraries)
        }

        project.afterEvaluate {
            if (extension.attachToAssemble.get()) {
                project.tasks.named("assemble").configure { task ->
                    task.dependsOn(obfuscateTask)
                }
            }
        }
    }
}
