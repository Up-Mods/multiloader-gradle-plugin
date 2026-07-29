@file:Suppress("UnstableApiUsage")

import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publishing)
}

group = "dev.upcraft.gradle.multiloader"
version = providers.environmentVariable("TAG").orElse("0.1.0-dev-SNAPSHOT")

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.neoforged.net/releases") {
        name = "NeoForge"
    }
    maven("https://maven.fabricmc.net") {
        name = "FabricMC"
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(project(":multiloader-gradle-settings-plugin"))
    implementation(libs.idea.ext)
    compileOnly(libs.moddevgradle)
    compileOnly(libs.fabric.loom)
}

java {
    toolchain {
        languageVersion = libs.versions.java.map(JavaLanguageVersion::of)
    }

    withSourcesJar()
}

tasks.named<Jar>("jar").configure {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})"
    )
}

gradlePlugin {
    website = "https://mods.upcraft.dev"
    vcsUrl = "https://github.com/Up-Mods/multiloader-gradle-plugin.git"

    plugins.create("multiloader") {
        id = "dev.upcraft.gradle.multiloader"
        implementationClass = "dev.upcraft.gradle.multiloader.MultiloaderPlugin"
        displayName = "Minecraft Multiloader Plugin"
        description = "Common configuration plugin for multi-loader environment development of Minecraft mods"
        tags.addAll("minecraft", "multiloader", "mods")
        compatibility {
            features {
                configurationCache = true
            }
        }
    }
}

val buildRepo = project(":multiloader-gradle-settings-plugin").layout.buildDirectory.dir("repo").map { it.asFile.toURI() }

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
            useKotlinTest(libs.versions.kotlin)
        }

        val functionalTest = register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit)
            useKotlinTest(libs.versions.kotlin)

            dependencies {
                implementation(project())
                implementation(libs.junit.parameterized.tests)
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure {
                        shouldRunAfter(test)
                        dependsOn(project(":multiloader-gradle-settings-plugin").tasks.named("publishAllPublicationsToBuildDirRepository"))
                        systemProperty("dev.upcraft.multiloader.test.build_repo", buildRepo.get())
                        systemProperty("dev.upcraft.multiloader.shared_dependencies_version", version.toString())
                    }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check").configure {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
