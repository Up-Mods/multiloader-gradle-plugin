package dev.upcraft.gradle.multiloader

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

fun RepositoryHandler.modrinth() {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
}

fun RepositoryHandler.curseforge() {
    exclusiveContent {
        forRepository {
            maven("https://www.cursemaven.com") {
                name = "Curseforge"
            }
        }
        filter { includeGroup("curse.maven") }
    }
}

internal fun Project.applyRepositories() {
    repositories {
        mavenCentral()

        // mod loaders
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForge"
        }
        maven("https://maven.fabricmc.net") {
            name = "FabricMC"
        }

        // mod platforms
        curseforge()
        modrinth()

        maven("https://maven.uuid.gg/releases")
    }
}

/**
 * [MC-Transforers](https://github.com/AshsWorkshop/mc-transformers)
 */
fun RepositoryHandler.mcTransformer() {
    exclusiveContent {
        forRepository {
            maven("https://maven.uuid.gg/snapshots")
        }
        filter { includeGroup("net.ashwork.mc") }
    }
}

/**
 * [DevLogin](https://github.com/covers1624/DevLogin)
 */
fun RepositoryHandler.devLogin() {
    exclusiveContent {
        forRepository {
            maven("https://maven.covers1624.net")
        }
        filter {
            includeGroup("net.covers1624")
        }
    }
}
