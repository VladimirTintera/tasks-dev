# guard Module (Low-Level System Resource Management)

The `guard` module is a critical low-level component of the framework. It is designed as a **reference-counted multiplexer** for managing asynchronous background tasks. It ensures that the framework holds exactly one active system token (e.g., `PowerManager.WakeLock` on Android or a `UIBackgroundTaskIdentifier` session on iOS) as long as at least one client task is running. Once all tasks complete, system resources are voluntarily released. In the event of an OS-triggered cancellation (resource revocation), the guard ensures tasks are cancelled synchronously and gracefully.

---

## Architecture and Lifecycle Control Flow

The component coordinates two core abstractions:

1. **ExecutionContext (Multiplexer $\rightarrow$ Client Task)**:
   * Represents a "permit" for running asynchronous background tasks.
   * Acquired via `acquire()` and released via `release()` (typically managed via `ExecutionContext.use { ... }`).
   * Each context exposes a reactive state flow `isExpired: StateFlow<Boolean>`.
2. **Token (System $\rightarrow$ Multiplexer)**:
   * Represents a physical platform-specific system resource (e.g., `PowerManager.WakeLock`, iOS background task, observer query).
   * The multiplexer holds a single active system token for the duration of an active execution session.

### Interaction and Coordination Directions

*   **Multiplexer $\rightarrow$ Token (Voluntary Release / Success)**:
    *   When the active task count drops to `0`, the multiplexer waits for a configured debounce period (`releaseDebounce`).
    *   If no new tasks are acquired during this window, the multiplexer releases the system token by invoking its suspending `release()` function.
*   **Token $\rightarrow$ Multiplexer (Forced Cancellation / OS Revocation)**:
    *   If the OS revokes or expires the system token (e.g., iOS background execution limit reached, low battery on Android), the token triggers its pre-cancel hooks.
    *   These hooks synchronously set `isExpired` to `true` on the active multiplexer session.
    *   Running tasks checking `isExpired` or execution status are immediately cancelled via a standard `CancellationException`.

---

## Resolved Critical Concurrency & Thread-Safety Issues

During deep analysis and refactoring in June 2026, four critical concurrency vulnerabilities were successfully resolved:

### 1. Lock Contention and Deadlocks in `SharedExecutionContextProvider`
*   **Issue:** The `acquire()` function held a coroutine-based mutex lock while suspending and waiting for system token acquisition (`tokenProducer.token().first()`). If system permission allocation took longer, this blocked all subsequent calls to `acquire()`, and more critically, blocked calls to `release()` for other active tasks.
*   **Solution:** Implemented a **Deferred session initialization** pattern (`scope.async`). The mutex lock is held only briefly to register the `Deferred` handle. Waiting for token emission (`deferred.await()`) occurs **outside the mutex**, allowing other coroutines to execute `release()` concurrently.

### 2. Lost Cancellation Signals in `CompositeTokenProducer`
*   **Issue:** When multiple system tokens were revoked concurrently (e.g., the OS revoked both a Foreground Service and a WakeLock), a race condition occurred. The pre-cancel hook evaluated the size of `activeTokens` (`size == 1`), which was updated asynchronously. This caused the multiplexer to miss the cancellation of the final token, leaving tasks running without active OS permissions.
*   **Solution:** The pre-cancel hook inside `CompositeTokenProducer` now synchronously inspects the current state of all active tokens directly in the collection (`token.state.value == TokenState.CANCELLED`). If all are cancelled, it triggers cancellation of the composite token immediately and synchronously.

### 3. Race Conditions during Pre-Cancel Hook Registration in `AbstractToken`
*   **Issue:** Registering a callback via `invokeOnPreCancel` ran concurrently with token cancellation in `finishWithCancel()`. A hook could get registered right after the token completed, causing the new callback to be stored in the collection forever without ever being invoked.
*   **Solution:** Implemented thread-safe atomic collection management:
    *   `finishWithCancel()` atomically consumes and clears registered hooks (`preCancelHooks.getAndUpdate { emptyList() }`) to prevent double-execution.
    *   `invokeOnPreCancel()` atomically updates the list and checks the token's final state afterwards. If already cancelled, it removes and instantly triggers the block.

### 4. Coroutine Leak in `CompositeTokenProducer`
*   **Issue:** If a flow subscriber to `CompositeTokenProducer.token()` was cancelled before emitting the combined token (e.g., using `Flow.first()`), the internal `collectionJob` merging sub-flows leaked and kept running in the background.
*   **Solution:** Wrapped flow collection in a `try-finally` block. If early termination or cancellation occurs, the `finally` block cancels the underlying `collectionJob`.

---

## Testing & Verification

All edge cases and concurrency behaviors are covered by unit and integration tests:
*   [CompositeTokenProducerTest.kt](file:///Users/vladimirtintera/Develop/tasks-dev/guard/src/commonTest/kotlin/eu/tintera/background/guard/CompositeTokenProducerTest.kt) – Validates concurrent cancellations, lifecycle flow, and token merging.
*   [SharedExecutionContextProviderTest.kt](file:///Users/vladimirtintera/Develop/tasks-dev/guard/src/commonTest/kotlin/eu/tintera/background/guard/SharedExecutionContextProviderTest.kt) – Checks non-blocking release mechanisms, instant expirations, and load resilience.
*   [ExhaustibleTokenProducerTest.kt](file:///Users/vladimirtintera/Develop/tasks-dev/guard/src/commonTest/kotlin/eu/tintera/background/guard/ExhaustibleTokenProducerTest.kt) – Tests exhaustible OS token producers.

Run the verification suite for the `:guard` module using:
```bash
./gradlew :guard:check
```
