package eu.tintera.tasks

import kotlin.uuid.Uuid

interface TaskScope {
    val taskId: Uuid
    val data: Data
    val retryCount: Int

    suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean
    suspend fun setProgress(data: Data)
}