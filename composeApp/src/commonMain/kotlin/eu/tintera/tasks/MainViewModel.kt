package eu.tintera.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tintera.tasks.handlers.TestHandler
import eu.tintera.tasks.handlers.scheduleTestHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MainViewModel(
    private val taskManager: TaskManager
) : ViewModel() {

    val tasks = combine(
        taskManager.taskInfosByTag(DEFAULT_TAG),
        taskManager.taskInfosByTag("sys:task_manager_cleanup")
    ) {
        it.flatMap { it }
    }.map {
        val finished = it.filter {
            it.state == State.Succeeded || it.state == State.Failed || it.state == State.Cancelled
        }

        TaskState(
            finished = finished.sortedByDescending { it.finishedAt },
            ongoing = (it - finished.toSet()).sortedBy { it.nextScheduledTime ?: Instant.DISTANT_PAST }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TaskState(emptyList(), emptyList())
    )

    fun enqueueTask() = viewModelScope.launch {
        taskManager.scheduleTestHandler(20)
    }

    fun cancelTaskGyId(id: Uuid) = viewModelScope.launch {
        taskManager.cancelTaskById(id)
    }

    fun cancelTasks() = viewModelScope.launch {
        taskManager.cancelTasksByTag(DEFAULT_TAG)
    }

    companion object {
        const val DEFAULT_TAG = "TestTask"
    }
}