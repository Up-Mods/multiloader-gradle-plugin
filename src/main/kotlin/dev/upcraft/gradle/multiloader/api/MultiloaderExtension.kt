package dev.upcraft.gradle.multiloader.api

import dev.upcraft.gradle.multiloader.applyCommonProjectDependency
import dev.upcraft.gradle.multiloader.shared
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import java.io.File
import javax.inject.Inject

@Suppress("unused")
abstract class MultiloaderExtension @Inject constructor(factory: ProviderFactory, val project: Project) {

    abstract val javaVersion: Property<Int>
    abstract val minecraftVersion: Property<String>

    abstract val generateSources: Property<Boolean>
    abstract val generateJavadoc: Property<Boolean>

    abstract val generateManifestBuildTimestamp: Property<Boolean>

    // mod properties
    abstract val modVersion: Property<String>
    abstract val modGroup: Property<String>
    abstract val modId: Property<String>
    abstract val modDisplayName: Property<String>
    abstract val modDescription: Property<String>
    abstract val modSourcesUrl: Property<String>
    abstract val modIssuesUrl: Property<String>
    abstract val modLicenseUrl: Property<String>
    abstract val modDiscordUrl: Property<String>
    abstract val modHomepageUrl: Property<String>

    abstract val curseforgeId: Property<Any>
    abstract val modrinthId: Property<Any>

    abstract val loader: Property<String>

    abstract val applySharedAccessTransforms: Property<Boolean>

    // run configurations
    abstract val commonRunDirectory: Property<Boolean>

    /**
     * [DevLogin](https://github.com/covers1624/DevLogin)
     */
    abstract val devLogin: Property<Boolean>
    abstract val debugRuns: Property<Boolean>
    abstract val mixinDebugRuns: Property<Boolean>
    abstract val loaderDebugRuns: Property<Boolean>

    internal var testmodConfig: TestmodConfiguration? = null

    internal var processResourcesProperties: List<Pair<List<String>, Map<String, Any>?>> = mutableListOf()

    init {
        javaVersion.convention(25)

        generateSources.convention(factory.gradleProperty("dev.upcraft.gradle.multiloader.generate_sources").map { it.toBoolean() }.orElse(true))
        generateJavadoc.convention(factory.gradleProperty("dev.upcraft.gradle.multiloader.generate_javadoc").map { it.toBoolean() }.orElse(false))

        generateManifestBuildTimestamp.convention(true)

        modVersion.convention(factory.provider { project.version.toString() })
        modGroup.convention(factory.provider { project.group.toString() })
        modId.convention(factory.gradleProperty("mod_id"))
        modDisplayName.convention(factory.gradleProperty("mod_display_name").orElse(factory.provider { project.displayName }))
        modDescription.convention(factory.gradleProperty("mod_description").orElse(""))
        modDiscordUrl.convention(factory.gradleProperty("discord_url").orElse(""))
        modSourcesUrl.convention(factory.gradleProperty("sources_url").orElse(""))
        modIssuesUrl.convention(factory.gradleProperty("issues_url").orElse(""))
        modLicenseUrl.convention(factory.gradleProperty("license_url").orElse(""))
        modHomepageUrl.convention(factory.gradleProperty("homepage_url").orElse(modDiscordUrl).orElse(modSourcesUrl).orElse(""))

        curseforgeId.convention(factory.gradleProperty("curseforge_id").orElse(""))
        modrinthId.convention(factory.gradleProperty("modrinth_id").orElse(""))

        loader.convention("common")
        applySharedAccessTransforms.convention(loader.map { it == "common" })

        commonRunDirectory.convention(false)

        devLogin.convention(true)
        debugRuns.convention(true)
        mixinDebugRuns.convention(debugRuns)
        loaderDebugRuns.convention(false)
    }

    fun withTestmod(config: Action<TestmodConfiguration>? = null): Provider<SourceSet> = with(project) {
        val cfg = objects.newInstance(TestmodConfiguration::class, this@MultiloaderExtension)
        config?.execute(cfg)
        testmodConfig = cfg

        val javaPlugin = the(JavaPluginExtension::class)
        val testMod = javaPlugin.sourceSets.register(cfg.sourceSetName.get()) {
            compileClasspath += javaPlugin.sourceSets["main"].compileClasspath
            runtimeClasspath += javaPlugin.sourceSets["main"].runtimeClasspath
        }

        testMod.configure {
            javaPlugin.registerFeature("testmod") {
                usingSourceSet(this@configure)
            }
            dependencies {
                implementationConfigurationName(javaPlugin.sourceSets["main"].output)
            }
        }

        afterEvaluate {
            val shared = the(VersionCatalogsExtension::class).shared
            val publishing = the(PublishingExtension::class)

            testMod.configure {
                dependencies {
                    shared.findLibrary("autoservice_annotations").ifPresent { compileOnlyConfigurationName(it) }
                    shared.findLibrary("autoservice").ifPresent { annotationProcessorConfigurationName(it) }
                }

                publishing.publications {
                    withType<MavenPublication>().configureEach {
                        suppressPomMetadataWarningsFor(apiElementsConfigurationName)
                        suppressPomMetadataWarningsFor(runtimeElementsConfigurationName)
                    }
                }
            }
        }

        return testMod
    }

    fun applyMetadataReplacements(patterns: List<String>, extraProperties: Map<String, Any>? = null) {
        processResourcesProperties += patterns to extraProperties
    }

    fun setCommonProject(projectPath: String) = applyCommonProjectDependency(project, projectPath)

    fun runDir(name: String): File {
        val baseDir = if (commonRunDirectory.get()) "../common" else project.projectDir
        return File("${baseDir}/run/$name")
    }
}
