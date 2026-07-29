package dev.upcraft.gradle.multiloader

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the

val VersionCatalogsExtension.shared
    get() = named("multiloaderSharedDependencies")

fun applySharedDependencies(target: Project) = with(target) {
    val shared = the<VersionCatalogsExtension>().shared
    dependencies {
        shared.findLibrary("autoservice_annotations").ifPresent { "compileOnly"(it) }
        shared.findLibrary("autoservice").ifPresent { "annotationProcessor"(it) }

        shared.findLibrary("jspecify").ifPresent { "compileOnly"(it) }
        shared.findLibrary("jetbrains_annotations").ifPresent { "compileOnly"(it) }

        shared.findLibrary("junit_api").ifPresent { "testImplementation"(it) }
        shared.findLibrary("junit_launcher").ifPresent { "testRuntimeOnly"(it) }
        shared.findLibrary("junit_engine").ifPresent { "testRuntimeOnly"(it) }
    }
}
