package eu.tintera.tasks

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import eu.tintera.tasks.core.TaskEvaluator
import eu.tintera.tasks.koin.TasksKoinComponent
import kotlinx.coroutines.CancellationException
import org.koin.core.component.inject
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

internal class TaskWorker(
    context: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(context, workerParameters), TasksKoinComponent {

    private val notificationManager by lazy {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val taskEvaluator: TaskEvaluator by inject()

    override suspend fun doWork(): Result {

        val taskIdentifier = inputData.getString(
            TASK_IDENTIFIER
        )?.takeIf {
            it.isNotBlank()
        } ?: return Result.failure()

        EventBus.send("TaskWorker", "Task started '$taskIdentifier', data = ${inputData.toData()}")

        val result = with(taskEvaluator) {
            with(taskScope()) {
                handle(taskIdentifier = taskIdentifier)
            }
        }

        result?.also {
            EventBus.send("TaskWorker", "Task finished '${taskIdentifier}', result = $result")
        }

        return when (result) {
            null -> Result.failure()
            TaskResult.Failure -> Result.failure()
            TaskResult.Retry -> Result.retry()
            is TaskResult.Success -> Result.success(
                Data.Builder().putAll(result.outputData.map).build()
            )
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

    // Implementace TaskScope jako vnitřní objekt
    private fun taskScope() = object : TaskScope {
        override val taskId: Uuid = id.toKotlinUuid()
        override val data: eu.tintera.tasks.Data = inputData.toData()
        override val retryCount: Int
            get() = this@TaskWorker.runAttemptCount

        override suspend fun setForegroundInfo(
            foregroundInfo: ForegroundInfo
        ): Boolean = this@TaskWorker.internalSetForegroundInfo(foregroundInfo)

        override suspend fun setProgress(data: eu.tintera.tasks.Data) {
            this@TaskWorker.setProgress(Data.Builder().putAll(data.map).build())
        }
    }

    companion object {
        const val TASK_IDENTIFIER = "task_identifier"
    }
}