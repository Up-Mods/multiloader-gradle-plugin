package dev.upcraft.gradle.multiloader.api

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import javax.inject.Inject

abstract class TestmodConfiguration @Inject constructor(factory: ProviderFactory, ext: MultiloaderExtension) {

    abstract val modId: Property<String>
    abstract val sourceSetName: Property<String>

    init {
        modId.convention(ext.modId.map { "${it}_testmod" })
        sourceSetName.convention("testmod")
    }
}
