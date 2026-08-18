package eu.tintera.background.tasks.runtime

import eu.tintera.background.tasks.SimpleTaskHandler
import eu.tintera.background.tasks.TaskRegistration
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Handler registrations arrive from the consumer's Koin (`taskHandlerOf` creates a `createdAtStart`
 * singleton), but `taskRegistry` is a process-wide singleton that outlives a Koin restart. When an
 * application restarts its Koin — typically on sign-out or a user switch — the very same
 * registrations arrive a second time.
 */
class TaskRegistryRegistrationTest {

    private class DummyHandler : SimpleTaskHandler {
        override suspend fun run() = TaskResult.success(Unit)
    }

    private class OtherHandler : SimpleTaskHandler {
        override suspend fun run() = TaskResult.success(Unit)
    }

    private object UnitSerializer : Serializer<Unit> {
        override fun encodeToBytes(value: Unit) = ByteArray(0)
        override fun decodeFromBytes(bytes: ByteArray) = Unit
    }

    private fun registration(
        identifier: String = "dummy",
        handler: () -> SimpleTaskHandler = { DummyHandler() },
        type: kotlin.reflect.KClass<out SimpleTaskHandler> = DummyHandler::class,
    ) = TaskRegistration(
        type = type,
        identifier = identifier,
        currentVersion = 1,
        factory = handler,
        inputSerializer = UnitSerializer,
        outputSerializer = UnitSerializer,
        progressSerializer = UnitSerializer,
        migrations = emptyList(),
    )

    @Test
    fun `the same handler may be registered again - application Koin restart`() = runTest {
        val registry = TaskRegistry()

        registry.register(registration())
        // A second pass through Koin's eager phase after a restart. Must not bring the app down.
        registry.register(registration())

        assertNotNull(registry.resolve<Unit, Unit, Unit>("dummy"))
    }

    @Test
    fun `re-registration replaces the previous one`() = runTest {
        val registry = TaskRegistry()
        val second = registration()

        registry.register(registration())
        registry.register(second)

        assertSame(second, registry.resolve<Unit, Unit, Unit>("dummy"))
    }

    @Test
    fun `re-registration does not duplicate the type registry entry`() = runTest {
        val registry = TaskRegistry()

        registry.register(registration())
        registry.register(registration())

        assertEquals(1, registry.resolve(DummyHandler::class)?.size)
    }

    @Test
    fun `two different handlers on one identifier are still an error`() = runTest {
        val registry = TaskRegistry()

        registry.register(registration())

        assertFailsWith<IllegalArgumentException> {
            registry.register(
                registration(handler = { OtherHandler() }, type = OtherHandler::class)
            )
        }
    }
}
