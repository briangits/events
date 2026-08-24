package io.github.briangits.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class Events {
    private val events = MutableSharedFlow<Event<Any>>()

    suspend fun <T : Any> subscribe(
        type: KClass<T>,
        scope: CoroutineScope? = null,
        handler: EventHandler<T>
    ): Job {
        val subscriptionScope = scope ?: CoroutineScope(currentCoroutineContext())

        return subscriptionScope.launch(start = CoroutineStart.UNDISPATCHED) {
            @Suppress("UNCHECKED_CAST")
            events.filter { it.type == type }.collect { handler(it.payload as T) }
        }
    }

    suspend inline fun <reified T : Any> subscribe(
        scope: CoroutineScope? = null,
        noinline handler: EventHandler<T>
    ): Job = subscribe(T::class, scope, handler)

    suspend fun <T : Any> publish(event: T) =
        events.emit(Event(event::class, event))

    suspend fun <T : Any> publish(block: () -> T) =
        publish(block())
}
