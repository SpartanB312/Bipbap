package net.spartanb312.bipbap.gradle.android

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class BipbapAndroidExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val preset: Property<String> = objects.property(String::class.java).convention("low")
    val threads: Property<Int> = objects.property(Int::class.java).convention(1)
    val safeMode: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val proguardCompatible: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val buildTypes: ListProperty<String> = objects.listProperty(String::class.java).convention(listOf("release"))
    val exclusions: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
}
