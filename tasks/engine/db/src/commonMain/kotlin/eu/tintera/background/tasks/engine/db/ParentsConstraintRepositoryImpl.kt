package eu.tintera.background.tasks.engine.db

import eu.tintera.background.tasks.core.constraints.ParentsConstraintRepository
import eu.tintera.background.tasks.db.dao.ParentConstraintDao
import eu.tintera.background.tasks.db.toTaskState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.uuid.Uuid

internal class ParentsConstraintRepositoryImpl(
    private val dao: ParentConstraintDao
) : ParentsConstraintRepository {
    override fun parentStates(
        taskId: Uuid
    ) = dao.parentStatesForTask(taskId).distinctUntilChanged().map { states ->
        states.map { it.toTaskState() }
    }
}