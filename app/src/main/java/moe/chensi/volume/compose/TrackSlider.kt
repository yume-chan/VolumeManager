package moe.chensi.volume.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.progressSemantics
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TrackSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trackColor: Color = MaterialTheme.colorScheme.primaryContainer,
    onTrackColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    onFillColor: Color = MaterialTheme.colorScheme.onPrimary,
    cornerRadius: Dp = 8.dp,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val coercedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val latestValue by rememberUpdatedState(coercedValue)
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }

    val fillWidthPercentage =
        (coercedValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .progressSemantics(value = coercedValue, valueRange = valueRange)
            .clip(GenericShape { size, _ ->
                addRoundRect(
                    RoundRect(
                        0f, 0f, size.width, size.height, cornerRadius = CornerRadius(cornerRadiusPx)
                    )
                )
            })
            .background(trackColor)
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
