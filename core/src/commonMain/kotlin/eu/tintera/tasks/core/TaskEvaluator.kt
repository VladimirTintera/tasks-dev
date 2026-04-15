package eu.tintera.tasks.core

import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class TaskEvaluator(
    private val taskRegistry: TaskRegistry,
    private val repository: Repository
) {
    suspend fun handle(
        taskIdentifier: String,
        taskId: Uuid,
        runAttemptCount: Int,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray> {
        val registration = taskRegistry.resolve(taskIdentifier) ?: return TaskResult.failure()

        val typedInput = deserializeData(byteArrayOf(), registration.inputSerializer)

        val scope = object : TaskScope<Any?, Any?> {
            override val taskId: Uuid = taskId
            override val data: Any? = typedInput
            override val retryCount: Int = runAttemptCount

            override suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean {
                return onForegroundInfo(foregroundInfo)
            }

            override suspend fun setProgress(data: Any?) {
                // Serializujeme typový progress na raw Data a uložíme do DB
                val rawProgress = data?.let {
                    serializeData(data, registration.progressSerializer as KSerializer<Any>)
                } ?: byteArrayOf()

                repository.updateProgressData(taskId, rawProgress)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val handler = registration.factory() as TaskHandler<Any?, Any?, Any?>

        val typedResult = try {
            with(handler) {
                with(scope) {
                    run()
                }
            }
        } catch (e: Exception) {
            // Ochrana před pádem samotného byznys kódu uživatele
            return TaskResult.Failure
        }

        return when (typedResult) {
            is TaskResult.Success -> {
                val rawOutput = serializeData(typedResult.outputData, registration.outputSerializer)
                TaskResult.Success(rawOutput)
            }
            TaskResult.Failure -> TaskResult.Failure
            TaskResult.Retry -> TaskResult.Retry
        }
    }

    private fun <T> deserializeData(data: ByteArray, serializer: KSerializer<T>): T {
        return Json.decodeFromString(serializer, data.decodeToString())
    }

    private fun <T> serializeData(value: T, serializer: KSerializer<T>): ByteArray {
        return Json.encodeToString(serializer, value).encodeToByteArray()
    }
}