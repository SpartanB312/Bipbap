plugins {
    java
    `java-gradle-plugin`
    kotlin("jvm") version "1.9.21"
}

repositories {
    google()
    mavenCentral()
    maven("https://repo1.maven.org/maven2/")
    maven("https://mvnrepository.com/artifact/")
}

val asmVersion = "9.7"

val library: Configuration by configurations.creating
configurations.implementation {
    extendsFrom(library)
}

dependencies {
    //Kotlin
    library(kotlin("stdlib"))

    //ASM
    library("org.ow2.asm:asm:$asmVersion")
    library("org.ow2.asm:asm-tree:$asmVersion")
    library("org.ow2.asm:asm-commons:$asmVersion")

    //GSON
    library("com.google.code.gson:gson:2.10")

    //Gradle Plugin APIs
    compileOnly("com.android.tools.build:gradle:8.1.4")
}

gradlePlugin {
    plugins {
        create("bipbapAndroid") {
            id = "net.spartanb312.bipbap.android"
            implementationClass = "net.spartanb312.bipbap.gradle.android.BipbapAndroidPlugin"
        }
    }
}

tasks {

    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveBaseName.set(project.name.toLowerCase())

        manifest {
            attributes(
                "Main-Class" to "net.spartanb312.bipbap.BipbapKt"
            )
        }

        from(
            library.map {
                if (it.isDirectory) it
                else zipTree(it)
            }
        )

        exclude("META-INF/versions/**", "module-info.class", "**/**.RSA")
    }

}
