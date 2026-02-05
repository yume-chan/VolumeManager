package better.volume.slider.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import better.volume.slider.R
import better.volume.slider.data.App

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
    apps: MutableCollection<App>,
    showAll: Boolean,
    onChange: (() -> Unit)? = null,
    content: (LazyListScope.() -> Unit)? = null
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        content?.invoke(this)

        if (!showAll) {
            items(items = apps.filter { app -> !app.hidden && app.isPlaying }
                .sortedWith(App.comparator), key = { app -> app.packageName }) { app ->
                AppVolumeSlider(app, showOptions = false, onChange = onChange)
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
