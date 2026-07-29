import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publishing)
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
}

java {
    toolchain {
        languageVersion = libs.versions.java.map(JavaLanguageVersion::of)
    }
}

tasks.named<Jar>("jar").configure {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})"
    )
}

gradlePlugin {
    plugins.create("multiloaderSettings") {
        id = "dev.upcraft.gradle.multiloader.settings"
        implementationClass = "dev.upcraft.gradle.multiloader.settings.MultiLoaderSettingsPlugin"
        displayName = "Minecraft Multiloader Settings Plugin"
        description = "Settings Plugin for `dev.upcraft.gradle.multiloader` plugin."
        tags.addAll("minecraft", "multiloader", "mods")
        compatibility {
            features {
                configurationCache = true
            }
        }
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

        providers.environmentVariable("MAVEN_UPLOAD_URL").orNull?.let {
            maven(it) {
                credentials {
                    username = providers.environmentVariable("MAVEN_UPLOAD_USERNAME").orNull
                    password = providers.environmentVariable("MAVEN_UPLOAD_PASSWORD").orNull
                }
            }
        }
    }
}
