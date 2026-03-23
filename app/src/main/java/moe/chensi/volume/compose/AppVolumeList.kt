package moe.chensi.volume.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.chensi.volume.R
import moe.chensi.volume.data.App

fun LazyListScope.group(
    header: @Composable () -> String,
    apps: List<App>,
    enableHide: Boolean = true,
    onChange: (() -> Unit)? = null
) {
    if (apps.isNotEmpty()) {
        item {
            Text(
                text = header(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
            )
        }

        items(
            items = apps.sortedWith(App.comparator), key = { app -> app.packageName }) { app ->
            AppVolumeSlider(app, true, enableHide, onChange)
        }
    }
}

@Composable
fun AppVolumeList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    apps: MutableCollection<App>,
    showEmpty: Boolean = false,
    showAll: Boolean,
    onChange: (() -> Unit)? = null,
    onShowAll: (() -> Unit)? = null,
    content: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding
    ) {
        content?.invoke(this)

        if (!showAll) {
            val activePlayers =
                apps.filter { app -> !app.hidden && app.isPlaying }.sortedWith(App.comparator)

            if (activePlayers.isNotEmpty()) {
                items(items = activePlayers, key = { app -> app.packageName }) { app ->
                    AppVolumeSlider(app, showOptions = false, onChange = onChange)
                }
            } else if (showEmpty) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(
                            12.dp, Alignment.CenterVertically
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No active players")

                        Button(onClick = { onShowAll?.invoke() }) {
                            Text(text = "Show all apps")
                        }
                    }
                }
            }
            return@LazyColumn
        }

        val activePlayers = mutableListOf<App>()
        val inactivePlayers = mutableListOf<App>()
        val hiddenPlayers = mutableListOf<App>()
        val otherApps = mutableListOf<App>()

        for (app in apps) {
            if (app.isPlayer) {
                if (!app.hidden) {
                    if (app.isPlaying) {
                        activePlayers.add(app)
                    } else {
                        inactivePlayers.add(app)
                    }
                } else {
                    hiddenPlayers.add(app)
                }
            } else {
                otherApps.add(app)
            }
        }

        group({ stringResource(R.string.group_active) }, activePlayers, onChange = onChange)
        group({ stringResource(R.string.group_inactive) }, inactivePlayers, onChange = onChange)
        group({ stringResource(R.string.group_hidden) }, hiddenPlayers, onChange = onChange)
        group(
            { stringResource(R.string.group_other) },
            otherApps,
            enableHide = false,
            onChange = onChange
        )
    }
}
