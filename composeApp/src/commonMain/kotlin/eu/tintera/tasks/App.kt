package eu.tintera.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.tintera.tasks.handlers.TestHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
@Preview
fun App() {
    MaterialTheme {

        val taskManager = koinInject<TaskManager>()
        val scope = rememberCoroutineScope()

        val tasks by retain(taskManager) {
            taskManager.taskInfosByTag("SuccessTask").map {
                val finished = it.filter {
                    it.state == State.Succeeded || it.state == State.Failed || it.state == State.Cancelled
                }

                TaskState(
                    finished = finished.sortedBy { it.nextScheduledTime },
                    ongoing = (it - finished.toSet()).sortedBy { it.nextScheduledTime }
                )
            }.stateIn(scope, SharingStarted.Eagerly, TaskState(emptyList(), emptyList()))
        }.collectAsStateWithLifecycle()

        Scaffold { paddingValues ->

            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            taskManager.enqueueTask(
                                taskRequest<TestHandler>(
                                    tags = setOf("SuccessTask"),
                                    constraints = Constraints(
                                        requiresDeviceIdle = false,
                                        requiresNetwork = true
                                    )
                                )
                            )
                        }
                    }
                ) {
                    Text("Schedule simple success")
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(tasks.ongoing) {
                        TaskRow(
                            info = it
                        ) {
                            scope.launch { taskManager.cancelTaskById(id = it.id) }
                        }
                    }

                    item {
                        HorizontalDivider()
                    }

                    items(tasks.finished) {
                        TaskRow(
                            info = it
                        ) {
                            scope.launch { taskManager.cancelTaskById(id = it.id) }
                        }
                    }
                }
            }
        }
    }
}