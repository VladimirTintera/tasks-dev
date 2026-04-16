package eu.tintera.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TaskInfoItem(
    modifier: Modifier = Modifier,
    info: TaskInfo
) = Card(
    modifier = modifier
) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.id.toString(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = info.state.toString(), modifier = Modifier.padding(start = 8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Run attempts count")
            Text(text = info.runAttemptCount.toString())
        }

        info.tags.forEach {
            Text(text = it)
        }

        Text(info.nextScheduledTime.toString())

        DataItems(info.progress?.toString())
        DataItems(info.outputData?.toString())
    }
}

@Composable
private fun DataItems(data: String?) {
    data?.takeIf {
        it.isNotEmpty()
    }?.also {
        Text(text = it)

    }
}