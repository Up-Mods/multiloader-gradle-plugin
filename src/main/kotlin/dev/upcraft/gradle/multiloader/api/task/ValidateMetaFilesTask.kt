package dev.upcraft.gradle.multiloader.api.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.CacheableTask

@CacheableTask
abstract class ValidateMetaFilesTask : DefaultTask() {
    companion object {
        const val TASK_NAME = "validateMetaFiles"
    }

    init {
        group = "verification"
        description = "Validate Mod Metadata Files like ATs/AWs and injected interfaces"
    }
}
