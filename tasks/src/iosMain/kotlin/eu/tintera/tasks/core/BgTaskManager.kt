package eu.tintera.tasks.core

import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskEvent
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.locks.ExecutionContextObserver
import eu.tintera.tasks.core.locks.Token
import eu.tintera.tasks.core.locks.TokenProducer
import eu.tintera.tasks.log
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toNSDate
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
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
    private val appLifecycleObserver: AppLifecycleObserver
) : TokenProducer, ExecutionContextObserver {
    private val currentToken = MutableStateFlow<BGTask?>(null)

    init {
        register()
    }

    override fun token(
        onExpire: () -> Unit
    ): Flow<Token> = currentToken.filterNotNull().map { task ->

        task.expirationHandler = onExpire

        object : Token {
            override suspend fun release() {
                currentToken.update { null }
                try {
                    withTimeoutOrNull(2.seconds) {
                        evaluateAndScheduleNext()
                    }
                }
                finally {
                    task.setTaskCompletedWithSuccess(true)
                }
            }

            override fun cancel() {
                currentToken.update { null }
                try {
                    schedule(Clock.System.now() + 1.hours)
                }
                finally {
                    task.setTaskCompletedWithSuccess(false)
                }
            }
        }
    }

    private val taskIdentifier = "$appPackage.bgtask"

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

            task?.also {
                currentToken.update { it }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun schedule(time: Instant) {
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

    override suspend fun onPreRelease() {
        if (appLifecycleObserver.isBackground.value)
            evaluateAndScheduleNext()
    }

    override fun onPreCancel() {
        if (appLifecycleObserver.isBackground.value)
            schedule(Clock.System.now() + 1.hours)
    }
}