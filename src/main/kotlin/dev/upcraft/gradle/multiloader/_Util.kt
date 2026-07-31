package dev.upcraft.gradle.multiloader

import dev.upcraft.gradle.multiloader.api.MultiloaderExtension

internal fun MultiloaderExtension.getTestModId(): String {
    return testmodConfig?.modId?.get() ?: error("Cannot retrieve testmod ID, no testmod configured!")
}

internal fun MultiloaderExtension.hasTestMod(): Boolean = testmodConfig != null
