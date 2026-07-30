@file:Suppress("UnstableApiUsage")

package dev.upcraft.gradle.multiloader.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.version
import javax.inject.Inject

abstract class MultiLoaderSettingsPlugin @Inject constructor(factory: ProviderFactory): Plugin<Settings> {

    val version = factory.gradleProperty("dev.upcraft.multiloader.shared_dependencies_version").orElse(factory.provider { MultiLoaderSettingsPlugin::class.java.`package`.implementationVersion })

    override fun apply(target: Settings) = with(target) {
        dependencyResolutionManagement {
            repositories {
                maven("https://maven.uuid.gg/releases") {
                    content { includeModule("dev.upcraft.gradle.multiloader", "shared-dependencies") }
                }
            }
            versionCatalogs {
                register("multiloaderSharedDependencies") {
                    from("dev.upcraft.gradle.multiloader:shared-dependencies:${version.get()}")
                }
            }
        }
        pluginManagement {
            repositories {
                gradlePluginPortal()
            }
            plugins {
                id("dev.upcraft.gradle.multiloader") version version.get()
            }
        }
    }
}
