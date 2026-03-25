package eu.tintera.tasks.core

import eu.tintera.tasks.*
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.data.backoffCriteriaOrDefault
import eu.tintera.tasks.core.locks.ExecutionContextProvider
import eu.tintera.tasks.core.locks.invoke
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal interface TaskProcessor {
    suspend fun run(task: Task)
}

internal class TaskProcessorImpl(
    private val repository: Repository,
    private val taskEvaluator: TaskEvaluator,
    private val networkState: NetworkState,
    private val executionContextProvider: ExecutionContextProvider,
    private val taskScopeFactory: TaskScopeFactory,
    config: TaskProcessorConfig = TaskProcessorConfig()
) : TaskProcessor {

    private val concurrencySemaphore = Semaphore(config.maxConcurrentTasks)

    override suspend fun run(task: Task) = coroutineScope {

        val actualTask = MutableStateFlow<Task?>(task)

        val workflowJob = launch {
            val taskParents = waitForPreconditions(task, actualTask) ?: return@launch
            val latestTaskSnapshot = actualTask.value ?: return@launch

            concurrencySemaphore.withPermit {
                executeTask(latestTaskSnapshot, taskParents)
            }
        }

        // 2. ZDE BĚŽÍ GLOBÁLNÍ HLÍDAČ (Paralelně vedle pracovníka)
        val observeJob = launch {
            repository.task(task.id).onEach {
                actualTask.update { it }
            }.first { t ->
                when {
                    t == null -> true
                    t.retriesCount != task.retriesCount -> true
                    t.processTime != task.processTime -> true
                    t.state.terminal() -> true
                    else -> false
                }
            }

            // ... tak nekompromisně ZABIJE CELÝ PRACOVNÍ PROCES!
            workflowJob.cancelAndJoin()
        }

        workflowJob.join()
        observeJob.cancel()
    }

    private suspend fun waitForPreconditions(
        task: Task,
        actualTask: StateFlow<Task?>
    ): List<Task>? {
        val timeFlow = flow {
            waitForProcessTime(task.processTime)
            emit(Unit)
        }

        val parentsFlow = repository.parentsFor(task.id).onEach { parents ->
            actualTask.value?.also { t ->
                if (t.state != State.Blocked && parents.any { !it.state.terminal() }) {
                    updateState(task.id, State.Blocked)
                }
            }
        }.filter { parents ->
            parents.isEmpty() || parents.all { it.state.terminal() }
        }

        val taskParents = combine(timeFlow, parentsFlow) { _, parentList -> parentList }.first()

        if (taskParents.any { it.state == State.Failed }) {
            val result = TaskResult.failure()
            withContext(NonCancellable) {
                EventBus.send(TaskEvent.TaskFinished(task.identifier, result))
                handleTaskResult(task, result)
            }
            return null // Konec, nepokračujeme
        }

        updateState(task.id, State.Enqueued)

        if (task.retriesCount == 0 && task.initialDelay.isPositive()) {
            delay(task.initialDelay)
        }

        if (actualTask.value?.networkRequired == true) {
            networkState.state().filter { it == NetworkState.State.Connected }.first()
        }

        if (actualTask.value?.state?.terminal() != false) {
            return null // Konec, nepokračujeme
        }

        return taskParents // Vše připraveno, vracíme data pro další fázi
    }

    private suspend fun executeTask(
        task: Task,
        taskParents: List<Task>
    ) = executionContextProvider {

        updateState(task.id, State.Running)

        val taskResult = try {
            val taskData = task.inputData + taskParents.sortedByDescending {
                it.finishedAt
            }.map { it.outputData }.sum()

            with(taskEvaluator) {
                EventBus.send(TaskEvent.TaskStarted(task.identifier, taskData))
                with(
                    taskScopeFactory.createScope(
                        taskId = task.id,
                        data = taskData,
                        retriesCount = task.retriesCount
                    )
                ) {
                    handle(taskIdentifier = task.identifier) ?: TaskResult.failure()
                }
            }
        } catch (e: CancellationException) {
            // when canceled, do nothing. Invalid Running states are handled by sweep mechanism
            // Let it crash
            throw e
        } catch (e: Throwable) {
            EventBus.send(TaskEvent.TaskFailed(task.identifier, "Task failed", e))
            TaskResult.failure()
        }

        withContext(NonCancellable) {
            EventBus.send(TaskEvent.TaskFinished(task.identifier, taskResult))
            handleTaskResult(task, taskResult)
        }
    }


    private suspend fun updateState(
        id: Uuid,
        state: State,
    ) = repository.updateState(
        id = id,
        state = state,
        allowedSourceStates = state.allowedSourceStatesForChangeTo().toSet()
    )

    private suspend fun handleTaskResult(task: Task, result: TaskResult) {
        val now = Clock.System.now()
        when (result) {
            TaskResult.Failure -> {
                val duration = task.repeatInterval
                if (duration != null) {
                    repository.updateRetry(
                        id = task.id,
                        retriesCount = 0,
                        state = State.Enqueued,
                        processTime = now + duration
                    )
                } else {
                    repository.updateTerminatingState(
                        id = task.id,
                        state = State.Failed,
                        finishedAt = now,
                        outputData = Data.EMPTY
                    )
                }
            }

            is TaskResult.Success -> {
                val duration = task.repeatInterval

                if (duration != null) {
                    repository.updateRetry(
                        id = task.id,
                        retriesCount = 0,
                        state = State.Enqueued,
                        processTime = now + duration
                    )
                } else {
                    repository.updateTerminatingState(
                        id = task.id,
                        state = State.Succeeded,
                        finishedAt = now,
                        outputData = result.outputData
                    )
                }
            }

            TaskResult.Retry -> {
                val backoff = task.backoffCriteriaOrDefault.calculate(task.retriesCount)
                repository.updateRetry(
                    id = task.id,
                    retriesCount = task.retriesCount + 1,
                    state = State.Enqueued,
                    processTime = now + backoff
                )
            }
        }
    }

    private suspend fun waitForProcessTime(time: Instant) {
        val now = Clock.System.now()
        val diff = time - now
        if (diff.isPositive())
            delay(diff.inWholeMilliseconds)
    }

    companion object {
        private const val TAG = "TaskProcessor"
    }
}