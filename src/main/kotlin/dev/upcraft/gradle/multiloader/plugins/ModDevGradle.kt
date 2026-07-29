@file:Suppress("UnstableApiUsage")

package dev.upcraft.gradle.multiloader.plugins

import dev.upcraft.gradle.multiloader.MultiloaderExtension
import dev.upcraft.gradle.multiloader.mcTransformer
import dev.upcraft.gradle.multiloader.shared
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.slf4j.event.Level
import java.util.Locale

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
}

fun applyMDG(target: Project, ext: MultiloaderExtension) = with(target) {
    val localRuntime = configurations.dependencyScope("localRuntime")
    configurations.named("runtimeClasspath").configure { extendsFrom(localRuntime) }

    extensions.configure<NeoForgeExtension> {
        validateAccessTransformers = true

        val java = the<JavaPluginExtension>()
        java.sourceSets["main"].resources.sourceDirectories.files
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .filter { it.name == "META-INF" }
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .find { it.name == "accesstransformer.cfg" }
            ?.let { atFile ->
                accessTransformers {
                    from(atFile)
                    publish(atFile)
                }
            }

        java.sourceSets["main"].resources.sourceDirectories.files
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .filter { it.name == "META-INF" }
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .find { it.name == "interfaces.json" }
            ?.let { interfacesFile ->
                interfaceInjectionData {
                    from(interfacesFile)
                    publish(interfacesFile)
                }
            }

        afterEvaluate {
            addModdingDependenciesTo(the<JavaPluginExtension>().sourceSets["test"])

            if(ext.applySharedAccessTransforms.get()) {
                repositories.mcTransformer()
                dependencies {
                    "accessTransformers"("net.ashwork.mc:transformers:${ext.minecraftVersion.get()}.+")
                }
            }

            if(ext.loader.get() == "common") {
                val shared = the<VersionCatalogsExtension>().shared
                dependencies {
                    shared.findBundle("mixin").ifPresent { "compileOnly"(it) }
                }
            }
            else {
                unitTest {
                    enable()

                    testedMod = mods[ext.modId.get()]
                    loadedMods = listOf(mods[ext.modId.get()])
                }

                runs {
                    register("client") {
                        client()
                        devLogin = ext.devLogin
                        gameDirectory = file("run/client")
                        systemProperty("neoforge.enabledGameTestNamespaces", ext.modId.get())

                        sourceSet = java.sourceSets["main"]
                        loadedMods = listOf(mods[ext.modId.get()])
                    }

                    register("server") {
                        server()
                        gameDirectory = file("run/server")
                        systemProperty("neoforge.enabledGameTestNamespaces", ext.modId.get())

                        sourceSet = java.sourceSets["main"]
                        loadedMods = listOf(mods[ext.modId.get()])

                        programArgument("--nogui")
                    }

                    if(ext.hasTestmod) {
                        register("testmodClient") {
                            client()
                            devLogin = ext.devLogin
                            gameDirectory = file("run/testmod_client")
                            systemProperty("neoforge.enabledGameTestNamespaces", ext.getTestModId())

                            sourceSet = java.sourceSets["testmod"]
                            loadedMods = listOf(mods[ext.modId.get()], mods[ext.getTestModId()])
                        }

                        register("testmodServer") {
                            server()
                            gameDirectory = file("run/testmod_server")
                            systemProperty("neoforge.enabledGameTestNamespaces", ext.getTestModId())

                            sourceSet = java.sourceSets["testmod"]
                            loadedMods = listOf(mods[ext.modId.get()], mods[ext.getTestModId()])

                            programArgument("--nogui")
                        }
                    }

                    configureEach {
                        systemProperty("terminal.ansi", "true")

                        systemProperty("sparkweave.debug", ext.debugRuns.map { it.toString() }.get())
                        systemProperty("mixin.debug", ext.mixinDebugRuns.map { it.toString() }.get())
                        if(ext.loaderDebugRuns.get()) {
                            logLevel = Level.DEBUG
                            systemProperty("forge.logging.markers", "REGISTRIES")
                        }

                        ideName = "NeoForge ${name.replaceFirstChar { it.titlecase(Locale.ROOT) }}"
                    }
                }
            }
        }
    }
}

fun applyMDGCommonProject(current: Project, commonProject: String) = with(current) {
    val metaInf = project(commonProject).the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files
        .flatMap { it.listFiles()?.toList() ?: listOf() }
        .filter { it.name == "META-INF" }
        .flatMap { it.listFiles()?.toList() ?: listOf() }

    if(metaInf.any { it.name == "accesstransformer.cfg" }) {
        dependencies {
            "accessTransformers"(project(commonProject))
        }
    }

    if(metaInf.any { it.name == "interfaces.json" }) {
        dependencies {
            "interfaceInjectionData"(project(commonProject))
        }
    }
}
