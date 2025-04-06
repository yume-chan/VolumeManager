package moe.chensi.volume

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun TrackSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackColor: Color = Color.LightGray,
    fillColor: Color = Color.Blue,
    cornerRadius: Dp = 8.dp,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val latestValue by rememberUpdatedState(coercedValue)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (enabled) {
                    var startValue = 0f
                    var startX = 0f

                    detectHorizontalDragGestures(onDragStart = { offset ->
                        startValue = latestValue
                        startX = offset.x
                    }) { change, _ ->
                        val dragAmount = change.position.x - startX
                        val changedPercentage = dragAmount / size.width.toFloat()
                        val totalRange = valueRange.endInclusive - valueRange.start
                        val newValue = (startValue + changedPercentage * totalRange)
                        val coercedNewValue =
                            newValue.coerceIn(valueRange.start, valueRange.endInclusive)
                        if (coercedNewValue != latestValue) {
                            onValueChange(coercedNewValue)
                        }
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // Draw track
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, 0f),
                size = size,
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )

            clipPath(Path().apply {
                addRoundRect(
                    RoundRect(
                        0f, 0f, size.width, size.height, CornerRadius(cornerRadius.toPx())
                    )
                )
            }) {
                // Draw fill
                drawRoundRect(
                    color = fillColor, topLeft = Offset(0f, 0f), size = Size(
                        (coercedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start) * size.width,
                        size.height
                    ), cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            content()
        }
    }
}

@Composable
fun AppVolumeSlider(app: Manager.App, onChange: (() -> Unit)? = null) {
    TrackSlider(cornerRadius = 20.dp, value = app.volume, onValueChange = { value ->
        app.volume = value
        onChange?.invoke()
    }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp, 8.dp)
        ) {
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = "App icon",
                modifier = Modifier.width(32.dp),
                contentScale = ContentScale.FillWidth
            )

            Text(text = app.name, color = Color.White)
        }
    }
}

@Composable
fun AppVolumeList(
    apps: MutableCollection<Manager.App>,
    onChange: (() -> Unit)? = null,
    content: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content?.invoke(this)

        items(items = apps.filter { it.players.size != 0 }.toList(),
            key = { app -> app.packageName }) { app ->
            AppVolumeSlider(app, onChange)
        }
    }
}