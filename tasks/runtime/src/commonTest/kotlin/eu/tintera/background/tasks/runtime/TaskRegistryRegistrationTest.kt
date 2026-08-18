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
 * Registrace handlerů přichází z Koinu konzumenta (`taskHandlerOf` zakládá `createdAtStart`
 * singleton). `taskRegistry` je ale procesový singleton, který restart Koinu přežije — takže
 * pokud aplikace svůj Koin restartuje (typicky odhlášení / přepnutí uživatele), přijdou přesně
 * tytéž registrace podruhé.
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
    fun `stejny handler smi byt zaregistrovan znovu - restart Koinu aplikace`() = runTest {
        val registry = TaskRegistry()

        registry.register(registration())
        // Druhý průchod eager fází Koinu po jeho restartu. Nesmí shodit start aplikace.
        registry.register(registration())

        assertNotNull(registry.resolve<Unit, Unit, Unit>("dummy"))
    }

    @Test
    fun `opakovana registrace nahradi tu predchozi`() = runTest {
        val registry = TaskRegistry()
        val second = registration()

        registry.register(registration())
        registry.register(second)

        assertSame(second, registry.resolve<Unit, Unit, Unit>("dummy"))
    }

    @Test
    fun `opakovana registrace nezduplikuje zaznam v type registru`() = runTest {
        val registry = TaskRegistry()

        registry.register(registration())
        registry.register(registration())

        assertEquals(1, registry.resolve(DummyHandler::class)?.size)
    }

    @Test
    fun `dva ruzne handlery na stejnem identifikatoru jsou porad chyba`() = runTest {
        val registry = TaskRegistry()

        registry.register(registration())

        assertFailsWith<IllegalArgumentException> {
            registry.register(
                registration(handler = { OtherHandler() }, type = OtherHandler::class)
            )
        }
    }
}
