package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskEvent
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProducer
import eu.tintera.tasks.log
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toNSDate
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Manages background task scheduling and execution on iOS using [BGTaskScheduler].
 *
 * This manager handles the registration, scheduling, and lifecycle of background processing tasks.
 * It integrates with the [Repository] to determine when the next background work should occur
 * and uses [EventBus] to broadcast status updates.
 */
internal class BgTaskManager(
    appPackage: String,
    private val repository: Repository,
) : TokenProducer {
    private class JobToken(
        private val job: Job
    ) : Token {
        override suspend fun release() {
            EventBus.send(TaskEvent.Custom(TAG, "releasing"))
            job.cancel()
        }

        override fun cancel() {
            EventBus.send(TaskEvent.Custom(TAG, "canceling"))
            job.cancel()
        }

        override fun toString(): String {
            return TAG
        }

        companion object {
            private const val TAG = "BgTaskManagerToken"
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // True, pokud nás iOS spustil jako velký task
    val isRunningBgTask = MutableStateFlow(false)

    // True, pokud nám iOS právě oznámil, že nám končí čas pro velký task
    val isBgTaskExpired = MutableStateFlow(false)

    override fun token(onExpire: () -> Unit): Flow<Token> = channelFlow {
        isRunningBgTask.first { it }
        val token = JobToken(
            job = scope.launch {
                isRunningBgTask.first { !it }
                if (isBgTaskExpired.value) onExpire()
            }
        )
        send(token)
    }
    private val taskIdentifier = "$appPackage.bgtask"

    init {
        register()
    }

    private suspend fun nextPlanedTime() = repository.tasksByState(
        listOf(State.Enqueued, State.Blocked, State.Running)
    ).map { tasks ->
        tasks.minOfOrNull { task ->
            task.processTime.also {
                log("unfinished task ${task.identifier}: ${task.processTime}")
            }
        }
    }.firstOrNull()?.coerceAtLeast(Clock.System.now() + 30.minutes)

    suspend fun evaluateAndScheduleNext() {
        nextPlanedTime()?.also {
            schedule(it)
        }
    }

    private fun register() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = taskIdentifier,
            usingQueue = null
        ) { task ->
            EventBus.send(
                TaskEvent.BackgroundProcessingStarted(Clock.System.now())
            )
            log("Task launched")

            task?.also {

                isRunningBgTask.value = true
                isBgTaskExpired.value = false // Reset pro nový běh

                val job = scope.launch {
                    log("Task job started")
                    var failed = false
                    var nextTime: Instant? = null

                    try {
                        val resultState = handleTask()
                        log("Task job finished")
                        nextTime = resultState.nextProcessTime
                    } catch (e: CancellationException) {
                        failed = true
                        throw e
                    } catch (_: Throwable) {
                        failed = true
                    } finally {

                        isRunningBgTask.value = false

                        withContext(NonCancellable) {
                            try {
                                withTimeout(2.seconds) {
                                    evaluateAndScheduleNext()
                                }
                            } catch (_: TimeoutCancellationException) {
                                log("EvaluateAndScheduleNext failed with timeout.")
                            }
                        }

                        EventBus.send(
                            TaskEvent.BackgroundProcessingCompleted(
                                wasSuccess = !failed,
                                nextProcessTime = nextTime
                            )
                        )

                        task.setTaskCompletedWithSuccess(!failed)
                    }
                }

                task.expirationHandler = {
                    EventBus.send(TaskEvent.BackgroundProcessingExpirationCalled(Clock.System.now()))
                    log("Task job cancelled")

                    isBgTaskExpired.value = true
                    job.cancel()
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun schedule(time: Instant) {
        log("Scheduling next processing time to ${time.toLocalDateTime(TimeZone.currentSystemDefault())}")
        EventBus.send(
            TaskEvent.BackgroundProcessingScheduling(time)
        )

        val request = BGProcessingTaskRequest(taskIdentifier)
        request.requiresExternalPower = false
        request.requiresNetworkConnectivity = true
        request.earliestBeginDate = time.toNSDate()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error.ptr)

            error.value?.also {
                EventBus.send(
                    TaskEvent.BackgroundInitializationFailed(
                        code = it.code,
                        description = it.description ?: ""
                    )
                )
            }
        }
    }

    private suspend fun handleTask(): TasksProcessingState {
        val start = Clock.System.now()
        val now = start + 5.minutes

        log(
            "handle task ${start.toLocalDateTime(TimeZone.currentSystemDefault())} - ${
                now.toLocalDateTime(
                    TimeZone.currentSystemDefault()
                )
            }"
        )

        return repository.tasksByState(
            listOf(State.Enqueued, State.Blocked, State.Running)
        ).map { tasks ->

            log(
                "processing tasks"
            )

            tasks.forEach {
                val identifier = it.identifier.substringAfterLast('.')
                log("[$identifier]: [${it.state}] ${it.processTime.toLocalDateTime(TimeZone.currentSystemDefault())}")
            }

            log("-----------------------")

            TasksProcessingState(
                finished = tasks.none { it.processTime <= now },
                nextProcessTime = tasks.filter {
                    it.processTime > now
                }.minOfOrNull {
                    it.processTime
                }
            ).also {
                log("state: $it")
            }
        }.first { it.finished }
    }

    suspend fun pendingTasks(): List<PendingIosTask> = suspendCancellableCoroutine { continuation ->

        BGTaskScheduler.sharedScheduler.getPendingTaskRequestsWithCompletionHandler { requests ->

            val pendingRequests = requests ?: emptyList<Any>()

            val mappedTasks = pendingRequests.mapNotNull { item ->
                val request = item as? BGTaskRequest ?: return@mapNotNull null

                PendingIosTask(
                    identifier = request.identifier,
                    earliestBeginTime = request.earliestBeginDate?.toKotlinInstant()
                )
            }

            continuation.resume(mappedTasks)
        }
    }

    companion object {
        private const val TAG = "BgTaskManager"
    }
}