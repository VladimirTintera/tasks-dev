package eu.tintera.tasks.android

import android.annotation.SuppressLint
import androidx.work.*
import eu.tintera.tasks.*
import eu.tintera.tasks.BackoffPolicy
import eu.tintera.tasks.Constraints
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.data.*
import eu.tintera.tasks.core.migrations.TaskMigrator
import eu.tintera.tasks.serialization.Serializer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class WorkManagerTaskManager(
    private val workManager: WorkManager,
    private val repository: Repository,
    private val taskRegistry: RegistryResolver,
    private val taskMigrator: TaskMigrator,
    private val tagMapper: TagMapper,
    private val transactionRunner: TransactionRunner
) : TaskManager {

    private suspend fun TaskRequest<*>.serializeTags() = tagMapper.serialize(
        tags = tags
    )

    private suspend fun <T : Any> TaskRequest<T>.oneTimeWorkRequest() =
        OneTimeWorkRequestBuilder<TaskWorker>().apply {
            set(
                identifier = identifier,
                initialDelay = initialDelay,
                constraints = constraints,
                tags = serializeTags() + identifier,
                backoffCriteria = backoffCriteria,
                keepResultsForAtLeast = keepResultsForAtLeast
            )
        }.build()


    override suspend fun <T : Any> enqueueUniqueTask(
        task: TaskRequest<T>,
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
                repeatInterval = null,
                parentIds = emptySet()
            )
            workManager.enqueueUniqueWork(
                uniqueName,
                existingTaskPolicy.toWorkPolicy(),
                request
            ).await()

            id
        }
    }

    override suspend fun <T : Any> enqueueTask(
        task: TaskRequest<T>,
    ): Uuid {
        val request = task.oneTimeWorkRequest()
        val id = request.id.toKotlinUuid()
        return withContext(NonCancellable) {
            saveTask(
                id = id,
                task = task,
                uniqueName = "",
                repeatInterval = null,
                parentIds = emptySet()
            )
            workManager.enqueue(request).await()
            id
        }
    }

    @SuppressLint("EnqueueWork")
    override suspend fun enqueueContinuation(continuation: TaskContinuation) {

        val roots = continuation.tasks.map { it to it.oneTimeWorkRequest() }
        val rootIds = roots.map { it.second.id.toKotlinUuid() }.toSet()
        val result = withContext(NonCancellable) {

            transactionRunner {

                roots.forEach { (task, work) ->
                    saveTask(
                        id = work.id.toKotlinUuid(),
                        task = task,
                        uniqueName = "",
                        repeatInterval = null,
                        parentIds = emptySet() // Kořeny nemají rodiče
                    )
                }

                workManager.beginWith(
                    roots.map { (_, work) -> work }
                ).appendAndSave(
                    taskContinuation = continuation.next,
                    parentIds = rootIds // KOŘENY SE STÁVAJÍ RODIČI PRO NEXT!
                )
            }
        }

        result.enqueue().await()
    }

    @SuppressLint("EnqueueWork")
    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) {
        val roots = continuation.tasks.map { it to it.oneTimeWorkRequest() }
        val rootIds = roots.map { it.second.id.toKotlinUuid() }.toSet()

        withContext(NonCancellable) {

            val result = transactionRunner {
                roots.forEach { (task, work) ->
                    saveTask(
                        id = work.id.toKotlinUuid(),
                        task = task,
                        uniqueName = uniqueName,
                        repeatInterval = null,
                        parentIds = emptySet() // Kořeny nemají rodiče
                    )
                }

                workManager.beginUniqueWork(
                    uniqueName,
                    existingTaskPolicy.toWorkPolicy(),
                    roots.map { (_, work) -> work }
                ).appendAndSave(
                    taskContinuation = continuation.next,
                    parentIds = rootIds // KOŘENY SE STÁVAJÍ RODIČI PRO NEXT!
                )
            }

            result.enqueue().await()
        }
    }

    @SuppressLint("EnqueueWork")
    private suspend fun WorkContinuation.appendAndSave(
        taskContinuation: TaskContinuation?,
        parentIds: Set<Uuid> // TADY PŘIJÍMÁME ID RODIČŮ Z PŘEDCHOZÍHO KROKU
    ): WorkContinuation {
        if (taskContinuation == null || taskContinuation.tasks.isEmpty()) {
            return this
        }

        val nextRequests = taskContinuation.tasks.map { it to it.oneTimeWorkRequest() }

        // Získáme IDčka aktuální vrstvy (to budou rodiče pro další krok)
        val currentLevelIds = nextRequests.map { it.second.id.toKotlinUuid() }.toSet()

        nextRequests.forEach { (task, request) ->
            saveTask(
                id = request.id.toKotlinUuid(),
                task = task,
                uniqueName = "",
                repeatInterval = null,
                parentIds = parentIds // PŘIŘADÍME IDČKA RODIČŮ DO DATABÁZE!
            )
        }

        return then(nextRequests.map { it.second })
            .appendAndSave(taskContinuation.next, parentIds = currentLevelIds)
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

    override suspend fun <T : Any> enqueuePeriodicUniqueTask(
        task: TaskRequest<T>,
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
                tags = task.serializeTags() + uniqueName + task.identifier,
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
                repeatInterval = repeatInterval,
                parentIds = emptySet()
            )

            workManager.enqueueUniquePeriodicWork(
                uniqueName,
                when (existingTaskPolicy) {
                    ExistingPeriodicTaskPolicy.Keep -> ExistingPeriodicWorkPolicy.KEEP
                    ExistingPeriodicTaskPolicy.Replace -> ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
                },
                request
            ).await()

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
        val inputData = Data.Builder().putString(TaskWorker.TASK_IDENTIFIER, identifier).build()
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
    override fun taskInfosByTag(
        tag: String
    ) = workManager.getWorkInfosByTagFlow(tag).toTaskInfos()

    override fun taskInfoById(
        id: Uuid
    ) = combine(
        repository.taskInfoById(id),
        workManager.getWorkInfoByIdFlow(id.toJavaUuid())
    ) { task, workInfo ->
        workInfo?.toTaskInfo(
            info = task,
            registration = task?.identifier?.let {
                taskRegistry.resolve(it)
            }
        )
    }

    override suspend fun cancelTaskById(id: Uuid) {
        withContext(NonCancellable) {
            repository.updateTerminatingState(
                id = id,
                state = State.Cancelled,
                finishedAt = Clock.System.now(),
                outputData = null
            )
            workManager.cancelWorkById(id.toJavaUuid()).await()
        }
    }

    override suspend fun cancelTasksByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag).await()
    }

    override fun taskInfos(
        query: TaskInfoQuery
    ) = query.takeIf { !it.isEmpty() }?.let {
        flow {
            val tags = tagMapper.serialize(query.tags)

            workManager.getWorkInfosFlow(
                WorkQuery.Builder
                    .fromTags(tags.toList())
                    .addIds(query.ids.map { it.toJavaUuid() })
                    .addStates(query.states.map { it.toWorkState() })
                    .addUniqueWorkNames(query.uniqueNames.toList()).build()
            ).map {
                println(it)
                it
            }.toTaskInfos().collect { emit(it) }
        }
    } ?: emptyFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<List<WorkInfo>>.toTaskInfos() = channelFlow {

        val sharedWorkInfosFlow = stateIn(
            scope = this,
            started = SharingStarted.Eagerly, // Můžeme Eagerly, protože hned pod tím to konzumujeme
            initialValue = emptyList()
        )

        val dbTasksFlow = sharedWorkInfosFlow
            .map { list -> list.map { it.id.toKotlinUuid() }.toSet() }
            .distinctUntilChanged()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    repository.taskInfoByIds(ids)
                }
            }

        combine(sharedWorkInfosFlow, dbTasksFlow) { workInfos, dbTasks ->
            val taskMap = dbTasks.associateBy { it.id }.mapValues { (_, task) ->
                task to taskRegistry.resolve<Any, Any, Any>(task.identifier)
            }

            workInfos.map { workInfo ->
                val pair = taskMap[workInfo.id.toKotlinUuid()]
                workInfo.toTaskInfo(
                    info = pair?.first,
                    registration = pair?.second,
                )
            }
        }.collect {
            send(it)
        }
    }

    private suspend fun WorkInfo.toTaskInfo(
        info: Info?,
        registration: TaskRegistration<Any, Any, Any>?
    ): TaskInfo {

        val migrationResult = info?.let { task ->
            registration?.let { registration ->
                taskMigrator.migrate(
                    data = ExecutableTask(
                        identifier = task.identifier,
                        runAttemptCount = task.runAttemptCount,
                        version = task.version,
                        inputData = task.inputData,
                        outputData = task.outputData,
                        progressData = task.progressData,
                        tags = tags
                    ),
                    registration = registration
                )
            }
        }

        val parsedTags = tagMapper.parse(
            tags.filterNot { tag ->
                tag == TaskWorker::class.java.name
            }
        )

        return TaskInfo(
            id = id.toKotlinUuid(),
            state = state.toState(),
            tags = parsedTags,
            runAttemptCount = runAttemptCount,
            outputData = info?.outputData?.let {
                migrationResult?.output ?: registration?.outputSerializer?.decodeFromBytesOrNull(it)
            },
            nextScheduledTime = Instant.fromEpochMilliseconds(nextScheduleTimeMillis).takeIf {
                it < Instant.DISTANT_FUTURE
            },
            progress = info?.progressData?.takeIf {
                state != WorkInfo.State.FAILED && state != WorkInfo.State.CANCELLED
            }?.let {
                migrationResult?.progress ?: registration?.progressSerializer?.decodeFromBytesOrNull(it)
            },
            finishedAt = info?.finishedAt,
            createdAt = info?.createdAt ?: Instant.DISTANT_PAST,
            identifier = info?.identifier ?: ""
        )
    }

    private suspend fun <T : Any> saveTask(
        id: Uuid,
        task: TaskRequest<T>,
        uniqueName: String,
        repeatInterval: Duration?,
        parentIds: Set<Uuid>
    ) {
        val registration = taskRegistry.resolve<T, Any, Any>(task.identifier) ?: error("Task registry not found")
        val t = Task(
            id = id,
            identifier = task.identifier,
            uniqueName = uniqueName,
            runAttemptCount = 0,
            state = State.Enqueued,
            processTime = Clock.System.now(),
            inputData = registration.inputSerializer.encodeToBytes(task.data),
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

        repository.insert(task = t, tags = emptySet(), parentIds = parentIds)
    }
}

private fun <T> Serializer<T>.decodeFromBytesOrNull(data: ByteArray) = try {
    decodeFromBytes(data)
} catch (_: Exception) {
    null
}