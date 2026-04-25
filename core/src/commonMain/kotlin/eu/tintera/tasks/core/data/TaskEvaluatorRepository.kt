package eu.tintera.tasks.core.data

import kotlin.uuid.Uuid

interface TaskEvaluatorRepository {

    suspend fun upgradeData(
        id: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    )

    suspend fun parentsDataFor(id: Uuid): List<ParentData>
}