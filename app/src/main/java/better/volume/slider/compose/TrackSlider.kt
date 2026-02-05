package better.volume.slider.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun TrackSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val valueRange: ClosedFloatingPointRange<Float> = 0f..1f
    val trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
    val onTrackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor: Color = MaterialTheme.colorScheme.primary
    val onFillColor: Color = MaterialTheme.colorScheme.onPrimary
    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val latestValue by rememberUpdatedState(coercedValue)
    val density = LocalDensity.current

    val fillWidthPercentage =
        (coercedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(GenericShape { size, _ ->
                addRoundRect(
                    RoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius = CornerRadius(size.minDimension / 5f)
                    )
                )
            })
            .background(trackColor)
            .pointerInput(true) {
                // Use awaitPointerEventScope to handle multiple gesture types
                coroutineScope {
                    // Logic for Taps
                    launch {
                        detectTapGestures { offset ->
                            val percentage = offset.x / size.width.toFloat()
                            val totalRange = valueRange.endInclusive - valueRange.start
                            val newValue = (valueRange.start + percentage * totalRange)
                                .coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(newValue)
                        }
                    }

                    // Logic for Drags
                    launch {
                        var startValue = 0f
                        var startX = 0f
                        detectHorizontalDragGestures(onDragStart = { offset ->
                            // FIX: calculate startValue from the offset, not from latestValue
                            val percentage = offset.x / size.width.toFloat()
                            val totalRange = valueRange.endInclusive - valueRange.start
                            startValue = (valueRange.start + percentage * totalRange)
                                .coerceIn(valueRange.start, valueRange.endInclusive)
                            startX = offset.x
                        }) { change, _ ->
                            val dragAmount = change.position.x - startX
                            val changedPercentage = dragAmount / size.width.toFloat()
                            val totalRange = valueRange.endInclusive - valueRange.start
                            val newValue = (startValue + changedPercentage * totalRange)
                                .coerceIn(valueRange.start, valueRange.endInclusive)

                            if (newValue != latestValue) {
                                onValueChange(newValue)
                            }
                        }
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalContentColor provides onTrackColor) {
                content()
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(GenericShape { size, _ ->
                    addRoundRect(
                        RoundRect(
                            0f,
                            0f,
                            fillWidthPercentage * size.width,
                            size.height,
                            cornerRadius = CornerRadius(with(density) { 2.dp.toPx() })
                        )
                    )
                })
                .background(fillColor)
        ) {
            CompositionLocalProvider(LocalContentColor provides onFillColor) {
                content()
            }
        }
    }
}

