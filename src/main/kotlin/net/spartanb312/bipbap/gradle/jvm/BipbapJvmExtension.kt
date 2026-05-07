package net.spartanb312.bipbap.gradle.jvm

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class BipbapJvmExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val preset: Property<String> = objects.property(String::class.java).convention("low")
    val threads: Property<Int> = objects.property(Int::class.java).convention(1)
    val attachToAssemble: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val includeRuntimeClasspath: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val inputJar: RegularFileProperty = objects.fileProperty()
    val outputJar: RegularFileProperty = objects.fileProperty()
    val configFile: RegularFileProperty = objects.fileProperty()
    val authUrl: Property<String> = objects.property(String::class.java)
    val libraries: ConfigurableFileCollection = objects.fileCollection()
    val exclusions: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
}
