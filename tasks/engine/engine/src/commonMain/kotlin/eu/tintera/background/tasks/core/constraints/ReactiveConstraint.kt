package eu.tintera.background.tasks.core.constraints

import eu.tintera.background.tasks.core.ProcessableTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

internal class ReactiveConstraint(
    private val delegate: Constraint
) {
    val monitorDuringExecution: Boolean get() = delegate.monitorDuringExecution

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isValid(
        taskFlow: Flow<ProcessableTask?>
    ) = taskFlow.filterNotNull().distinctUntilChangedBy {
        delegate.hasConstraint(it)
    }.flatMapLatest { task ->
        if (!delegate.hasConstraint(task)) {
            flowOf(ConstraintResult.Met)
        } else {
            delegate.isValid(task)
        }
    }
}