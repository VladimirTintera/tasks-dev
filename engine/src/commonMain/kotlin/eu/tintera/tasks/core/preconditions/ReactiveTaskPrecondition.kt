package eu.tintera.tasks.core.preconditions

import eu.tintera.tasks.core.ProcessableTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class ReactiveTaskPrecondition(
    private val delegate: TaskPrecondition
) {
    val monitorDuringExecution: Boolean get() = delegate.monitorDuringExecution

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isValid(
        taskFlow: Flow<ProcessableTask?>
    ) = taskFlow.filterNotNull().distinctUntilChangedBy {
        delegate.hasConstraint(it)
    }.flatMapLatest { task ->
        if (!delegate.hasConstraint(task)) {
            flowOf(PreconditionResult.Met)
        } else {
            delegate.isValid(task)
        }
    }
}