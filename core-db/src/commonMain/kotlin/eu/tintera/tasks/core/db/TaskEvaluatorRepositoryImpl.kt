package eu.tintera.tasks.core.db

import eu.tintera.tasks.core.data.ParentData
import eu.tintera.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.tasks.db.dao.TaskEvaluatorDao
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TaskEvaluatorRepositoryImpl(
    private val dao: TaskEvaluatorDao,
) : TaskEvaluatorRepository {

    override suspend fun parentsDataFor(id: Uuid) = dao.parentsDataFor(id).map {
        ParentData(
            id = it.id,
            identifier = it.identifier,
            outputData = it.outputData,
            finishedAt = it.finishedAt ?: Instant.DISTANT_PAST,
            version = it.version
        )
    }

    override suspend fun upgradeData(
        id: Uuid,
        input: ByteArray?,
        output: ByteArray?,
        progress: ByteArray?,
        version: Int
    ) = dao.upgradeData(
        taskId = id,
        input = input,
        output = output,
        progress = progress,
        version = version
    )
}