package eu.tintera.tasks

import kotlin.uuid.Uuid

interface TaskScope<Input, Progress> {
    val taskId: Uuid
    val data: Input
    val retryCount: Int

    suspend fun setForegroundInfo(foregroundInfo: ForegroundInfo): Boolean
    suspend fun setProgress(data: Progress)
}