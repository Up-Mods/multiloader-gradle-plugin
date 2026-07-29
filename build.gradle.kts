plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.kotlin.jvm)
}

group = "dev.upcraft.gradle.multiloader"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.neoforged.net/releases") {
        name = "NeoForge"
    }
    maven("https://maven.fabricmc.net") {
        name = "FabricMC"
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(project(":multiloader-gradle-settings-plugin"))
    implementation(libs.idea.ext)
    compileOnly(libs.moddevgradle)
    compileOnly(libs.fabric.loom)
}

tasks.named<Jar>("jar").configure {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})"
    )
}

gradlePlugin {
    val multiloader = plugins.create("multiloader") {
        id = "dev.upcraft.gradle.multiloader"
        implementationClass = "dev.upcraft.gradle.multiloader.MultiloaderPlugin"
    }
}

val buildRepo = project(":multiloader-gradle-settings-plugin").layout.buildDirectory.dir("repo").map { it.asFile.toURI() }

testing {
    suites {
        val test = named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
            useKotlinTest(libs.versions.kotlin)
        }

        val functionalTest = register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter(libs.versions.junit)
            useKotlinTest(libs.versions.kotlin)

            dependencies {
                implementation(project())
                implementation(libs.junit.parameterized.tests)
            }

            targets {
                all {
                    // This test suite should run after the built-in test suite has run its tests
                    testTask.configure {
                        shouldRunAfter(test)
                        dependsOn(project(":multiloader-gradle-settings-plugin").tasks.named("publishAllPublicationsToBuildDirRepository"))
                        systemProperty("dev.upcraft.multiloader.test.build_repo", buildRepo.get())
                        systemProperty("dev.upcraft.multiloader.shared_dependencies_version", version.toString())
                    }
                }
            }
        }
    }
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named<Task>("check").configure {
    // Include functionalTest as part of the check lifecycle
    dependsOn(testing.suites.named("functionalTest"))
}
