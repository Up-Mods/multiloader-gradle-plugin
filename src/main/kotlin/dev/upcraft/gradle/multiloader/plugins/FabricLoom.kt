@file:Suppress("UnstableApiUsage")

package dev.upcraft.gradle.multiloader.plugins

import dev.upcraft.gradle.multiloader.MultiloaderExtension
import dev.upcraft.gradle.multiloader.devLogin
import dev.upcraft.gradle.multiloader.mcTransformer
import dev.upcraft.gradle.multiloader.shared
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.LoomTasks
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object FabricLoom {
    const val LEGACY_PLUGIN_ID = "fabric-loom"
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
}

fun applyLoom(target: Project, ext: MultiloaderExtension) = with(target) {

    // must apply this early or loom yells at us
    repositories.devLogin()

    val extraDeps = configurations.dependencyScope("multiloaderExtraDeps")
    configurations.named("runtimeClasspath").configure { extendsFrom(extraDeps) }

    // TODO create default and testmod run configs

    val loom = the(LoomGradleExtensionAPI::class)
    loom.accessWidenerPath.convention(provider { the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files
        .flatMap { it.listFiles()?.toList() ?: listOf() }
        .firstOrNull { it.name == "${ext.modId.get()}.classtweaker" }
    }.map { layout.projectDirectory.file(it.absolutePath) })

    afterEvaluate {
        if(ext.applySharedAccessTransforms.get()) {
            repositories.mcTransformer()
            dependencies {
                "implementation"("net.ashwork.mc:transformers:${ext.minecraftVersion.get()}.+")
            }
        }

        val shared = the(VersionCatalogsExtension::class).shared

        if(ext.loader.get() != "common") {
            loom.runConfigs {
                named("client") {
                    client()
                    displayName = "Fabric Client"
                    runDirectory = file("run/client")
                }
                named("server") {
                    server()
                    displayName = "Fabric Server"
                    runDirectory = file("run/server")
                }

                if(ext.hasTestmod) {
                    create("testmodClient") {
                        client()
                        displayName = "Fabric TestmodClient"
                        runDirectory = file("run/testmod_client")
                        sourceSet = "testmod"
                    }
                    create("testmodServer") {
                        server()
                        displayName = "Fabric TestmodServer"
                        runDirectory = file("run/testmod_server")
                        sourceSet = "testmod"
                    }
                }
            }
        }

        loom.runConfigs.configureEach {
            appendProjectPathToDisplayName = false

            systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
            systemProperties.put("sparkweave.debug", ext.debugRuns.map { it.toString() }.get())
            systemProperties.put("mixin.debug", ext.mixinDebugRuns.map { it.toString() }.get())
            if(ext.loaderDebugRuns.get()) {
                systemProperties.put("fabric.log.level", "debug")
            }

            // register as Gradle runs instead of IDEA runs
            // https://github.com/FabricMC/fabric-loom/issues/1349
            generateRunConfig = false
            rootProject.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")
            rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>(displayName.get()) {
                taskNames = listOf(LoomTasks.getRunConfigTaskName(this@configureEach))
                setProject(project)
            }
        }

        if (ext.devLogin.get()) {
            dependencies {
                extraDeps(shared.findLibrary("devlogin").orElseThrow())
            }

            loom.runConfigs.configureEach {
                if (runtimeEnvironment.get() == "client") {
                    programArguments.addAll(
                        listOf(
                            "--launch_target",
                            "net.fabricmc.loader.impl.launch.knot.KnotClient"
                        )
                    )
                    mainClass = "net.covers1624.devlogin.DevLogin"
                }
            }
        }
    }
}

fun applyLoomMcGradleConventions(target: Project, loader: String, attribute: Attribute<String>) = with(target) {
    pluginManager.withPlugin(FabricLoom.PLUGIN_ID) {
        project.configurations.named("modCompileClasspath").configure {
            attributes { attribute(attribute, loader) }
        }
    }
}
