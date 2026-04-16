package eu.tintera.tasks.core

import android.annotation.SuppressLint
import androidx.work.*
import eu.tintera.tasks.*
import eu.tintera.tasks.BackoffPolicy
import eu.tintera.tasks.Constraints
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.seriaization.SerializationEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class WorkManagerCoreTaskManager(
    private val workManager: WorkManager,
    private val repository: Repository,
    private val serializationEngine: SerializationEngine,
    private val taskRegistry: TaskRegistry
) : CoreTaskManager {

    private fun TaskRequest<*>.oneTimeWorkRequest() =
        OneTimeWorkRequestBuilder<TaskWorker>().apply {
            set(
                identifier = identifier,
                initialDelay = initialDelay,
                constraints = constraints,
                tags = tags + identifier,
                backoffCriteria = backoffCriteria,
                keepResultsForAtLeast = keepResultsForAtLeast
            )
        }.build()


    override suspend fun enqueueUniqueTask(
        task: TaskRequest<*>,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ): Uuid {

        if (existingTaskPolicy == ExistingTaskPolicy.Keep) {
            val existingId = findExistingId(uniqueName)
            if (existingId != null) {
                return existingId
            }
        }

        val request = task.oneTimeWorkRequest()
        val id = request.id.toKotlinUuid()

        return withContext(NonCancellable) {
            saveTask(
                id = id,
                task = task,
                uniqueName = uniqueName,
                repeatInterval = null
            )
            workManager.enqueueUniqueWork(
                uniqueName,
                existingTaskPolicy.toWorkPolicy(),
                request
            ).await()

            id
        }
    }

    override suspend fun enqueueTask(
        task: TaskRequest<*>,
    ): Uuid {
        val request = task.oneTimeWorkRequest()
        val id = request.id.toKotlinUuid()
        return withContext(NonCancellable) {
            saveTask(
                id = id,
                task = task,
                uniqueName = "",
                repeatInterval = null
            )
            workManager.enqueue(request).await()
            id
        }
    }

    override suspend fun enqueueContinuation(continuation: TaskContinuation) {
        val roots = continuation.tasks.map { it to it.oneTimeWorkRequest() }

        withContext(NonCancellable) {

            roots.forEach { (task, work) ->
                saveTask(
                    id = work.id.toKotlinUuid(),
                    task = task,
                    uniqueName = "",
                    repeatInterval = null
                )
            }

            workManager.beginWith(
                roots.map { (_, work) -> work }
            ).appendAndSave(continuation.next).enqueue().await()
        }
    }

    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) {
        val roots = continuation.tasks.map { it to it.oneTimeWorkRequest() }
        withContext(NonCancellable) {

            roots.forEach { (task, work) ->
                saveTask(
                    id = work.id.toKotlinUuid(),
                    task = task,
                    uniqueName = uniqueName,
                    repeatInterval = null
                )
            }

            workManager.beginUniqueWork(
                uniqueName,
                existingTaskPolicy.toWorkPolicy(),
                roots.map { (_, work) -> work }
            ).appendAndSave(continuation.next).enqueue().await()
        }
    }

    @SuppressLint("EnqueueWork")
    private suspend fun WorkContinuation.appendAndSave(
        taskContinuation: TaskContinuation?,
    ): WorkContinuation {
        if (taskContinuation == null || taskContinuation.tasks.isEmpty()) {
            return this
        }

        val nextRequests = taskContinuation.tasks.map { it to it.oneTimeWorkRequest() }

        nextRequests.forEach { (task, request) ->
            saveTask(
                id = request.id.toKotlinUuid(),
                task = task,
                uniqueName = "",
                repeatInterval = null
            )
        }

        return then(nextRequests.map { it.second }).appendAndSave(taskContinuation.next)
    }

    private suspend fun findExistingId(
        uniqueName: String
    ): Uuid? {
        val existingInfos = workManager.getWorkInfosForUniqueWorkFlow(uniqueName).first()
        val uncompletedExisting = existingInfos.firstOrNull {
            it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.BLOCKED
        }

        return uncompletedExisting?.id?.toKotlinUuid()
    }

    override suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest<*>,
        repeatInterval: Duration,
        uniqueName: String,
        existingTaskPolicy: ExistingPeriodicTaskPolicy,
    ): Uuid {

        if (existingTaskPolicy == ExistingPeriodicTaskPolicy.Keep) {
            val existingId = findExistingId(uniqueName)
            if (existingId != null) {
                return existingId
            }
        }

        val request = PeriodicWorkRequestBuilder<TaskWorker>(
            repeatInterval.coerceAtLeast(MINIMAL_REPEAT_INTERVAL).inWholeMilliseconds,
            TimeUnit.MILLISECONDS
        ).apply {
            set(
                identifier = task.identifier,
                initialDelay = task.initialDelay,
                constraints = task.constraints,
                tags = task.tags + uniqueName + task.identifier,
                backoffCriteria = task.backoffCriteria,
                keepResultsForAtLeast = task.keepResultsForAtLeast
            )
        }.build()


        val id = request.id.toKotlinUuid()

        return withContext(NonCancellable) {

            saveTask(
                id = id,
                task = task,
                uniqueName = uniqueName,
                repeatInterval = repeatInterval
            )

            workManager.enqueueUniquePeriodicWork(
                uniqueName,
                when (existingTaskPolicy) {
                    ExistingPeriodicTaskPolicy.Keep -> ExistingPeriodicWorkPolicy.KEEP
                    ExistingPeriodicTaskPolicy.Replace -> ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                },
                request
            ).await()

            // Pro Replace, Append, nebo když Keep nic neblokoval, vracíme ID nového requestu
            id
        }
    }

    private fun <B : WorkRequest.Builder<B, *>, W : WorkRequest> WorkRequest.Builder<B, W>.set(
        identifier: String,
        initialDelay: Duration,

        constraints: Constraints,
        tags: Set<String>,
        backoffCriteria: BackoffCriteria?,
        keepResultsForAtLeast: Duration
    ) {
        val inputData = androidx.work.Data.Builder().putString(TaskWorker.TASK_IDENTIFIER, identifier).build()
        setInputData(inputData)
        if (constraints.requiresNetwork || constraints.requiresDeviceIdle)
            setConstraints(
                androidx.work.Constraints(
                    requiredNetworkType = NetworkType.CONNECTED,
                    requiresDeviceIdle = constraints.requiresDeviceIdle
                )
            )

        if (initialDelay.isPositive())
            setInitialDelay(initialDelay.inWholeMilliseconds, TimeUnit.MILLISECONDS)

        backoffCriteria?.also {
            setBackoffCriteria(
                backoffPolicy = when (backoffCriteria.backoffPolicy) {
                    BackoffPolicy.Linear -> androidx.work.BackoffPolicy.LINEAR
                    BackoffPolicy.Exponential -> androidx.work.BackoffPolicy.EXPONENTIAL
                },
                backoffDelay = backoffCriteria.delay.inWholeMilliseconds,
                timeUnit = TimeUnit.MILLISECONDS,
            )
        }

        keepResultsForAtLeast(keepResultsForAtLeast.toJavaDuration())

        addTag(identifier)
        tags.forEach {
            addTag(it)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun taskInfosByTag(tag: String) = channelFlow {
        val sharedWorkInfosFlow = workManager.getWorkInfosByTagFlow(tag)
            .stateIn(
                scope = this,
                started = SharingStarted.Eagerly, // Můžeme Eagerly, protože hned pod tím to konzumujeme
                initialValue = emptyList()
            )

        // 2. Pomalý stream (reaguje jen na změnu ID)
        val dbTasksFlow = sharedWorkInfosFlow
            .map { list -> list.map { it.id.toKotlinUuid() }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    repository.tasksByIds(ids)
                }
            }

        // 3. Spojíme je a pošleme ven z channelFlow
        combine(sharedWorkInfosFlow, dbTasksFlow) { workInfos, dbTasks ->
            val taskMap = dbTasks.associateBy { it.id }

            workInfos.map { workInfo ->
                workInfo.toTaskInfo(taskMap[workInfo.id.toKotlinUuid()])
            }
        }.collect {
            // Všechno, co vyjde z combine, přepošleme do výstupu našeho channelFlow
            send(it)
        }
    }

    override fun taskInfoById(
        id: Uuid
    ) = combine(
        repository.taskById(id),
        workManager.getWorkInfoByIdFlow(id.toJavaUuid())
    ) { task, workInfo ->
        workInfo?.toTaskInfo(task?.task)
    }

    override suspend fun cancelTaskById(id: Uuid) {
        workManager.cancelWorkById(id.toJavaUuid()).await()
    }

    override suspend fun cancelTasksByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag).await()
    }

    private fun WorkInfo.toTaskInfo(task: Task?): TaskInfo {
        return TaskInfo(
            id = id.toKotlinUuid(),
            state = when (state) {
                WorkInfo.State.BLOCKED -> State.Blocked
                WorkInfo.State.ENQUEUED -> State.Enqueued
                WorkInfo.State.RUNNING -> State.Running
                WorkInfo.State.SUCCEEDED -> State.Succeeded
                WorkInfo.State.CANCELLED -> State.Cancelled
                WorkInfo.State.FAILED -> State.Failed
            },
            tags = tags.filterNot { tag ->
                tag == TaskWorker::class.java.name
            }.toSet(),
            runAttemptCount = runAttemptCount,
            outputData = outputData.toData(),
            nextScheduledTime = Instant.fromEpochMilliseconds(nextScheduleTimeMillis).takeIf {
                it < Instant.DISTANT_FUTURE
            },
            progress = progress.toData(),
            finishedAt = task?.finishedAt,
            createdAt = task?.createdAt ?: Instant.DISTANT_PAST
        )
    }

    private suspend fun saveTask(
        id: Uuid,
        task: TaskRequest<*>,
        uniqueName: String,
        repeatInterval: Duration?
    ) {
        val registration = taskRegistry.resolve(task.identifier) ?: return

        val t = Task(
            id = id,
            identifier = task.identifier,
            uniqueName = uniqueName,
            runAttemptCount = 0,
            state = State.Enqueued,
            processTime = Clock.System.now(),
            inputData = task.data?.let { data ->
                serializationEngine.encodeToBytes(data, registration.inputSerializer as KSerializer<Any>)
            },
            outputData = null,
            networkRequired = task.constraints.requiresNetwork,
            createdAt = Clock.System.now(),
            finishedAt = null,
            repeatInterval = repeatInterval,
            initialDelay = task.initialDelay,
            backoffCriteria = task.backoffCriteria ?: BackoffCriteria.DEFAULT,
            progressData = null,
            retentionDelay = task.keepResultsForAtLeast,
            requiresDeviceIdle = task.constraints.requiresDeviceIdle,
            version = registration.currentVersion
        )

        repository.insert(task = t, tags = emptySet(), parentIds = emptySet())
    }
}