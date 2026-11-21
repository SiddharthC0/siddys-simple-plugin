
plugins {
    id("java")
    id("com.gradleup.shadow") version "9.5.0"
}

group = "org.siddharth"
version = "4.0.0"

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
    compileOnly("org.apache.logging.log4j:log4j-core:2.19.0")

    implementation("org.xerial:sqlite-jdbc:3.54.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    archiveClassifier.set("plain")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    manifest {
        attributes(
            "Implementation-Title" to "Siddy's Simple Plugin",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Siddharth C",
            "Specification-Title" to "SSMP",
            "Specification-Version" to project.version,
            "Main-Class" to "org.siddharth.ssmp.ssmp"
        )
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.register("inspectJar") {
    dependsOn(tasks.shadowJar)

    doLast {
        val file = tasks.shadowJar.get().archiveFile.get().asFile

        println("--- JAR Inspection ---")
        println("Name: ${file.name}")
        println("Size: ${file.length() / 1024} KB")
        println("Location: ${file.absolutePath}")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
