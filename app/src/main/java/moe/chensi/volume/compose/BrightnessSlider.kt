package moe.chensi.volume.compose

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.chensi.volume.R
import moe.chensi.volume.system.DisplayManagerProxy
import kotlin.math.abs
import kotlin.math.roundToInt

private const val BRIGHTNESS_SLIDER_MAX = 100f
private const val BRIGHTNESS_CHANGE_TOLERANCE = 0.001f

@Composable
fun BrightnessSlider(
    displayManagerProxy: DisplayManagerProxy,
    footer: (@Composable () -> Unit)? = null,
    onChange: (() -> Unit)? = null
) {
    fun readBrightnessPercent(): Float {
        return (displayManagerProxy.getDefaultDisplayBrightness().coerceIn(0f, 1f) * BRIGHTNESS_SLIDER_MAX)
    }

    var brightnessPercent by remember { mutableFloatStateOf(readBrightnessPercent()) }
    val mainThreadHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(displayManagerProxy) {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit

            override fun onDisplayRemoved(displayId: Int) = Unit

            override fun onDisplayChanged(displayId: Int) {
                if (displayId == Display.DEFAULT_DISPLAY) {
                    brightnessPercent = readBrightnessPercent()
                }
            }
        }

        displayManagerProxy.registerDisplayListener(listener, mainThreadHandler)

        onDispose {
            displayManagerProxy.unregisterDisplayListener(listener)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackSlider(
            modifier = Modifier.weight(1f),
            cornerRadius = 20.dp,
            value = brightnessPercent,
            valueRange = 0f..BRIGHTNESS_SLIDER_MAX,
            onValueChange = { value ->
                if (abs(brightnessPercent - value) < BRIGHTNESS_CHANGE_TOLERANCE) {
                    return@TrackSlider
                }

                brightnessPercent = value
                displayManagerProxy.setDefaultDisplayBrightness((value / BRIGHTNESS_SLIDER_MAX).coerceIn(0f, 1f))
                onChange?.invoke()
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp, 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness6,
                    contentDescription = stringResource(R.string.brightness),
                    modifier = Modifier.size(32.dp),
                )
                StreamSliderTextContent(
                    name = stringResource(R.string.brightness),
                    valueText = "${brightnessPercent.roundToInt()}/${BRIGHTNESS_SLIDER_MAX.roundToInt()}"
                )
            }
        }

        footer?.invoke()
    }
}
