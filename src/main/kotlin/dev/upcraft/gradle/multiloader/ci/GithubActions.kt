package dev.upcraft.gradle.multiloader.ci

import dev.upcraft.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.provider.Provider

internal val MultiloaderExtension.isRunningOnGithubActions: Provider<Boolean>
    // https://docs.github.com/en/actions/reference/workflows-and-actions/variables
    get() = project.providers.environmentVariable("GITHUB_ACTIONS").map { it.toBoolean() }
