package eu.tintera.background.tasks.core.db

import eu.tintera.background.tasks.core.data.TaskScopeRepository
import eu.tintera.background.tasks.db.dao.TaskScopeDao
import kotlin.uuid.Uuid

internal class TaskScopeRepositoryImpl(
    private val taskScopeDao: TaskScopeDao,
) : TaskScopeRepository {

    override suspend fun updateProgressData(
        id: Uuid,
        progressData: ByteArray?
    ) = taskScopeDao.updateProgressData(id, progressData)
}