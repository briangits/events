# events

A lightweight, coroutine-native in-process event bus for Kotlin Multiplatform.

## Quick Start

Add the dependency to your project:

```kotlin
dependencies {
    implementation("io.github.briangits.events:events:<version>")
}
```

Then create an event bus, publish events, and subscribe to them:

```kotlin
val bus = Events()

val subscription = bus.subscribe<UserLoggedIn> { event ->
    println("User logged in: ${event.username}")
}

bus.publish { UserLoggedIn("janedoe") }

subscription.cancel()
```


## Usage

### Define an Event

```kotlin
data class UserLoggedIn(val username: String)
```

### Subscribe to an Event

```kotlin
val bus = Events()
val subscription = bus.subscribe<UserLoggedIn> { event ->
    println("User logged in: ${event.username}")
}
```

A `subscribe()` returns a `Job` that can be canceled when no longer needed.

```kotlin
subscription.cancel()
```


### Publish an Event

```kotlin
bus.publish { UserLoggedIn("janedoe") }
```

### Structured Concurrency

Subscriptions can be attached to an existing `CoroutineScope`
and are canceled when the scope is canceled

```kotlin
val scope = CoroutineScope(Dispatchers.Default)

val subscription = bus.subscribe<UserLoggedIn>(scope) { event ->
    println("User logged in: ${event.username}")
}

// Cancelling the scope cancels the subscription
scope.cancel()
```

## License

[Apache-2.0](LICENSE)
