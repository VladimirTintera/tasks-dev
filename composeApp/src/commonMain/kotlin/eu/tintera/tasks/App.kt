package eu.tintera.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import taskmanager.composeapp.generated.resources.schedule_24dp_1f1f1f_fill0_wght400_grad0_opsz24
import kotlin.uuid.Uuid


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
                FlowRow(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = viewModel::enqueueTask
                    ) { Text("Enqueue task") }

                    Button(
                        onClick = viewModel::enqueueContinuation
                    ) { Text("Enqueue continuation") }
                }

                val listState = rememberLazyListState()

                var seenIds by remember { mutableStateOf(setOf<Uuid>()) }

                LaunchedEffect(tasks.ongoing) {
                    val firstTask = tasks.ongoing.firstOrNull() ?: return@LaunchedEffect
                    val firstId = firstTask.id

                    // Pokud ID prvního prvku vidíme poprvé
                    if (firstId !in seenIds) {
                        // DŮLEŽITÉ: Scrollujeme jen pokud tam nějaké 'seenIds' už byly.
                        // (Abychom nescrollovali hned při prvním načtení obrazovky)
                        if (seenIds.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }

                        // Přidáme všechna aktuální ID do množiny "viděných"
                        seenIds = seenIds + tasks.ongoing.map { it.id }.toSet()
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp),
                    state = listState
                ) {
                    item(key = "static_top_anchor") {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                    items(tasks.ongoing, { "${it.id}" }) {
                        TaskRow(
                            modifier = Modifier.animateItem(),
                            info = it
                        ) {
                            if (it.state == State.Running || it.state == State.Enqueued || it.state == State.Blocked) {
                                Spacer(modifier = Modifier.width(8.dp))
                                FlowRow {
                                    TextButton(onClick = { viewModel.cancelTaskGyId(it.id) }) {
                                        Text(
                                            text = "Cancel",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    AnimatedVisibility(it.state == State.Running) {
                                        TextButton(onClick = {
                                            TestHandler.retry(it.id)
                                        }) {
                                            Text(
                                                text = "Retry",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    AnimatedVisibility(it.state == State.Running) {
                                        TextButton(onClick = {
                                            TestHandler.fail(it.id)
                                        }) {
                                            Text(
                                                text = "Fail",
                                                color = MaterialTheme.colorScheme.error
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

                    items(tasks.finished, { "${it.id}"}) {
                        TaskRow(
                            modifier = Modifier.animateItem(),
                            info = it
                        )
                    }
                }
            }
        }
    }
}