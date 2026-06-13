package eu.tintera.background.tasks.ios

import eu.tintera.background.guard.AbstractToken
import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.guard.PendingTokenProducer
import eu.tintera.background.guard.Token
import eu.tintera.background.tasks.EventBus
import eu.tintera.background.tasks.core.AppDispatchers
import eu.tintera.background.tasks.core.ApplicationScope
import eu.tintera.background.tasks.core.runningStates
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
    private val repository: BgTaskManagerRepository,
    private val appLifecycleObserver: AppLifecycleObserver,
    private val tag: String,
    private val clock: Clock
) : PendingTokenProducer(scope), ExecutionContextObserver {
    protected var lastKnownTasks: List<BgTaskManagerTask> = emptyList()
        private set

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

    abstract fun List<BgTaskManagerTask>.filter(): List<BgTaskManagerTask>

    private suspend fun nextPlanedTime() = repository.tasks(runningStates.toList()).filter().let { tasks ->
        lastKnownTasks = tasks
        tasks.minOfOrNull { it.processTime }?.coerceAtLeast(clock.now() + 30.minutes)
    }

    suspend fun evaluateAndScheduleNext() {
        nextPlanedTime()?.also {
            schedule(it)
        }
    }

    private fun register() {
        BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
            identifier = taskIdentifier,
            usingQueue = null
        ) {
            EventBus.send(tag, "BGTask started")

            it?.also { task ->
                produce(BgTaskToken(taskIdentifier, task))
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

internal class BgTaskToken(
    identifier: String,
    private val task: BGTask
) : AbstractToken() {

    override val tag = "BgTask:$identifier"

    init {
        task.expirationHandler = {
            finishWithCancel()
        }
    }

    override suspend fun onRelease() {
        task.setTaskCompletedWithSuccess(true)
    }

    override fun onCancel() {
        task.setTaskCompletedWithSuccess(false)
    }

    fun cancel() {
        finishWithCancel()
    }
}