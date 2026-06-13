package eu.tintera.background.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun ProgressSection(progress: Map<String, Any>) {
    // 1. Zkusíme vytáhnout procenta (buď přímo jako "percent" nebo vypočítat z "current"/"total")
    val percent = when {
        progress.containsKey("percent") -> (progress["percent"] as? Number)?.toFloat() ?: 0f
        progress.containsKey("current") && progress.containsKey("total") -> {
            val current = (progress["current"] as? Number)?.toFloat() ?: 0f
            val total = (progress["total"] as? Number)?.toFloat() ?: 1f
            current / total
        }
        else -> null
    }

    // 2. Textový popis (např. "Stahuji..." nebo "15/100")
    val statusText = progress["status"] as? String ?: progress["message"] as? String

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (percent != null) {
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                strokeCap = StrokeCap.Round
            )
        } else if (progress.containsKey("indeterminate")) {
            // Pro tasky, kde nevíme, jak dlouho to bude trvat
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (statusText != null) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}