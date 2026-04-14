package eu.tintera.tasks.core

import eu.tintera.guard.ExecutionContextObserver
import eu.tintera.guard.Token
import eu.tintera.guard.TokenProducer
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.State
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.toNSDate
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSError
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal abstract class BgTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    private val repository: Repository,
    private val appLifecycleObserver: AppLifecycleObserver,
    private val tag: String
) : TokenProducer, ExecutionContextObserver {
    protected val currentToken = MutableStateFlow<BGTask?>(null)

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

    abstract fun List<Task>.filter(): List<Task>

    private suspend fun nextPlanedTime() = repository.tasksByState(
        listOf(State.Enqueued, State.Blocked, State.Running)
    ).map { tasks ->
        val now = Clock.System.now()
        tasks.filter().minOfOrNull { it.processTime ?: now }
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
            EventBus.send(tag, "BGTask started")

            task?.also {
                currentToken.update { it }
            }
        }
    }

    abstract fun createRequest(): BGTaskRequest

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun schedule(time: Instant) {
        EventBus.send(
            tag, "scheduling to $time"
        )

        val request = createRequest()
        request.earliestBeginDate = time.toNSDate()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error.ptr)

            error.value?.also {
                EventBus.send(
                    tag, "BgTask schedule failed, code = ${it.code}, description = ${it.description}"
                )
            }
        }
    }

    override suspend fun onPreRelease() {
        if (appLifecycleObserver.isBackground.value) evaluateAndScheduleNext()
    }

    override fun onPreCancel() {
        if (appLifecycleObserver.isBackground.value) schedule(Clock.System.now() + 1.hours)
    }
}