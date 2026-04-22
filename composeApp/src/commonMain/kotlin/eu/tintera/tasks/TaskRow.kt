package eu.tintera.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import eu.tintera.tasks.handlers.TestHandlerData
import eu.tintera.tasks.handlers.TestHandlerProgress
import org.jetbrains.compose.resources.painterResource
import taskmanager.composeapp.generated.resources.Res
import taskmanager.composeapp.generated.resources.check_24dp_1f1f1f_fill0_wght400_grad0_opsz24
import taskmanager.composeapp.generated.resources.schedule_24dp_1f1f1f_fill0_wght400_grad0_opsz24

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskRow(
    modifier: Modifier = Modifier,
    info: TaskInfo,
    actions: @Composable () -> Unit = {}
) {
    val stateColor = when (info.state) {
        State.Running -> Color(0xFF4CAF50)
        State.Enqueued -> Color(0xFFFF9800)
        State.Succeeded -> Color(0xFF9E9E9E)
        State.Failed -> Color(0xFFF44336)
        State.Blocked -> Color(0xFF2196F3) // Modrá pro blocked
        else -> Color.Gray
    }

    val shortId = info.id.toString().take(8)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Barevný indikátor stavu
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Hlavní informace
            Column(modifier = Modifier.weight(1f)) {

                // Horní řádek: Scrollovací Tagy + Stav napravo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tagy
                    FlowRow(
                        modifier = Modifier
                            .weight(1f)
                            //.horizontalScroll(rememberScrollState())
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        info.tags.forEach { tag ->
                            // Pokud je tag plný název třídy, vezmeme jen konec
                            val displayTag = if (tag.contains(".")) tag.substringAfterLast(".") else tag

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = displayTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }



                    // Textový stav
                    Text(
                        text = info.state.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = stateColor,
                        fontWeight = FontWeight.Bold
                    )
                }



                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "ID: $shortId... | Run attempts: ${info.runAttemptCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(info.nextScheduledTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(Res.drawable.schedule_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                            contentDescription = "Scheduled at"
                        )
                        Text(
                            text = info.nextScheduledTime.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(info.finishedAt != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(Res.drawable.check_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                            contentDescription = "Finished at"
                        )
                        Text(
                            text = info.finishedAt.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                (info.outputData as? TestHandlerData)?.also {
                    Text(
                        text = "output data: count: ${it.count}, name = ${it.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        actions()

        (info.progress as? TestHandlerProgress)?.also {
            Column {
                AnimatedVisibility(it.parents.isNotEmpty()) {
                    Text(
                        modifier = modifier.padding(16.dp),
                        text = "parent data: ${it.parents.joinToString()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val progress by animateFloatAsState(
                    targetValue = (it.progress - 1).toFloat() / it.totalCount.toFloat(),
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                )
                LinearWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
        }
        //ProgressSection()
    }
}