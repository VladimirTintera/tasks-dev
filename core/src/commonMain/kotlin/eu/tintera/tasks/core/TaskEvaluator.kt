package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.ParentData
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.tasks.core.migrations.TaskMigrator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

enum class TaskEvaluatorResult {
    SUCCESS,
    FAILURE,
    RETRY
}


interface TaskEvaluator {
    suspend fun handle(
        id: Uuid,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult
}

class TaskEvaluatorImpl(
    private val registryResolver: RegistryResolver,
    private val taskMigrator: TaskMigrator,
    private val taskScopeFactory: TaskScopeFactory,
    private val applicationScope: ApplicationScope,
    private val dispatchers: AppDispatchers,
    private val tagMapper: TagMapper,
    private val repository: TaskEvaluatorRepository,
    private val taskResultHandler: TaskResultHandler
) : TaskEvaluator {

    override suspend fun handle(
        id: Uuid,
        onForegroundInfo: suspend (ForegroundInfo) -> Boolean
    ): TaskEvaluatorResult {

        val task = repository.executableTask(id) ?: return TaskEvaluatorResult.FAILURE.also {
            println("Task $id not found")
        }

        val registration = registryResolver.resolve<Any, Any, Any>(
            identifier = task.identifier
        ) ?: return handleResult(
            TaskEvaluationResult.Failed(
                id = id,
                repeatInterval = null
            )
        ).also {
            println("No registration found for ask with id $id, identifier ${task.identifier}")
        }

        val migrationResult = taskMigrator.migrate(
            data = task,
            registration = registration
        )?.also {
            repository.upgradeData(
                id = id,
                input = it.input?.let { input ->
                    registration.inputSerializer.encodeToBytes(input)
                } ?: task.inputData,
                output = it.output?.let { output ->
                    registration.outputSerializer.encodeToBytes(output)
                } ?: task.outputData,
                progress = it.progress?.let { progress ->
                    registration.progressSerializer.encodeToBytes(progress)
                } ?: task.progressData,
                version = it.version
            )
        }

        val typedInput = migrationResult?.input ?: task.inputData?.let {
            registration.inputSerializer.decodeFromBytes(it)
        } ?: return handleResult(
            TaskEvaluationResult.Failed(
                id = id,
                repeatInterval = null
            )
        )

        val parents = repository.parentsDataFor(id).mapNotNull { parentEntity ->
            registryResolver.resolve<Any, Any, Any>(parentEntity.identifier)?.let { parentRegistration ->
                val migratedData = taskMigrator.migrate(data = parentEntity, registration = parentRegistration)
                ParentData(
                    id = parentEntity.id,
                    identifier = parentEntity.identifier,
                    data = parentEntity.outputData?.let {
                        migratedData?.output ?: parentRegistration.outputSerializer.decodeFromBytes(it)
                    },
                    finishedAt = parentEntity.finishedAt
                )
            }
        }

        val result = taskScopeFactory.createForTask(
            taskId = id,
            data = typedInput,
            retryCount = task.runAttemptCount - 1,
            parentData = parents,
            onForegroundInfoProvided = onForegroundInfo,
            progressSerializer = registration.progressSerializer,
            scope = applicationScope + dispatchers.default,
            tags = tagMapper.parse(tags = task.tags).toSet(),
            saveDispatcher = dispatchers.io
        ).use { scope ->
            try {
                with(registration.factory()) {
                    scope.run()
                }.also {
                    scope.flushProgress()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                EventBus.send(TAG, "Task execution failed with error '${e.message}'")
                TaskResult.Failure
            }
        }

        return handleResult(
            when (result) {
                TaskResult.Failure -> TaskEvaluationResult.Failed(
                    id = id,
                    repeatInterval = task.repeatInterval
                )

                TaskResult.Retry -> TaskEvaluationResult.Retry(
                    id = id,
                    backoffCriteria = task.backoffCriteria,
                    retryCount = task.runAttemptCount - 1
                )

                is TaskResult.Success -> TaskEvaluationResult.Success(
                    id = id,
                    registration = registration,
                    repeatInterval = task.repeatInterval,
                    outputData = result.outputData,
                )
            }
        )
    }

    private suspend fun handleResult(
        result: TaskEvaluationResult
    ): TaskEvaluatorResult {

        withContext(NonCancellable) {
            taskResultHandler.handleResult(
                result
            )
        }

        return when (result) {
            is TaskEvaluationResult.Failed -> TaskEvaluatorResult.FAILURE
            is TaskEvaluationResult.Success -> TaskEvaluatorResult.SUCCESS
            is TaskEvaluationResult.Retry -> TaskEvaluatorResult.RETRY
        }
    }

    companion object {
        private const val TAG = "TaskEvaluator"
    }
}

