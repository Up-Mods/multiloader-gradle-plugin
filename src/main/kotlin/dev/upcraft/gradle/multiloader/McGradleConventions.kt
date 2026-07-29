package dev.upcraft.gradle.multiloader

import dev.upcraft.gradle.multiloader.plugins.applyLoomMcGradleConventions
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

/**
 * See [MC Gradle Conventions](https://github.com/mcgradleconventions)
 */
fun Project.applyMcGradleConventions(loader: String) {
    buildList {
        add("apiElements")
        add("runtimeElements")

        val ml = the(MultiloaderExtension::class)
        if(ml.generateSources.get()) {
            add("sourcesElements")
        }
        if(ml.generateJavadoc.get()) {
            add("javadocElements")
        }
    }.forEach { configName ->
        configurations.named(configName).configure {
            attributes { attribute(loaderAttribute, loader) }

            // Declare capabilities on the outgoing configurations.
            // Read more about capabilities here: https://docs.gradle.org/current/userguide/component_capabilities.html#sec:declaring-additional-capabilities-for-a-local-component
            outgoing {
                capability("$group:${project.name}:$version")
                capability("$group:${rootProject.name}:$version")
            }
        }

        the(PublishingExtension::class).publications.withType<MavenPublication>().configureEach {
            suppressPomMetadataWarningsFor(configName)
        }
    }

    the(JavaPluginExtension::class).sourceSets.configureEach {
        listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach {
            configurations.named(it) {
                attributes { attribute(loaderAttribute, loader) }
            }
        }
    }

    applyLoomMcGradleConventions(this, loader, loaderAttribute)
}
