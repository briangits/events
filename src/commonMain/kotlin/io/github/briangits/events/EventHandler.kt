package io.github.briangits.events

/**
 * A handler for events of type [T].
 */
typealias EventHandler<T> = suspend (event: T) -> Unit
