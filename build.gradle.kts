plugins {
    kotlin("jvm")
    id("fabric-loom")
    `maven-publish`
    java
}

group = property("maven_group")!!
version = property("mod_version")!!


repositories {
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype-oss-snapshots1"
        mavenContent { snapshotsOnly() }
    }
    mavenCentral()
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
    maven { url= uri("https://maven.nucleoid.xyz") }
    maven("https://maven.impactdev.net/repository/development/")
        maven {
        url = uri("https://vault.roanoke.dev/releases")
    }

    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
    maven { url= uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    modImplementation("net.kyori:adventure-platform-fabric:5.9.0")!!
    modImplementation("net.kyori:adventure-text-minimessage:4.14.0")!!

    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    modImplementation("eu.pb4:sgui:1.2.2+1.20")

    modImplementation("dev.roanoke:rib:1.8.6")

    modImplementation("com.cobblemon:fabric:1.5.0+1.20.1-SNAPSHOT")

    modImplementation("net.luckperms:api:5.4")

    modImplementation("eu.pb4:placeholder-api:2.1.3+1.20.1")!!

    modImplementation("me.lucko:fabric-permissions-api:0.2-SNAPSHOT")
}

tasks {

    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version))
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifact(remapJar) {
                    builtBy(remapJar)
                }
                artifact(kotlinSourcesJar) {
                    builtBy(remapSourcesJar)
                }
            }
        }

        // select the repositories you want to publish to
        repositories {
            // uncomment to publish to the local maven
            // mavenLocal()
        }
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}



// configure the maven publication