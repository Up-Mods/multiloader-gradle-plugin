@file:Suppress("UnstableApiUsage")

package dev.upcraft.gradle.multiloader

import dev.upcraft.gradle.multiloader.api.MultiloaderExtension
import dev.upcraft.gradle.multiloader.plugins.ModDevGradle
import dev.upcraft.gradle.multiloader.plugins.applyMDGCommonProject
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources

fun applyCommonProjectDependency(current: Project, commonProject: String) = with(current) {
    // this needs to be declared early because loom resolves the compileOnly configuration
    dependencies {
        "compileOnly"(project(commonProject)) {
            attributes { attribute(loaderAttribute, "common") }
        }

        pluginManager.withPlugin(ModDevGradle.PLUGIN_ID) {
            applyMDGCommonProject(current, commonProject)
        }
    }

    afterEvaluate {
        val commonJavaDep = configurations.dependencyScope("commonJavaDep")
        val commonJava = configurations.resolvable("commonJava") { extendsFrom(commonJavaDep) }

        val commonResourcesDep = configurations.dependencyScope("commonResourcesDep")
        val commonResources = configurations.resolvable("commonResources") { extendsFrom(commonResourcesDep) }

        dependencies {
            commonJavaDep(project(commonProject, "commonJava"))
            commonResourcesDep(project(commonProject, "commonResources"))
        }

        tasks.named<JavaCompile>("compileJava").configure {
            dependsOn(commonJava)
            source(commonJava)
        }

        tasks.named<ProcessResources>("processResources").configure {
            dependsOn(commonResources)
            from(commonResources)
        }

        tasks.named<Javadoc>("javadoc").configure {
            dependsOn(commonJava)
            source(commonJava)
        }

        tasks.named<Jar>("sourcesJar").configure {
            dependsOn(commonJava, commonResources)
            from(commonJava, commonResources)
        }

        the(MultiloaderExtension::class).testmodConfig?.let {
            val testmodCommonJavaDep = configurations.dependencyScope("testmodCommonJavaDep")
            val testmodCommonJava = configurations.resolvable("testmodCommonJava") { extendsFrom(testmodCommonJavaDep) }
            val testmodCommonResourcesDep = configurations.dependencyScope("testmodCommonResourcesDep")
            val testmodCommonResources = configurations.resolvable("testmodCommonResources") { extendsFrom(testmodCommonResourcesDep) }

            dependencies {
                "testmodCompileOnly"(project(commonProject)) {
                    attributes { attribute(loaderAttribute, "common") }
                    capabilities { requireFeature("testmod") }
                }

                testmodCommonJavaDep(project(commonProject, "testmodCommonJava"))
                testmodCommonResourcesDep(project(commonProject, "testmodCommonResources"))
            }

            tasks.named<JavaCompile>("compileTestmodJava").configure {
                dependsOn(testmodCommonJava)
                source(testmodCommonJava)
            }

            tasks.named<ProcessResources>("processTestmodResources").configure {
                dependsOn(testmodCommonResources)
                from(testmodCommonResources)
            }
        }
    }
}

