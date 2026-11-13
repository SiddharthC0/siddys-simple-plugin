
plugins {
    id("java")
}

group = "org.siddharth"
version = "2.2.1"

base {
    archivesName.set("SiddysSimplePlugin")
}

repositories {
    mavenCentral()

    maven {
        name = "spigotmc"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }

    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://jitpack.io")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.3-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.7")
    compileOnly("com.github.milkbowl:VaultAPI:1.7.1")
}


java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    archiveClassifier.set("")

    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes(
            "Implementation-Title" to "Siddy's Simple Plugin",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Siddharth Chauhan",
            "Specification-Title" to "SSMP",
            "Specification-Version" to project.version,
            "Main-Class" to "org.siddharth.ssmp.ssmp"
        )
    }
}

tasks.register("inspectJar") {
    dependsOn(tasks.jar)

    doLast {
        val file = tasks.jar.get().archiveFile.get().asFile

        println("Name: ${file.name}")
        println("Size: ${file.length() / 1024} KB")
        println("Location: ${file.absolutePath}")
    }
}
