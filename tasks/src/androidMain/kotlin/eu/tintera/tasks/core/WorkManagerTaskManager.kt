package eu.tintera.tasks.core

import androidx.work.*
import eu.tintera.tasks.*
import eu.tintera.tasks.BackoffPolicy
import eu.tintera.tasks.Constraints
import eu.tintera.tasks.Data
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

internal class WorkManagerCoreTaskManager(
    private val workManager: WorkManager,
) : CoreTaskManager {

    private fun TaskRequest.oneTimeWorkRequest() =
        OneTimeWorkRequestBuilder<TaskWorker>().apply {
            set(
                handler = handler,
                initialDelay = initialDelay,
                data = data,
                constraints = constraints,
                tags= tags + handler.fullName,
                backoffCriteria = backoffCriteria,
                keepResultsForAtLeast = keepResultsForAtLeast
            )
        }.build()


    override suspend fun enqueueUniqueTask(
        task: TaskRequest,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ): Uuid {
        val existingInfos = workManager.getWorkInfosForUniqueWorkFlow(uniqueName).first()

        val request = task.oneTimeWorkRequest()

        workManager.enqueueUniqueWork(
            uniqueName,
            existingTaskPolicy.toWorkPolicy(),
            request
        ).await()

        if (existingTaskPolicy == ExistingTaskPolicy.Keep) {
            // Pokud je policy Keep, podíváme se, jestli existoval nějaký nedokončený task
            val uncompletedExisting = existingInfos.firstOrNull {
                it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.RUNNING ||
                        it.state == WorkInfo.State.BLOCKED
            }

            // Pokud ano, WorkManager náš nový request zahodil a my vracíme ID toho starého
            if (uncompletedExisting != null) {
                return uncompletedExisting.id.toKotlinUuid()
            }
        }

        // Pro Replace, Append, nebo když Keep nic neblokoval, vracíme ID nového requestu
        return request.id.toKotlinUuid()
    }

    override suspend fun enqueueTask(
        task: TaskRequest,
    ): Uuid {
        val request = task.oneTimeWorkRequest()
        workManager.enqueue(request).await()
        return request.id.toKotlinUuid()
    }

    override suspend fun enqueueContinuation(continuation: TaskContinuation) {
        workManager.beginWith(
            continuation.tasks.map { it.oneTimeWorkRequest() }
        ).append(continuation.next).enqueue().await()
    }

    override suspend fun enqueueUniqueContinuation(
        continuation: TaskContinuation,
        uniqueName: String,
        existingTaskPolicy: ExistingTaskPolicy,
    ) {
        workManager.beginUniqueWork(
            uniqueName,
            existingTaskPolicy.toWorkPolicy(),
            continuation.tasks.map { it.oneTimeWorkRequest() }
        ).append(continuation.next).enqueue().await()
    }

    private fun WorkContinuation.append(
        taskContinuation: TaskContinuation?,
    ): WorkContinuation = when {
        taskContinuation == null -> this
        taskContinuation.tasks.isEmpty() -> this
        else -> then(taskContinuation.tasks.map { it.oneTimeWorkRequest() }).append(taskContinuation.next)
    }

    override suspend fun enqueuePeriodicUniqueTask(
        task: TaskRequest,
        repeatInterval: Duration,
        uniqueName: String,
        existingTaskPolicy: ExistingPeriodicTaskPolicy,
    ): Uuid {

        val existingInfos = workManager.getWorkInfosForUniqueWorkFlow(uniqueName).first()

        val request = PeriodicWorkRequestBuilder<TaskWorker>(
            repeatInterval.coerceAtLeast(MINIMAL_REPEAT_INTERVAL).inWholeMilliseconds,
            TimeUnit.MILLISECONDS
        ).apply {
            set(
                handler = task.handler,
                initialDelay = task.initialDelay,
                data = task.data,
                constraints = task.constraints,
                tags = task.tags + uniqueName + task.handler.fullName,
                backoffCriteria = task.backoffCriteria,
                keepResultsForAtLeast = task.keepResultsForAtLeast
            )
        }.build()

        workManager.enqueueUniquePeriodicWork(
            uniqueName,
            when (existingTaskPolicy) {
                ExistingPeriodicTaskPolicy.Keep -> ExistingPeriodicWorkPolicy.KEEP
                ExistingPeriodicTaskPolicy.Replace -> ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            },
            request
        ).await()

        // 3. Rozhodneme, jaké ID vrátit
        if (existingTaskPolicy == ExistingPeriodicTaskPolicy.Keep) {
            // Pokud je policy Keep, podíváme se, jestli existoval nějaký nedokončený task
            val uncompletedExisting = existingInfos.firstOrNull {
                it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.RUNNING ||
                        it.state == WorkInfo.State.BLOCKED
            }

            // Pokud ano, WorkManager náš nový request zahodil a my vracíme ID toho starého
            if (uncompletedExisting != null) {
                return uncompletedExisting.id.toKotlinUuid()
            }
        }

        // Pro Replace, Append, nebo když Keep nic neblokoval, vracíme ID nového requestu
        return request.id.toKotlinUuid()
    }

    private fun <B : WorkRequest.Builder<B, *>, W : WorkRequest> WorkRequest.Builder<B, W>.set(
        handler: KClass<out TaskHandler>,
        initialDelay: Duration,
        data: Data,
        constraints: Constraints,
        tags: Set<String>,
        backoffCriteria: BackoffCriteria?,
        keepResultsForAtLeast: Duration
    ) {
        val inputData =
            androidx.work.Data.Builder().putString(TaskWorker.TASK_IDENTIFIER, handler.fullName)
                .putAll(data.map).build()
        setInputData(inputData)
        if (constraints.requiresNetwork)
            setConstraints(androidx.work.Constraints(requiredNetworkType = NetworkType.CONNECTED))

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

        addTag(handler.fullName)
        tags.forEach {
            addTag(it)
        }
    }

    override fun taskInfosByTag(
        tag: String
    ) = workManager.getWorkInfosByTagFlow(tag).map { list ->
        list.map { it.toTaskInfo() }
    }

    override fun taskInfoById(
        id: Uuid
    ) = workManager.getWorkInfoByIdFlow(id.toJavaUuid()).map { it?.toTaskInfo() }

    override suspend fun cancelTaskById(id: Uuid) {
        workManager.cancelWorkById(id.toJavaUuid()).await()
    }

    override suspend fun cancelTasksByTag(tag: String) {
        workManager.cancelAllWorkByTag(tag).await()
    }

    private fun WorkInfo.toTaskInfo(): TaskInfo {
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
            nextScheduledTime = Instant.fromEpochMilliseconds(nextScheduleTimeMillis),
            progress = progress.toData()
        )
    }
}