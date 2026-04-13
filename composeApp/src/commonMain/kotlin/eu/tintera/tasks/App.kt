package eu.tintera.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.tintera.tasks.handlers.TestHandler
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import taskmanager.composeapp.generated.resources.Res
import taskmanager.composeapp.generated.resources.cancel_24dp_1f1f1f_fill0_wght400_grad0_opsz24
import taskmanager.composeapp.generated.resources.close_24dp_1f1f1f_fill0_wght400_grad0_opsz24


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    MaterialTheme {

        val viewModel = koinViewModel<MainViewModel>()

        val tasks by viewModel.tasks.collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tasks") },
                    actions = {
                        IconButton(onClick = viewModel::cancelTasks) {
                            Icon(
                                painter = painterResource(Res.drawable.close_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                contentDescription = "Cancel all tasks"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).consumeWindowInsets(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = viewModel::enqueueTask
                    ) { Text("Enqueue task") }
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
                            if (it.state == State.Running || it.state == State.Enqueued || it.state == State.Blocked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    IconButton(onClick = { viewModel.cancelTaskGyId(it.id) }) {
                                        Icon(
                                            painter = painterResource(Res.drawable.close_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                            contentDescription = "Cancel Task",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    AnimatedVisibility(it.state == State.Running) {
                                        IconButton(onClick = {
                                            TestHandler.interrupt(it.id)
                                        }) {
                                            Icon(
                                                painter = painterResource(Res.drawable.cancel_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                                                contentDescription = "Retry Task",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HorizontalDivider()
                    }

                    items(tasks.finished) {
                        TaskRow(info = it)
                    }
                }
            }
        }
    }
}