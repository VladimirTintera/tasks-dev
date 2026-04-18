package eu.tintera.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import eu.tintera.tasks.core.ExecutionResult
import eu.tintera.tasks.core.TaskEvaluator
import eu.tintera.tasks.core.TaskRegistry
import eu.tintera.tasks.core.TaskResultProcessor
import eu.tintera.tasks.core.data.Repository
import eu.tintera.tasks.core.data.Task
import eu.tintera.tasks.core.nonTerminalStates
import eu.tintera.tasks.koin.TasksKoinComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.toKotlinUuid

internal class TaskWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters), TasksKoinComponent {

    private val notificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val taskEvaluator: TaskEvaluator by inject()
    private val repository: Repository by inject()
    private val taskResultProcessor: TaskResultProcessor by inject()
    private val taskRegistry: TaskRegistry by inject()

    override suspend fun doWork(): Result {

        val taskIdentifier = inputData.getString(
            TASK_IDENTIFIER
        )?.takeIf {
            it.isNotBlank()
        } ?: return Result.failure()

        EventBus.send("TaskWorker", "Task started '$taskIdentifier', data = ${inputData.toData()}")

        val registration = taskRegistry.resolve<Any, Any, Any>(taskIdentifier) ?: return Result.failure()

        val taskId = id.toKotlinUuid()

        var task = repository.task(taskId).first()

        if (task == null) {
            // ADOPCE STARÉHO ÚKOLU:
            // Vytáhneme všechna data z WorkManageru a zabalíme je do starého formátu (Verze 1)

            task = Task(
                id = taskId,
                identifier = taskIdentifier,
                inputData = registration.inputSerializer.encodeToBytes(inputData.toData()),
                outputData = null,
                progressData = null,
                version = 1, // DŮLEŽITÉ: Je to starý task, jde do verze 1
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
            )

            // Založíme ho u nás, aby o něm systém odteď věděl
            repository.insert(task, emptySet(), emptySet())
        } else {
            // Úkol už je náš, jen updatneme stav (pokud se např. jedná o Retry)
            repository.updateState(
                id = taskId,
                state = State.Running,
                allowedSourceStates = nonTerminalStates.toSet(),
                resetProcessTime = true,

                )
            task = task.copy(
                state = State.Running,
                runAttemptCount = runAttemptCount + 1
            )
        }


        val result = taskEvaluator.handle(
            task = task,
            onForegroundInfo = ::internalSetForegroundInfo
        )

        taskResultProcessor.handleResult(task, ExecutionResult.Finished(result))

        EventBus.send("TaskWorker", "Task finished '${taskIdentifier}', result = $result")

        return when (result) {
            TaskResult.Failure -> Result.failure()
            TaskResult.Retry -> Result.retry()
            is TaskResult.Success -> Result.success()
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

        val notification =
            NotificationCompat.Builder(applicationContext, channelId)
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