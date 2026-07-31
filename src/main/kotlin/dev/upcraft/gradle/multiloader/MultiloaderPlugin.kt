@file:Suppress("UnstableApiUsage")
@file:OptIn(ExperimentalTime::class)

package dev.upcraft.gradle.multiloader

import dev.upcraft.gradle.multiloader.plugins.FabricLoom
import dev.upcraft.gradle.multiloader.plugins.ModDevGradle
import dev.upcraft.gradle.multiloader.plugins.applyLoom
import dev.upcraft.gradle.multiloader.plugins.applyMDG
import dev.upcraft.gradle.multiloader.time.BuildTimeManifestTask
import dev.upcraft.gradle.multiloader.time.BuildTimeValueSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.of
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel
import kotlin.time.ExperimentalTime

@Suppress("unused")
abstract class MultiloaderPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("java-library")
        pluginManager.apply("maven-publish")
        pluginManager.apply("version-catalog")

        val now = providers.of(BuildTimeValueSource::class) {}

        if (this != rootProject) {
            version = rootProject.version
        }

        applySharedDependencies(this)

        val ext = extensions.create<MultiloaderExtension>("multiLoader")

        pluginManager.withPlugin(ModDevGradle.PLUGIN_ID) {
            applyMDG(this@with, ext)
        }

        //FIXME genuine gradle bug yay
        pluginManager.withPlugin(FabricLoom.LEGACY_PLUGIN_ID) {
            if(project.pluginManager.hasPlugin(FabricLoom.PLUGIN_ID)) {
                applyLoom(this@with, ext)
            }
        }

        val javaExt = the<JavaPluginExtension>()
        javaExt.toolchain {
            languageVersion = ext.javaVersion.map { JavaLanguageVersion.of(it) }
            vendor = JvmVendorSpec.MICROSOFT
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release = ext.javaVersion

            // docs.oracle.com/en/java/javase/25/docs/specs/man/javac.html#options
            val xlint = listOf(
                "cast", // unnecessary casts
                "dangling-doc-comments", // dangling javadoc
                "text-blocks", // inconsistent whitespace in textblocks
                "dep-ann", // deprecated in javadoc but no @Deprecated annotation
                "empty", // empty if statements
                "overrides",
                "deprecation",
                "removal",
                "rawtypes",
                "unchecked",
                "static", // static method access using object instance
                "varargs",
            )
            options.compilerArgs.addAll(listOf(
                "-Xmaxerrs", "500",
                "-Xmaxwarns", "500",
                "-Werror", // warnings as errors
                "-Xlint:${xlint.joinToString(",")}",
                "-Xpkginfo:nonempty", // only emit package-info.class if it contains class or runtime scope annotations
            ))
        }

        applyRepositories()

        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).tags(listOf("reason", "implNote"))
        }

        tasks.named<Test>("test").configure {
            useJUnitPlatform()
        }

        tasks.withType<ProcessResources>().configureEach {
            filteringCharset = "UTF-8"
        }

        the<PublishingExtension>().apply {
            publications {
                register<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }

            providers.environmentVariable("MAVEN_UPLOAD_URL").orNull?.let { url ->
                repositories {
                    maven(url) {
                        credentials {
                            username = providers.environmentVariable("MAVEN_UPLOAD_USERNAME").orNull
                            password = providers.environmentVariable("MAVEN_UPLOAD_PASSWORD").orNull
                        }
                    }
                }
            }
        }

        afterEvaluate {
            logger.lifecycle("Minecraft: ${ext.minecraftVersion.get()}")
            logger.lifecycle("Java: ${ext.javaVersion.get()}")

            if(ext.generateSources.get()) {
                javaExt.withSourcesJar()
            }
            if(ext.generateJavadoc.get()) {
                javaExt.withJavadocJar()
            }

            applyMcGradleConventions(ext.loader.get())

            listOf("LICENSE", "LICENSE.md").forEach { filename ->
                val license = rootProject.file(filename)
                val filenameNoExt = license.nameWithoutExtension
                val filenameExt = license.extension.let { if(it.isNotEmpty()) ".${it}" else it }
                if(license.exists()) {
                    tasks.named<Jar>("jar").configure {
                        inputs.file(license)
                        from(license) {
                            rename(filename, "${filenameNoExt}_${rootProject.name}${filenameExt}")
                        }
                    }

                    if(ext.generateSources.get()) {
                        tasks.named<Jar>("sourcesJar").configure {
                            inputs.file(license)
                            from(license) {
                                rename(filename, "${filenameNoExt}_${rootProject.name}${filenameExt}")
                            }
                        }
                    }
                }
            }

            if(ext.loader.get() == "common") {
                val commonJava = configurations.consumable("commonJava")
                val commonResources = configurations.consumable("commonResources")

                extensions.configure<JavaPluginExtension> {
                    artifacts {
                        sourceSets["main"].java.sourceDirectories.forEach { add(commonJava.name, it) }
                        sourceSets["main"].resources.sourceDirectories.forEach { add(commonResources.name, it) }
                    }
                }

                dependencies {
                    platform("net.neoforged:minecraft-dependencies:${ext.minecraftVersion.get()}")
                }

                if(ext.hasTestMod()) {
                    val testmodCommonResources = configurations.consumable("testmodCommonResources")
                    val testmodCommonJava = configurations.consumable("testmodCommonJava")

                    extensions.configure<JavaPluginExtension> {
                        artifacts {
                            sourceSets["testmod"].java.sourceDirectories.forEach { add(testmodCommonJava.name, it) }
                            sourceSets["testmod"].resources.sourceDirectories.forEach { add(testmodCommonResources.name, it) }
                        }
                    }
                }


            }

            fun isJson(path: String): Boolean {
                return listOf("json", "mcmeta").any { path.endsWith(it) }
            }

            the(JavaPluginExtension::class).sourceSets.forEach {
                tasks.named<ProcessResources>(it.processResourcesTaskName).configure {
                    val expandProps = mapOf<String, Any?>(
                        "version" to ext.modVersion.get(),
                        "maven_group_id" to ext.modGroup.get(),
                        "mod_id" to ext.modId.get(),
                        "mod_display_name" to ext.modDisplayName.orElse(ext.modId).get(),
                        "mod_description" to ext.modDescription.orNull,
                        "sources_url" to ext.modSourcesUrl.orNull,
                        "issues_url" to ext.modIssuesUrl.orNull,
                        "license_url" to ext.modLicenseUrl.orNull,
                        "discord_url" to ext.modDiscordUrl.orNull,
                        "homepage_url" to ext.modHomepageUrl.orNull,

                        "curseforge_id" to ext.curseforgeId.orNull,
                        "modrinth_id" to ext.modrinthId.orNull,

                        "java_version" to ext.javaVersion.get(),
                        "minecraft_version" to ext.minecraftVersion.get()
                    )
                    inputs.properties(expandProps)

                    ext.processResourcesProperties.forEach { (patterns, extraProperties) ->
                        val finalProps = mutableMapOf<String, Any?>()
                        finalProps.putAll(expandProps)
                        extraProperties?.mapValues { entry -> if(entry.value is Provider<*>) (entry.value as Provider<*>).orNull else entry.value }?.let { map ->
                            inputs.properties(map)
                            finalProps.putAll(map)
                        }

                        patterns.filter(::isJson).let { filters ->
                            if(filters.isNotEmpty()) {
                                filesMatching(filters) {
                                    expand(finalProps) {
                                        escapeBackslash = true
                                    }
                                }
                            }
                        }
                        patterns.filterNot(::isJson).let { filters ->
                            if(filters.isNotEmpty()) {
                                filesMatching(filters) {
                                    expand(finalProps)
                                }
                            }
                        }
                    }
                }
            }

            if(ext.generateManifestBuildTimestamp.get()) {
                val tmpManifest = tasks.register<BuildTimeManifestTask>("createManifestTimestamp") {
                    description = "Create manifest file with build timestamp"
                    group = "build"

                    buildTime = now
                }

                tasks.withType<Jar>().configureEach {
                    manifest.from(tmpManifest.flatMap { it.manifestPath })
                    dependsOn(tmpManifest)
                }
            }

            tasks.withType<Jar>().configureEach {
                exclude(".cache/**")

                manifest.attributes(mapOf<String, Any>(
                    "Specification-Title" to rootProject.name,
                    "Specification-Version" to rootProject.version,

                    "Implementation-Title" to project.name,
                    "Implementation-Version" to archiveVersion,

                    "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})",
                    "Built-On-Minecraft" to ext.minecraftVersion.get()
                ))
            }
        }

        // IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
        rootProject.pluginManager.apply("idea")
        rootProject.the(IdeaModel::class).module {
            isDownloadSources = true
        }
    }
}
