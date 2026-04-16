package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.TaskHandler
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.TaskScope
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.migrations.findMigrationPath
import eu.tintera.tasks.core.seriaization.SerializationEngine
import eu.tintera.tasks.migrations.FieldMigrator
import kotlinx.serialization.KSerializer
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

interface TaskEvaluator {
    suspend fun handle(
        task: Task,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray>
}

@Suppress("UNCHECKED_CAST")
internal class TaskEvaluatorImpl(
    private val taskRegistry: TaskRegistry,
    private val repository: Repository,
    private val serializationEngine: SerializationEngine
) : TaskEvaluator {
    override suspend fun handle(
        task: Task,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskResult<ByteArray> {

        val registration = taskRegistry.resolve(task.identifier) ?: return TaskResult.failure()

        val migrationsToRun = registration.migrations.findMigrationPath(
            startVersion = task.version,
            targetVersion = registration.currentVersion,
        )

        val taskData = migrationsToRun.fold(
            TaskData(
                inputBytes = task.inputData,
                outputBytes = task.outputData,
                progressBytes = task.progressData,
                parsedInput = null,
                version = task.version
            )
        ) { data, migration ->

            val nextInput = data.inputBytes?.let { bytes ->
                migration.inputMigrator?.apply(bytes) ?: (bytes to data.parsedInput)
            }

            TaskData(
                inputBytes = nextInput?.first,
                parsedInput = nextInput?.second,

                outputBytes = data.outputBytes?.let {
                    migration.outputMigrator?.apply(it)?.first ?: it
                },
                progressBytes = data.progressBytes?.let {
                    migration.progressMigrator?.apply(it)?.first ?: it
                },
                version = migration.endVersion
            )
        }

        if (migrationsToRun.isNotEmpty()) repository.upgradeData(
            id = task.id,
            input = taskData.inputBytes,
            output = taskData.outputBytes,
            progress = taskData.progressBytes,
            version = taskData.version
        )

        val typedInput = taskData.parsedInput ?: taskData.inputBytes?.toTypedData(
            serializationEngine = serializationEngine,
            serializer = registration.inputSerializer
        )

        val scope = object : TaskScope<Any?, Any?> {
            override val taskId: Uuid = task.id
            override val data: Any? = typedInput
            override val retryCount: Int = task.runAttemptCount - 1

            override suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean {
                return onForegroundInfo(foregroundInfo)
            }

            override suspend fun setProgress(data: Any?) {
                // Serializujeme typový progress na raw Data a uložíme do DB
                val rawProgress = data?.let {
                    serializationEngine.encodeToBytes(data, registration.progressSerializer as KSerializer<Any>)
                }

                repository.updateProgressData(taskId, rawProgress)
            }
        }


        val handler = registration.factory() as TaskHandler<Any?, Any?, Any?>

        val typedResult = try {
            with(handler) {
                with(scope) {
                    run()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            EventBus.send(TAG, "Task execution failed with error '${e.message}'")
            return TaskResult.Failure
        }

        return when (typedResult) {
            is TaskResult.Success -> {
                val rawOutput = serializationEngine.encodeToBytes(
                    value = typedResult.outputData,
                    serializer = registration.outputSerializer as KSerializer<Any?>
                )
                TaskResult.Success(rawOutput)
            }

            TaskResult.Failure -> TaskResult.Failure
            TaskResult.Retry -> TaskResult.Retry
        }
    }


    private fun <From, To> FieldMigrator<From, To>.apply(bytes: ByteArray): Pair<ByteArray, To> {
        val newObj = migrationBlock(serializationEngine.decodeFromBytes(bytes, fromSerializer))
        val newBytes = serializationEngine.encodeToBytes(newObj, toSerializer)

        return Pair(newBytes, newObj)
    }


    private class TaskData(
        val inputBytes: ByteArray?,
        val outputBytes: ByteArray?,
        val progressBytes: ByteArray?,
        val parsedInput: Any? = null, // TADY SI ULOŽÍME HOTOVÝ OBJEKT!
        val version: Int
    )

    companion object {
        private const val TAG = "TaskEvaluator"
    }
}

