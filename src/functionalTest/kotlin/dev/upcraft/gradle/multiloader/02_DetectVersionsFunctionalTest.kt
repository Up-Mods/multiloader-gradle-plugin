package dev.upcraft.gradle.multiloader

import org.gradle.testkit.runner.GradleRunner
import java.io.File
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertTrue

class `02_DetectVersionsFunctionalTest` {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    private val buildRepo by lazy { System.getProperty("dev.upcraft.multiloader.test.build_repo") }
    private val buildVersion by lazy { System.getProperty("dev.upcraft.multiloader.shared_dependencies_version") }

    @ParameterizedTest
    @CsvSource("25,26.1.2", "17,1.20.1", "8,1.16.5")
    fun `can detect Java version`(javaVersion: Int, mcVersion: String) {
        // Set up the test build
        propertiesFile.writeText("""
            dev.upcraft.multiloader.shared_dependencies_version=${buildVersion}
        """.trimIndent())
        settingsFile.writeText("""
            dependencyResolutionManagement {
                repositories {
                    maven {
                        url = uri("$buildRepo")
                        name = "buildDir"
                    }
                }
            }
            
            plugins {
                id("dev.upcraft.gradle.multiloader.settings")
            }
            
            rootProject.name = "functional-test"
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id("dev.upcraft.gradle.multiloader")
            }
            
            group = "dev.upcraft.test"
            version = "0.1.0-SNAPSHOT"
            
            multiLoader {
                minecraftVersion = "$mcVersion"
                javaVersion = $javaVersion
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .build()

        assertTrue(result.output.contains("Minecraft: $mcVersion"))
        assertTrue(result.output.contains("Java: $javaVersion"))
    }
}
