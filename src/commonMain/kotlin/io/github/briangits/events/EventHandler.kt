package io.github.briangits.events

typealias EventHandler<T> = suspend (event: T) -> Unit
