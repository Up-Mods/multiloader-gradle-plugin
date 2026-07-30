import org.gradle.plugin.compatibility.compatibility

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
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
    website = "https://mods.upcraft.dev"
    vcsUrl = "https://github.com/Up-Mods/multiloader-gradle-plugin.git"

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

publishing {
    repositories {
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
