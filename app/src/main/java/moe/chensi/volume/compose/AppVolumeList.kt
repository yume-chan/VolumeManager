package moe.chensi.volume.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.chensi.volume.R
import moe.chensi.volume.data.App

internal data class Group(
    val name: String,
    val apps: List<App>,
    val startIndex: Int,
    val enableHide: Boolean
)

fun LazyListScope.group(
    header: @Composable () -> String,
    apps: List<App>,
    enableHide: Boolean = true,
    onChange: (() -> Unit)? = null,
    onHeaderClick: (() -> Unit)? = null
) {
    if (apps.isNotEmpty()) {
        stickyHeader {
            Text(
                text = header(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    .then(
                        if (onHeaderClick != null) {
                            Modifier.clickable { onHeaderClick() }
                        } else {
                            Modifier
                        }
                    )
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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    val activePlayers = mutableListOf<App>()
    val inactivePlayers = mutableListOf<App>()
    val hiddenPlayers = mutableListOf<App>()
    val otherAppsWithActivities = mutableListOf<App>()
    val otherAppsWithoutActivities = mutableListOf<App>()

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
            if (app.hasAnyActivity) {
                otherAppsWithActivities.add(app)
            } else {
                otherAppsWithoutActivities.add(app)
            }
        }
    }

    val groups = buildList<Group> {
        var currentIndex = 0
        val addGroup = { name: String, appsList: List<App>, enableHide: Boolean ->
            if (appsList.isNotEmpty()) {
                add(Group(name, appsList, currentIndex, enableHide))
                currentIndex += 1 + appsList.size
            }
        }
        addGroup(stringResource(R.string.group_active), activePlayers, true)
        addGroup(stringResource(R.string.group_inactive), inactivePlayers, true)
        addGroup(stringResource(R.string.group_hidden), hiddenPlayers, true)
        addGroup(stringResource(R.string.group_other), otherAppsWithActivities, false)
        addGroup(stringResource(R.string.group_system), otherAppsWithoutActivities, false)
    }

    LaunchedEffect(showAll) {
        if (showAll && groups.isNotEmpty()) {
            scope.launch {
                listState.scrollToItem(0, 0)
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = contentPadding
    ) {
        content?.invoke(this)

        if (!showAll) {
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

        groups.forEach { group ->
            group({ group.name }, group.apps, enableHide = group.enableHide, onChange = onChange, onHeaderClick = { selectedGroup = group.name })
        }
    }

    if (selectedGroup != null) {
        GroupSelectionDialog(
            groups = groups,
            selectedGroup = selectedGroup!!,
            listState = listState,
            scope = scope,
            onDismiss = { selectedGroup = null }
        )
    }
}

@Composable
internal fun GroupSelectionDialog(
    groups: List<Group>,
    selectedGroup: String,
    listState: LazyListState,
    scope: kotlinx.coroutines.CoroutineScope,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.jump_to_group)) },
        text = {
            Column {
                groups.forEach { group ->
                    val isSelected = group.name == selectedGroup
                    Button(
                        onClick = {
                            scope.launch {
                                listState.animateScrollToItem(group.startIndex)
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = if (isSelected) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        }
                    ) {
                        Text(text = group.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = stringResource(R.string.close))
            }
        }
    )
}
