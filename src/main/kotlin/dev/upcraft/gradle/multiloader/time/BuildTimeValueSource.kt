package dev.upcraft.gradle.multiloader.time

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
abstract class BuildTimeValueSource : ValueSource<Instant, ValueSourceParameters.None> {

    override fun obtain(): Instant? = Clock.System.now()
}
