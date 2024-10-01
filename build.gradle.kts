plugins {
    java
    kotlin("jvm") version "1.9.21"
}

repositories {
    mavenCentral()
    maven("https://repo1.maven.org/maven2/")
    maven("https://mvnrepository.com/artifact/")
}

val asmVersion = "9.7"

val library: Configuration by configurations.creating

dependencies {
    //Kotlin
    library(kotlin("stdlib"))

    //ASM
    library("org.ow2.asm:asm:$asmVersion")
    library("org.ow2.asm:asm-tree:$asmVersion")
    library("org.ow2.asm:asm-commons:$asmVersion")

    //GSON
    library("com.google.code.gson:gson:2.10")

    implementation(library)
}

tasks {

    compileJava {
        options.encoding = "UTF-8"
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "1.8"
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