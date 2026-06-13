package eu.tintera.background.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun <T> TagsRow(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    items: Collection<T>,
    tagValue: (T) -> String
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color,
                contentColor = contentColor
            ) {
                Text(
                    text = tagValue(tag),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}