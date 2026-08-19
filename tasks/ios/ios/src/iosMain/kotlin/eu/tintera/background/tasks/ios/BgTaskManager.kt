package eu.tintera.background.tasks.ios

import eu.tintera.background.guard.ExecutionContextObserver
import eu.tintera.background.guard.PendingTokenProducer
import eu.tintera.background.tasks.TaskLifecycleObserver
import eu.tintera.background.tasks.TaskResult
import eu.tintera.background.tasks.core.AppDispatchers
import eu.tintera.background.tasks.core.ApplicationScope
import eu.tintera.background.tasks.core.CompositeTasksLogger
import eu.tintera.background.tasks.core.runningStates
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.toNSDate
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSError
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal abstract class BgTaskManager(
    scope: ApplicationScope,
    dispatchers: AppDispatchers,
    private val taskIdentifier: String,
    private val repository: BgTaskManagerRepository,
    private val appLifecycleObserver: AppLifecycleObserver,
    private val tag: String,
    private val clock: Clock,
    private val log: CompositeTasksLogger
) : PendingTokenProducer(scope), ExecutionContextObserver, TaskLifecycleObserver {
    private val _lastKnownTasks = MutableStateFlow<List<BgTaskManagerTask>>(emptyList())
    protected val lastKnownTasks: List<BgTaskManagerTask>
        get() = _lastKnownTasks.value

    override fun onCompleted(id: Uuid, result: TaskResult<Any>) {
        _lastKnownTasks.update { it.filterNot { task -> task.id == id } }
    }

    override fun onCanceled(id: Uuid, reason: String?) {
        _lastKnownTasks.update { it.filterNot { task -> task.id == id } }
    }

    init {
        register()

        scope.launch(dispatchers.default) {
            appLifecycleObserver.isBackground
                .dropWhile { it } // ignore the initial background state
                .distinctUntilChanged()
                .collectLatest { isBg ->
                    if (isBg) evaluateAndScheduleNext()
                }
        }
    }

    abstract fun List<BgTaskManagerTask>.filter(): List<BgTaskManagerTask>

    private suspend fun nextPlanedTime() = repository.tasks(runningStates.toList()).filter().let { tasks ->
        _lastKnownTasks.value = tasks
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
            log.info(tag) { "background window opened by the system at ${clock.now()}" }
            it?.also { task ->
                produce(BgTaskToken(taskIdentifier, task))
            }
        }
    }

    abstract fun createRequest(): BGTaskRequest

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun schedule(time: Instant) {

        val request = createRequest()
        request.earliestBeginDate = time.toNSDate()

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, error.ptr)

            // Without this, scheduling background windows is completely opaque: iOS routinely
            // refuses the request (identifier missing from Info.plist, too many pending requests,
            // background refresh disabled) and the application simply never wakes up.
            error.value?.also {
                log.error(tag) {
                    "BGTaskScheduler refused the request for ${time}: " +
                        "[${it.code}] ${it.localizedDescription}"
                }
            } ?: log.debug(tag) { "next background window requested for $time" }
        }
    }

    override suspend fun onPreRelease() {
        if (appLifecycleObserver.isBackground.value) evaluateAndScheduleNext()
    }

    override fun onPreCancel() {
        if (appLifecycleObserver.isBackground.value) schedule(Clock.System.now() + 1.hours)
    }
}