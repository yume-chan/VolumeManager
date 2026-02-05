package better.volume.slider.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToggleButton(
    checked: Boolean,
    checkedDescription: String,
    checkedIcon: ImageVector,
    uncheckedDescription: String,
    uncheckedIcon: ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    val description = if (checked) checkedDescription else uncheckedDescription
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below, 12.dp
        ),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = { onCheckedChange(!checked) }) {
            Icon(
                if (checked) checkedIcon else uncheckedIcon,
                contentDescription = description
            )
        }
    }
}