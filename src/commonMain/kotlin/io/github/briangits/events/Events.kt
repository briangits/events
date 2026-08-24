package io.github.briangits.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * An in-process event bus..
 *
 * ```
 * data class UserCreated(id: String, name: String)
 *
 * val bus = Events()
 **
 * bus.subscribe<UserCreated> { event ->
 *     println("User created: ${event.id} - ${event.name}")
 *}
 *
 * bus.publish(UserCreated("123", "John Doe"))
 * ```
 */
class Events {
    private val events = MutableSharedFlow<Event<Any>>()

    /**
     * Subscribes to events of a specific [type].
     *
     * @param type The class type of the events to subscribe to.
     * @param scope The [CoroutineScope] in which the subscription will run. If null, uses the current coroutine context.
     * @param handler The suspending function to be called when an event of the specified type is received.
     * @return A [Job] representing the subscription. Cancel this job to unsubscribe.
     */
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

    /**
     * Subscribes to events of a specific type [T].
     *
     * Example:
     * ```
     * data class UserCreated(id: String, name: String)
     *
     * val bus = Events()
     *
     * bus.subscribe<UserCreated> { event ->
     *      println("User created: ${event.id} - ${event.name}")
     * }
     * ```
     *
     * @param [T] The event type to subscribe to.
     * @param scope The [CoroutineScope] in which the subscription will run.
     *   If null, uses the current coroutine context.
     * @param handler The suspending function to be called when an event of type [T] is received.
     * @return A [Job] representing the subscription. Cancel this job to unsubscribe.
     */
    suspend inline fun <reified T : Any> subscribe(
        scope: CoroutineScope? = null,
        noinline handler: EventHandler<T>
    ): Job = subscribe(T::class, scope, handler)

    /**
     * Publishes an event to the bus.
     *
     * @param event The event object to publish.
     */
    suspend fun <T : Any> publish(event: T) =
        events.emit(Event(event::class, event))

    /**
     * Publishes an event produced by a [block].
     *
     * Example:
     * ```
     * data class UserCreated(id: String, name: String)
     *
     * val bus = Events()
     **
     * bus.publish { UserCreated("123", "John Doe") }
     * ```
     *
     * @param block A function producing the event to be published.
     */
    suspend fun <T : Any> publish(block: () -> T) =
        publish(block())
}
