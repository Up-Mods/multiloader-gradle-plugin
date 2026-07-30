plugins {
    `maven-publish`
    `version-catalog`
}

group = rootProject.group
version = rootProject.version

catalog {
    versionCatalog {
        from(rootProject.files("gradle/projects.versions.toml"))
    }
}

val buildRepo = layout.buildDirectory.dir("repo").map { it.asFile.toURI() }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
        }
    }

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
