package eu.tintera.background.tasks.core.data

import kotlin.uuid.Uuid

interface TaskScopeRepository {
    suspend fun updateProgressData(id: Uuid, progressData: ByteArray?)
}