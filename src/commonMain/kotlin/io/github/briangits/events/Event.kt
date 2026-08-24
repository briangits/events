package io.github.briangits.events

import kotlin.reflect.KClass

/**
 * Represents an event with a type and a payload.
 *
 * @property [type] The class type of the event.
 * @property [payload] The actual event data.
 */
internal data class Event<out T : Any>(
    val type: KClass<out T>,
    val payload: T
)
