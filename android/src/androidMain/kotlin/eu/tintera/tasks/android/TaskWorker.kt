package eu.tintera.tasks.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.tintera.tasks.EventBus
import eu.tintera.tasks.ForegroundInfo
import eu.tintera.tasks.InternalTasksApi
import eu.tintera.tasks.State
import eu.tintera.tasks.TaskResult
import eu.tintera.tasks.core.*
import eu.tintera.tasks.core.data.ExecutableTask
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.data.TaskProcessResult
import eu.tintera.tasks.di.TasksKoinComponent
import kotlinx.coroutines.CancellationException
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.toKotlinUuid

@OptIn(InternalTasksApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class TaskWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters), TasksKoinComponent {

    private val notificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val taskEvaluator: TaskEvaluator by inject()
    private val repository: Repository by inject()
    private val taskResultProcessor: TaskResultProcessor by inject()

    private val workManagerConfiguration: WorkManagerConfiguration by inject()
    private val taskLifecycleObserver: CompositeTaskLifecycleObserver by inject()

    override suspend fun doWork(): Result {

        val taskIdentifier = inputData.getString(
            TASK_IDENTIFIER
        )?.takeIf {
            it.isNotBlank()
        } ?: return Result.failure()

        EventBus.send("TaskWorker", "Task started '$taskIdentifier'")

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
            // ADOPCE STARÉHO ÚKOLU:
            // Vytáhneme všechna data z WorkManageru a zabalíme je do starého formátu (Verze 1)

            val sourceData = inputData.keyValueMap.mapNotNull { (key, value) ->
                key.takeIf { it != TASK_IDENTIFIER }?.let {
                    key to value
                }
            }.toMap()

            workManagerConfiguration.compatTransformation(sourceData)?.let { byteArray ->
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
                task = ExecutableTask(
                    identifier = task.identifier,
                    runAttemptCount = task.runAttemptCount,
                    version = task.version,
                    inputData = task.inputData,
                    outputData = task.outputData,
                    progressData = task.progressData
                ),
                onForegroundInfo = ::internalSetForegroundInfo
            )

            taskResultProcessor.handleResult(
                TaskProcessResult(
                    id = taskId,
                    executionResult = ExecutionResult.EvaluatorResult(result),
                    repeatInterval = task.repeatInterval,
                    backoffCriteria = task.backoffCriteria,
                    retryCount = task.runAttemptCount
                )
            )

            when (result) {
                TaskEvaluatorResult.Failure -> Result.failure()
                TaskEvaluatorResult.Retry -> Result.retry()
                is TaskEvaluatorResult.Success -> Result.success()
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
    }

}