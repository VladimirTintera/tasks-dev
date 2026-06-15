package eu.tintera.background.tasks.core.fakes

import eu.tintera.background.tasks.*
import eu.tintera.background.tasks.core.RegistryResolver
import eu.tintera.background.tasks.core.TagRegistration
import eu.tintera.background.tasks.serialization.Serializer
import kotlinx.coroutines.delay
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes

class FakeRegistryResolver : RegistryResolver {
    private val registrations = mutableMapOf<String, TaskRegistration<Any, Any, Any>>()

    @Suppress("UNCHECKED_CAST")
    fun register(identifier: String, handler: suspend () -> TaskResult<Any>) {
        registrations[identifier] = TaskRegistration(
            type = TaskHandler::class as KClass<out TaskHandler<Any, Any, Any>>,
            identifier = identifier,
            currentVersion = 1,
            factory = {
                TaskHandler {
                    handler()
                }
            },
            inputSerializer = FakeUnitSerializer as Serializer<Any>,
            outputSerializer = FakeUnitSerializer as Serializer<Any>,
            progressSerializer = FakeUnitSerializer as Serializer<Any>,
            migrations = emptyList()
        )
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <I : Any, O : Any, P : Any> resolve(identifier: String): TaskRegistration<I, O, P>? {
        return registrations[identifier] as TaskRegistration<I, O, P>?
    }

    override suspend fun <T : TaskHandler<I, O, P>, I : Any, O : Any, P : Any> resolve(type: KClass<out T>): List<TaskRegistration<I, O, P>>? {
        return null
    }

    override suspend fun <T : Tag> resolveTag(identifier: String): TagRegistration<T>? {
        return null
    }

    override suspend fun <T : Tag> resolveTag(type: KClass<out T>): TagRegistration<T>? {
        return null
    }
}

fun fakeTaskRegistry() = FakeRegistryResolver().apply {
    register("fakeTask") {
        delay(10.minutes)
        TaskResult.Success(Unit)
    }
}

private object FakeUnitSerializer : Serializer<Unit> {
    override fun encodeToBytes(value: Unit): ByteArray = ByteArray(0)
    override fun decodeFromBytes(bytes: ByteArray): Unit = Unit
}