package dev.upcraft.gradle.multiloader

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class `01_SettingsPluginFunctionalTest` {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }
    private val propertiesFile by lazy { projectDir.resolve("gradle.properties") }

    private val buildRepo by lazy { System.getProperty("dev.upcraft.multiloader.test.build_repo") }
    private val buildVersion by lazy { System.getProperty("dev.upcraft.multiloader.shared_dependencies_version") }

    @Test
    fun `does throw without settings plugin`() {
        propertiesFile.writeText("""
            dev.upcraft.multiloader.shared_dependencies_version=${buildVersion}
        """.trimIndent())
        settingsFile.writeText("""
            rootProject.name = "functional-test"
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id("dev.upcraft.gradle.multiloader")
            }
            
            group = "dev.upcraft.test"
            version = "0.1.0-SNAPSHOT"
            
            multiLoader {
                minecraftVersion = "26.1.2"
            }
        """.trimIndent())

        val runner = GradleRunner.create()
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("dependencies", "--stacktrace")
            .withProjectDir(projectDir)
        val err = assertThrows<UnexpectedBuildFailure> { runner.build() }
        assertTrue(err.buildResult.output.contains("did you forget to apply the 'dev.upcraft.gradle.multiloader.settings' plugin"))
    }

    @Test
    fun `does register shared version catalog`() {
        propertiesFile.writeText("""
            dev.upcraft.multiloader.shared_dependencies_version=${buildVersion}
        """.trimIndent())
        settingsFile.writeText("""
            dependencyResolutionManagement {
                repositories {
                    exclusiveContent {
                        forRepository {
                            maven("$buildRepo") { name = "buildDir" }
                        }
                        filter {
                            includeModule("dev.upcraft.gradle.multiloader", "shared-dependencies")
                        }
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
                minecraftVersion = "26.1.2"
            }
        """.trimIndent())

        val result = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withArguments("dependencies", "--stacktrace")
        .withProjectDir(projectDir)
        .build()

        assertTrue(result.output.contains("com.google.auto.service:auto-service-annotations"))
    }
}
