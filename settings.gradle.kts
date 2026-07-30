plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("projects") {
            from(files("gradle/projects.versions.toml"))
        }
    }
}

rootProject.name = "multiloader-gradle-plugin"

include("settings-plugin", "shared-dependencies")
