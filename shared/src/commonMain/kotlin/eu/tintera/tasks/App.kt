package eu.tintera.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.tintera.tasks.handlers.TestHandler
import eu.tintera.time.format.context.withRegionalContext
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import taskmanager.shared.generated.resources.Res
import taskmanager.shared.generated.resources.close_24dp_1f1f1f_fill0_wght400_grad0_opsz24
import kotlin.uuid.Uuid


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() = withRegionalContext {
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

                val listState = rememberLazyGridState()

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

                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    state = listState,
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(
                        key = "static_top_anchor",
                        span = { GridItemSpan(maxLineSpan) } // Zabrání odsunutí první položky doprava
                    ) {
                        Spacer(modifier = Modifier.height(0.dp))
                    }

                    items(tasks.ongoing, { "${it.id}" }) {
                        TaskRow(
                            modifier = Modifier.fillMaxWidth().animateItem(),
                            info = it
                        ) {
                            if (it.state == State.Running || it.state == State.Enqueued || it.state == State.Blocked) {
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

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HorizontalDivider()
                    }

                    items(tasks.finished, { "${it.id}" }) {
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