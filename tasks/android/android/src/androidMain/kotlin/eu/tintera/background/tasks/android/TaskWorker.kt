package eu.tintera.background.tasks.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.tintera.background.tasks.*
import eu.tintera.background.tasks.core.CompositeTaskLifecycleObserver
import eu.tintera.background.tasks.core.CompositeTasksLogger
import eu.tintera.background.tasks.core.TaskEvaluator
import eu.tintera.background.tasks.core.TaskEvaluatorResult
import eu.tintera.background.tasks.core.data.Repository
import eu.tintera.background.tasks.core.data.Task
import eu.tintera.background.tasks.core.nonTerminalStates
import eu.tintera.background.tasks.di.TasksKoinComponent
import eu.tintera.background.tasks.di.TasksKoinContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.toKotlinUuid

/**
 * The `CoroutineWorker` every task runs in. Normally you never touch it — [WorkManagerTaskManager]
 * enqueues it for you.
 *
 * It is `open` for one reason: **adopting work scheduled by something else.** WorkManager stores the
 * worker's class name in its own database, so work enqueued before you migrated still asks for the
 * old class by name. Declaring a subclass under that name makes those rows resolve again:
 *
 * ```
 * package com.example.legacy
 *
 * internal class LegacyWorker(
 *     context: Context,
 *     parameters: WorkerParameters,
 * ) : eu.tintera.background.tasks.android.TaskWorker(context, parameters)
 * ```
 *
 * Such a row has no matching record in this library's database, so [doWork] tries to adopt it
 * through `TaskManagerConfiguration.compatTransformation`. Without that transformation the task
 * fails — see the log message there.
 *
 * Providing the subclass is optional. If you skip it, WorkManager cannot instantiate the missing
 * class, logs an error and marks the work failed; it does not crash. That is a perfectly reasonable
 * choice when the scheduled work can simply be recreated after the upgrade.
 */
@OptIn(InternalTasksApi::class)
open class TaskWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters), TasksKoinComponent {

    private val notificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val taskEvaluator: TaskEvaluator by inject()
    private val repository: Repository by inject()

    private val workManagerConfiguration: WorkManagerConfiguration by inject()
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver by inject()
    private val log: CompositeTasksLogger by inject()

    override suspend fun doWork(): Result {

        // WorkManager initializes itself and its scheduler can reach for work while
        // `Application.onCreate` is still running, so a worker may well start before the
        // application had a chance to call TasksInitializer.initialize(...). Waiting costs
        // milliseconds; failing here and relying on a retry would push the same work minutes
        // into the future for nothing.
        withTimeoutOrNull(INITIALIZATION_TIMEOUT) { TasksKoinContext.awaitKoinApp() }
            ?: error(
                "TaskManager was not initialized within $INITIALIZATION_TIMEOUT. Call " +
                    "TasksInitializer.initialize(...) when your application starts."
            )

        val taskIdentifier = inputData.getString(
            TASK_IDENTIFIER
        )?.takeIf {
            it.isNotBlank()
        } ?: return Result.failure()


        val taskId = id.toKotlinUuid()

        val task = repository.task(taskId)?.also {
            repository.updateState(
                id = taskId,
                state = State.Running,
                allowedSourceStates = nonTerminalStates.toSet(),
                resetProcessTime = true,
                runAttemptCount = runAttemptCount + 1
            )
        } ?: run {
            // ADOPTING A LEGACY TASK: take everything WorkManager holds and wrap it in the old
            // format (version 1).

            val sourceData = inputData.keyValueMap.mapNotNull { (key, value) ->
                key.takeIf { it != TASK_IDENTIFIER }?.let {
                    key to value
                }
            }.toMap()

            val adopted = workManagerConfiguration.compatTransformation(sourceData)

            if (adopted == null) log.error(TAG) {
                "Task $taskId ('$taskIdentifier') is not in the database and compatTransformation " +
                    "cannot adopt it, so it will fail. If this is work scheduled by a previous version " +
                    "of the application, provide TaskManagerConfiguration.compatTransformation."
            }

            adopted?.let { byteArray ->
                Task(
                    id = taskId,
                    identifier = taskIdentifier,
                    inputData = byteArray,
                    outputData = null,
                    progressData = null,
                    version = 1, // IMPORTANT: Old task is always version 1
                    state = State.Running,
                    runAttemptCount = runAttemptCount + 1,
                    uniqueName = "",
                    initialDelay = Duration.ZERO,
                    processTime = null,
                    networkRequired = false,
                    createdAt = Clock.System.now(),
                    finishedAt = null,
                    repeatInterval = null,
                    backoffCriteria = null,
                    retentionDelay = 24.hours,
                    requiresDeviceIdle = false
                ).also {
                    repository.insert(it, emptySet(), emptySet())
                }
            }
        }

        if (task == null) return Result.failure()

        taskLifecycleObserver.onStarted(taskId)

        return try {
            val result = taskEvaluator.handle(
                id = taskId,
                onForegroundInfo = ::internalSetForegroundInfo
            )

            when (result) {
                TaskEvaluatorResult.FAILURE -> Result.failure()
                TaskEvaluatorResult.RETRY -> Result.retry()
                TaskEvaluatorResult.SUCCESS -> Result.success()
            }
        } catch (e: CancellationException) {
            taskLifecycleObserver.onCanceled(taskId)
            throw e
        } catch (e: Throwable) {
            taskLifecycleObserver.onCompleted(taskId, TaskResult.failure())
            throw e
        }
    }

    private suspend fun internalSetForegroundInfo(
        foregroundInfo: ForegroundInfo,
    ): Boolean = try {
        setForeground(foregroundInfo.createForegroundInfo())
        true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        false
    }

    private fun ForegroundInfo.createForegroundInfo(): androidx.work.ForegroundInfo {
        createChannel(channelId, channelName)
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(notificationTitle)
            .setTicker(notificationTitle)
            .setOngoing(true)
            .setSmallIcon(notificationIcon)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            androidx.work.ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        else androidx.work.ForegroundInfo(
            notificationId,
            notification
        )
    }

    private fun createChannel(id: String, name: String) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                id,
                name,
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        const val TASK_IDENTIFIER = "task_identifier"
        private const val TAG = "TaskWorker"

        /**
         * How long a worker waits for the library to be initialized. Generous on purpose — it only
         * has to outlast a cold start; running out means nobody ever called
         * `TasksInitializer.initialize(...)`, which no amount of waiting fixes.
         */
        private val INITIALIZATION_TIMEOUT = 30.seconds
    }

}