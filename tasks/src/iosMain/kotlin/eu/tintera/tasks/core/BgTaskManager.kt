package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.Token
import eu.tintera.guard.TokenProducer
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskEvent
import eu.tintera.tasks.core.data.Repository
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.toKotlinInstant
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
import kotlin.time.Instant

internal class BgTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    private val repository: Repository,
    private val appLifecycleObserver: AppLifecycleObserver
) : TokenProducer, ExecutionContextObserver {
    private val currentToken = MutableStateFlow<BGTask?>(null)

    init {
        register()

        scope.launch(dispatchers.default) {
            appLifecycleObserver.isBackground
                .dropWhile { it } // Ignoruj počáteční background
                .distinctUntilChanged()
                .collectLatest { isBg ->
                    if (isBg) evaluateAndScheduleNext()
                }
        }
    }

    override fun token(
        onExpire: () -> Unit
    ): Flow<Token> = currentToken.filterNotNull().map { task ->

        task.expirationHandler = onExpire

        object : Token {
            override suspend fun release() {
                currentToken.update { null }
                task.setTaskCompletedWithSuccess(true)
            }

            override fun cancel() {
                currentToken.update { null }
                task.setTaskCompletedWithSuccess(false)
            }
        }
    }

    private suspend fun nextPlanedTime() = repository.tasksByState(
        listOf(State.Enqueued, State.Blocked, State.Running)
    ).map { tasks ->
        tasks.minOfOrNull { it.processTime }
    }.firstOrNull()?.coerceAtLeast(Clock.System.now() + 30.minutes)

    suspend fun evaluateAndScheduleNext() {
        EventBus.send(TAG, "calling evaluateAndScheduleNext")
        nextPlanedTime()?.also {
            schedule(it)
        }
    }

    private fun register() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = taskIdentifier,
            usingQueue = null
        ) { task ->
            EventBus.send(TAG, "BgProcessingTask started")

            task?.also {
                currentToken.update { it }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun schedule(time: Instant) {
        EventBus.send(
            TAG, "scheduling BgProcessingTask to $time"
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
        if (appLifecycleObserver.isBackground.value) evaluateAndScheduleNext()
    }

    override fun onPreCancel() {
        if (appLifecycleObserver.isBackground.value) schedule(Clock.System.now() + 1.hours)
    }

    companion object {
        private const val TAG = "BgTaskManager"
    }
}