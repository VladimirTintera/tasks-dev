package eu.tintera.tasks.core

import eu.tintera.tasks.Data
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.data.backoffCriteriaOrDefault
import kotlin.time.Clock

interface TaskResultProcessor {
    suspend fun handleResult(task: Task, result: ExecutionResult)
}

class TaskResultProcessorImpl(
    private val repository: Repository
) : TaskResultProcessor {

    override suspend fun handleResult(task: Task, result: ExecutionResult) {
        val now = Clock.System.now()
        when (result) {
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
                        val backoff = task.backoffCriteriaOrDefault.calculate(task.runAttemptCount + 1)
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
        }
    }
}