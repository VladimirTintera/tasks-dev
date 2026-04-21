package eu.tintera.tasks.core

import eu.tintera.tasks.BackoffCriteria
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.Repository
import kotlin.time.Clock

interface TaskResultProcessor {
    suspend fun handleResult(task: eu.tintera.tasks.core.data.TaskProcessResult)
}

class TaskResultProcessorImpl(
    private val repository: Repository
) : TaskResultProcessor {

    override suspend fun handleResult(task: eu.tintera.tasks.core.data.TaskProcessResult) {
        val now = Clock.System.now()
        when (val result = task.executionResult) {
            is ExecutionResult.Finished -> {
                when (val taskResult = result.result) {
                    TaskResult.Failure -> {
                        val duration = task.repeatInterval
                        if (duration != null) {
                            repository.updateNextRun(
                                id = task.id,
                                state = State.Enqueued,
                                processTime = now + duration,
                                progressData = null,
                                runAttemptCount = 0
                            )
                        } else {
                            repository.updateTerminatingState(
                                id = task.id,
                                state = State.Failed,
                                finishedAt = now,
                                outputData = null
                            )
                        }
                    }

                    is TaskResult.Success -> {
                        val duration = task.repeatInterval

                        if (duration != null) {
                            repository.updateNextRun(
                                id = task.id,
                                state = State.Enqueued,
                                processTime = now + duration,
                                progressData = null,
                                runAttemptCount = 0
                            )
                        } else {
                            repository.updateTerminatingState(
                                id = task.id,
                                state = State.Succeeded,
                                finishedAt = now,
                                outputData = taskResult.outputData
                            )
                        }
                    }

                    TaskResult.Retry -> {
                        val backoff = (task.backoffCriteria ?: BackoffCriteria.DEFAULT).calculate(task.retryCount)
                        repository.updateNextRun(
                            id = task.id,
                            state = State.Enqueued,
                            processTime = now + backoff,
                            progressData = null,
                            runAttemptCount = null
                        )
                    }
                }
            }

            ExecutionResult.Yielded -> {
                repository.updateNextRun(
                    id = task.id,
                    state = State.Enqueued,
                    processTime = now,
                    progressData = null,
                    runAttemptCount = null
                )
            }

            ExecutionResult.Canceled -> repository.updateTerminatingState(
                id = task.id,
                state = State.Cancelled,
                finishedAt = now,
                outputData = null
            )
        }
    }
}