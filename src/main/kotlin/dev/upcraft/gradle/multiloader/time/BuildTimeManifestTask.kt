package dev.upcraft.gradle.multiloader.time

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.util.jar.Attributes
import java.util.jar.Manifest
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@CacheableTask
abstract class BuildTimeManifestTask : DefaultTask() {

    @get:Input
    abstract val buildTime: Property<Instant>

    @get:OutputFile
    abstract val manifestPath: RegularFileProperty

    init {
        manifestPath.convention(project.layout.buildDirectory.file("tmp/timestamp.MF"))
    }

    @TaskAction
    fun run() {
        val mf = Manifest()
        mf.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        val timestamp = buildTime.get()
        mf.mainAttributes[Attributes.Name("Implementation-Timestamp")] = timestamp.toString()
        mf.mainAttributes[Attributes.Name("Timestamp")] = timestamp.toEpochMilliseconds().toString()

        manifestPath.get().asFile.outputStream().use { mf.write(it) }
    }
}
