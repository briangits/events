package io.github.briangits.events

import kotlin.reflect.KClass

internal data class Event<out T : Any>(
    val type: KClass<out T>,
    val payload: T
)
