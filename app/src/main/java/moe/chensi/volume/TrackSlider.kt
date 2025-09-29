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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppVolumeSlider(
    app: Manager.App, menuVisible: Boolean, onChange: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackSlider(
            modifier = Modifier.weight(1f),
            cornerRadius = 20.dp,
            value = app.volume,
            onValueChange = { value ->
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

        if (menuVisible) {
            Box {
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(if (app.hidden) "Unhide app" else "Hide app") } },
                    state = rememberTooltipState()
                ) {
                    IconButton(onClick = { app.hidden = !app.hidden }) {
                        Icon(
                            if (app.hidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (app.hidden) "Unhide app" else "Hide app"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppVolumeList(
    apps: MutableCollection<Manager.App>,
    showAll: Boolean,
    onChange: (() -> Unit)? = null,
    content: (LazyListScope.() -> Unit)? = null
) {
    val (visibleActiveApps, visibleInactiveApps, hiddenApps) = if (showAll) {
        apps.fold(
            Triple(
                mutableListOf<Manager.App>(),
                mutableListOf<Manager.App>(),
                mutableListOf<Manager.App>()
            )
        ) { acc, app ->
            (if (app.hidden) acc.third else if (app.players.isNotEmpty()) acc.first else acc.second).add(
                app
            )
            acc
        }
    } else {
        Triple(
            apps.filter { app -> !app.hidden && app.players.isNotEmpty() },
            emptyList<Manager.App>(),
            emptyList<Manager.App>()
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content?.invoke(this)

        if (visibleActiveApps.isNotEmpty()) {
            if (showAll) {
                item {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            items(
                items = visibleActiveApps.sortedBy { it.name },
                key = { app -> app.packageName }) { app ->
                AppVolumeSlider(app, showAll, onChange)
            }
        }

        if (visibleInactiveApps.isNotEmpty()) {
            item {
                Text(
                    text = "Inactive",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(
                items = visibleInactiveApps.sortedBy { it.name },
                key = { app -> app.packageName }) { app ->
                AppVolumeSlider(app, true, onChange)
            }
        }

        if (hiddenApps.isNotEmpty()) {
            item {
                Text(
                    text = "Hidden",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(
                items = hiddenApps.sortedBy { it.name },
                key = { app -> app.packageName }) { app ->
                AppVolumeSlider(app, true, onChange)
            }
        }
    }
}