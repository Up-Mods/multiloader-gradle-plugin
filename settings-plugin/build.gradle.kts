plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.kotlin.jvm)
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    gradlePluginPortal()
}

tasks.named<Jar>("jar").configure {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})"
    )
}

gradlePlugin {
    val multiloaderSettings = plugins.create("multiloaderSettings") {
        id = "dev.upcraft.gradle.multiloader.settings"
        implementationClass = "dev.upcraft.gradle.multiloader.settings.MultiLoaderSettingsPlugin"
    }
}

catalog {
    versionCatalog {
        from(rootProject.files("gradle/projects.versions.toml"))
    }
}
components.named<AdhocComponentWithVariants>("java").configure {
    addVariantsFromConfiguration(configurations["versionCatalogElements"]) { }
}

val buildRepo = layout.buildDirectory.dir("repo").map { it.asFile.toURI() }

publishing {
    repositories {
        maven(buildRepo.get()) {
            name = "buildDir"
        }
    }
}
