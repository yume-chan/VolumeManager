package better.volume.slider.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import better.volume.slider.data.App

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppVolumeSlider(
    app: App, showOptions: Boolean, enableHide: Boolean = true, onChange: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrackSlider(
            modifier = Modifier.weight(1f),
            value = app.volume,
            onValueChange = { value ->
                app.volume = value
                onChange?.invoke()
            }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp, 8.dp),
            ) {
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon!!,
                        contentDescription = "App icon",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(Color.Gray)
                    )
                }

                Text(text = app.name)
            }
        }

        if (showOptions) {
            if (enableHide) {
                ToggleButton(
                    checked = app.hidden,
                    checkedIcon = Icons.Default.Visibility,
                    checkedDescription = "Unhide app",
                    uncheckedIcon = Icons.Default.VisibilityOff,
                    uncheckedDescription = "Hide app"
                ) {
                    app.hidden = it
                }
            }

            ToggleButton(
                checked = app.disableVolumeButtons,
                checkedIcon = Icons.AutoMirrored.Filled.VolumeUp,
                checkedDescription = "Enable volume buttons",
                uncheckedIcon = Icons.AutoMirrored.Filled.VolumeOff,
                uncheckedDescription = "Disable volume buttons"
            ) {
                app.disableVolumeButtons = it
            }
        }
    }
}