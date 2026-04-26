package eu.tintera.tasks.core.constraints

import eu.tintera.tasks.State
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface ParentsConstraintRepository {
    fun parentStates(taskId: Uuid): Flow<List<State>>
}