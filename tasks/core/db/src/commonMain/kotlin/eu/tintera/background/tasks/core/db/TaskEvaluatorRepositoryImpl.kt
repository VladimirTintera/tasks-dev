package eu.tintera.background.tasks.core.db

import eu.tintera.background.tasks.core.data.ExecutableTask
import eu.tintera.background.tasks.core.data.ParentData
import eu.tintera.background.tasks.core.data.TaskEvaluatorRepository
import eu.tintera.background.tasks.db.dao.TaskEvaluatorDao
import eu.tintera.background.tasks.db.toTaskBackoffCriteria
import kotlin.collections.component1
import kotlin.collections.component2
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

    override suspend fun executableTask(id: Uuid): ExecutableTask? =
        dao.getExecutableTasksById(id).map { (task, tags) ->
            ExecutableTask(
                identifier = task.identifier,
                runAttemptCount = task.runAttemptCount,
                version = task.version,
                inputData = task.inputData,
                outputData = task.outputData,
                progressData = task.progressData,
                tags = tags.map { it.name }.toSet(),
                repeatInterval = task.repeatInterval,
                backoffCriteria = task.backoffCriteria?.toTaskBackoffCriteria()
            )
        }.firstOrNull()
}