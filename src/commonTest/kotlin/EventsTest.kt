import io.github.briangits.events.Events
import kotlinx.coroutines.CompletableDeferred
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

data class TestEvent(
    val message: String
)

data class OtherEvent(
    val value: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventsTest {

    @Test
    fun `subscribers receive published event`() = runTest {
        val bus = Events()
        val received = CompletableDeferred<TestEvent>()

        val subscription = bus.subscribe<TestEvent> {
            received.complete(it)
        }

        try {
            val event = TestEvent("Hello, PayNX!")

            bus.publish(event)

            assertEquals(event, received.await())
        } finally {
            subscription.cancel()
        }
    }

    @Test
    fun `subscription ignores unrelated event types`() = runTest {
        val bus = Events()
        var received = false

        val subscription = bus.subscribe<TestEvent> {
            received = true
        }

        try {
            bus.publish(OtherEvent(42))
            runCurrent()

            assertFalse(received)
        } finally {
            subscription.cancel()
        }
    }

    @Test
    fun `multiple conscurrent subscriptions`() = runTest {
        val bus = Events()
        val first = CompletableDeferred<TestEvent>()
        val second = CompletableDeferred<TestEvent>()

        val subscription1 = bus.subscribe<TestEvent> {
            first.complete(it)
        }

        val subscription2 = bus.subscribe<TestEvent> {
            second.complete(it)
        }

        try {
            val event = TestEvent("Hello")

            bus.publish(event)

            assertEquals(event, first.await())
            assertEquals(event, second.await())
        } finally {
            subscription1.cancel()
            subscription2.cancel()
        }
    }

    @Test
    fun `continuous event publication`() = runTest {
        val bus = Events()
        val received = mutableListOf<TestEvent>()

        val subscription = bus.subscribe<TestEvent> {
            received += it
        }

        try {
            val events = listOf(
                TestEvent("one"),
                TestEvent("two"),
                TestEvent("three")
            )

            events.forEach { bus.publish(it) }
            runCurrent()

            assertEquals(events, received)
        } finally {
            subscription.cancel()
        }
    }

    @Test
    fun `independent subscriptions`() = runTest {
        val bus = Events()
        val testEvents = mutableListOf<TestEvent>()
        val otherEvents = mutableListOf<OtherEvent>()

        val testSubscription = bus.subscribe<TestEvent> {
            testEvents += it
        }

        val otherSubscription = bus.subscribe<OtherEvent> {
            otherEvents += it
        }

        try {
            val testEvent = TestEvent("test")
            val otherEvent = OtherEvent(42)

            bus.publish(testEvent)
            bus.publish(otherEvent)
            runCurrent()

            assertEquals(listOf(testEvent), testEvents)
            assertEquals(listOf(otherEvent), otherEvents)
        } finally {
            testSubscription.cancel()
            otherSubscription.cancel()
        }
    }

    @Test
    fun `subscription cancellation`() = runTest {
        val bus = Events()
        val received = mutableListOf<TestEvent>()

        val subscription = bus.subscribe<TestEvent> {
            received += it
        }

        bus.publish(TestEvent("before"))
        runCurrent()

        subscription.cancel()

        bus.publish(TestEvent("after"))
        runCurrent()

        assertEquals(
            listOf(TestEvent("before")),
            received
        )
    }

    @Test
    fun `subscription cancellation does not affect another`() = runTest {
        val bus = Events()
        val first = mutableListOf<TestEvent>()
        val second = mutableListOf<TestEvent>()

        val subscription1 = bus.subscribe<TestEvent> {
            first += it
        }

        val subscription2 = bus.subscribe<TestEvent> {
            second += it
        }

        try {
            subscription1.cancel()

            val event = TestEvent("Hello")
            bus.publish(event)
            runCurrent()

            assertTrue(first.isEmpty())
            assertEquals(listOf(event), second)
        } finally {
            subscription2.cancel()
        }
    }

    @Test
    fun `cancelling parent coroutine cancels subscription when no scope is provided`() = runTest {
        val bus = Events()
        val received = mutableListOf<TestEvent>()

        val parent = launch(start = CoroutineStart.UNDISPATCHED) {
            bus.subscribe<TestEvent> {
                received += it
            }

            awaitCancellation()
        }

        bus.publish(TestEvent("before"))
        runCurrent()

        parent.cancel()
        runCurrent()

        bus.publish(TestEvent("after"))
        runCurrent()

        assertEquals(
            listOf(TestEvent("before")),
            received
        )
    }

    @Test
    fun `provided scope controls subscription lifecycle`() = runTest {
        val bus = Events()
        val received = mutableListOf<TestEvent>()
        val scope = CoroutineScope(Job())

        bus.subscribe<TestEvent>(scope = scope) {
            received += it
        }

        try {
            bus.publish(TestEvent("before"))
            runCurrent()

            scope.cancel()

            bus.publish(TestEvent("after"))
            runCurrent()

            assertEquals(
                listOf(TestEvent("before")),
                received
            )
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `publication order is preserved`() = runTest {
        val bus = Events()
        val received = mutableListOf<TestEvent>()

        val subscription = bus.subscribe<TestEvent> {
            received += it
        }

        try {
            val events = (1..100)
                .map { TestEvent(it.toString()) }

            events.forEach { bus.publish(it) }
            runCurrent()

            assertEquals(events, received)
        } finally {
            subscription.cancel()
        }
    }

    @Test
    fun `concurrent publications deliver all events`() = runTest {
        val bus = Events()
        val received = mutableListOf<TestEvent>()

        val subscription = bus.subscribe<TestEvent> {
            received += it
        }

        try {
            coroutineScope {
                (1..100)
                    .map { index ->
                        launch {
                            bus.publish(TestEvent(index.toString()))
                        }
                    }
                    .joinAll()
            }

            advanceUntilIdle()

            assertEquals(100, received.size)
            assertEquals(
                (1..100).map(Int::toString).toSet(),
                received.map(TestEvent::message).toSet()
            )
        } finally {
            subscription.cancel()
        }
    }

    @Test
    fun `subscribe returns active job`() = runTest {
        val bus = Events()

        val subscription = bus.subscribe<TestEvent> {}

        assertTrue(subscription.isActive)

        subscription.cancel()

        assertTrue(subscription.isCancelled)
    }

    @Test
    fun `subscription job cancellation`() = runTest {
        val bus = Events()

        val subscription = bus.subscribe<TestEvent> {}

        subscription.cancel()

        assertFalse(subscription.isActive)
        assertTrue(subscription.isCancelled)
    }
}
