package eu.tintera.background.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.tintera.background.tasks.handlers.scheduleTestHandler
import eu.tintera.background.tasks.handlers.testTaskRequest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MainViewModel(
    private val taskManager: TaskManager
) : ViewModel() {

    val tasks = taskManager.taskInfosByTag(DEFAULT_TAG).catch {
        it.printStackTrace()
        emit(emptyList())
    }.map {
        val finished = it.filter {
            it.state == State.Succeeded || it.state == State.Failed || it.state == State.Cancelled
        }

        TaskState(
            finished = finished.sortedByDescending { it.finishedAt },
            ongoing = (it - finished.toSet()).sortedWith(
                compareBy({ it.state != State.Running }, { it.nextScheduledTime ?: Instant.DISTANT_PAST })
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TaskState(emptyList(), emptyList())
    )

    fun enqueueTask() = viewModelScope.launch {
        taskManager.scheduleTestHandler(count = 10, initialDelay = 5.seconds)
    }

    fun enqueueContinuation() = viewModelScope.launch {
        taskManager.enqueueContinuation(
            listOf(
                testTaskRequest(20, "task 1 A"),
                testTaskRequest(10, "task 1 B")
            ) then testTaskRequest(30, "task 2 A") then listOf(
                testTaskRequest(40, "task 3 A"),
                testTaskRequest(10, "task 3 B")
            )
        )
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