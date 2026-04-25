package eu.tintera.tasks.core.data

import kotlin.uuid.Uuid

interface TaskScopeRepository {
    suspend fun updateProgressData(id: Uuid, progressData: ByteArray?)
}