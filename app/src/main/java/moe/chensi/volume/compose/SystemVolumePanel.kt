package moe.chensi.volume.compose

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.chensi.volume.R
import kotlinx.coroutines.delay

private const val CALL_MODE_POLL_INTERVAL_MS = 500L
private const val DEFAULT_BRIGHTNESS = 127
private const val TAG = "SystemVolumePanel"

private fun isCallMode(mode: Int): Boolean {
    return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemVolumePanel(
    audioManager: AudioManager,
    showCallVolumeAlways: Boolean,
    showHideButton: Boolean,
    showSliders: Boolean,
    onShowSlidersChange: (Boolean) -> Unit,
    onChange: (() -> Unit)? = null
) {
    var inCallMode by remember { mutableStateOf(isCallMode(audioManager.mode)) }

    LaunchedEffect(showCallVolumeAlways) {
        if (!showCallVolumeAlways) {
            while (true) {
                inCallMode = isCallMode(audioManager.mode)
                delay(CALL_MODE_POLL_INTERVAL_MS)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showHideButton) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                ToggleButton(
                    checked = showSliders,
                    checkedDescription = stringResource(R.string.hide_system_sliders_in_popup),
                    checkedIcon = Icons.Default.Visibility,
                    uncheckedDescription = stringResource(R.string.show_system_sliders_in_popup),
                    uncheckedIcon = Icons.Default.VisibilityOff,
                    onCheckedChange = onShowSlidersChange
                )
            }
        }

        if (!showSliders) {
            return@Column
        }

        if (!showCallVolumeAlways && inCallMode) {
            StreamVolumeSlider(
                streamType = AudioManager.STREAM_VOICE_CALL,
                icon = Icons.Default.PhoneInTalk,
                name = stringResource(R.string.stream_call),
                audioManager = audioManager,
                onChange = onChange
            )
        }

        StreamVolumeSlider(
            streamType = AudioManager.STREAM_MUSIC,
            icon = Icons.Default.VolumeUp,
            name = stringResource(R.string.stream_media),
            audioManager = audioManager,
            onChange = onChange
        )

        StreamVolumeSlider(
            streamType = AudioManager.STREAM_RING,
            icon = Icons.Default.NotificationsActive,
            name = stringResource(R.string.stream_ring),
            audioManager = audioManager,
            onChange = onChange
        )

        if (showCallVolumeAlways) {
            StreamVolumeSlider(
                streamType = AudioManager.STREAM_VOICE_CALL,
                icon = Icons.Default.PhoneInTalk,
                name = stringResource(R.string.stream_call),
                audioManager = audioManager,
                onChange = onChange
            )
        }

        StreamVolumeSlider(
            streamType = AudioManager.STREAM_ALARM,
            icon = Icons.Default.Alarm,
            name = stringResource(R.string.stream_alarm),
            audioManager = audioManager,
            onChange = onChange
        )

        StreamVolumeSlider(
            streamType = AudioManager.STREAM_NOTIFICATION,
            icon = Icons.Default.NotificationsNone,
            name = stringResource(R.string.stream_notification),
            audioManager = audioManager,
            footer = { NotificationModeToggles(audioManager = audioManager, onChange = onChange) },
            onChange = onChange
        )

        BrightnessSlider(onChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationModeToggles(audioManager: AudioManager, onChange: (() -> Unit)? = null) {
    val context = LocalContext.current
    val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

    var ringerMode by remember { mutableIntStateOf(audioManager.ringerMode) }
    var interruptionFilter by remember { mutableIntStateOf(notificationManager.currentInterruptionFilter) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                ringerMode = audioManager.ringerMode
                interruptionFilter = notificationManager.currentInterruptionFilter
            }
        }

        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 4.dp)) {
        ToggleButton(
            checked = ringerMode == AudioManager.RINGER_MODE_VIBRATE,
            checkedDescription = stringResource(R.string.disable_vibrate_mode),
            checkedIcon = Icons.Default.Vibration,
            uncheckedDescription = stringResource(R.string.enable_vibrate_mode),
            uncheckedIcon = Icons.Default.VolumeUp
        ) {
            audioManager.ringerMode =
                if (it) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL
            ringerMode = audioManager.ringerMode
            onChange?.invoke()
        }

        ToggleButton(
            checked = interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL,
            checkedDescription = stringResource(R.string.disable_do_not_disturb),
            checkedIcon = Icons.Default.DoNotDisturbOn,
            uncheckedDescription = stringResource(R.string.enable_do_not_disturb),
            uncheckedIcon = Icons.Default.NotificationsActive
        ) {
            try {
                notificationManager.setInterruptionFilter(
                    if (it) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "Can't change interruption filter", e)
            }
            interruptionFilter = notificationManager.currentInterruptionFilter
            onChange?.invoke()
        }
    }
}

@Composable
private fun BrightnessSlider(onChange: (() -> Unit)? = null) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    fun getBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (_: Settings.SettingNotFoundException) {
            DEFAULT_BRIGHTNESS
        }
    }

    var brightness by remember { mutableIntStateOf(getBrightness()) }
    val canWrite = Settings.System.canWrite(context)
    val mainThreadHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(mainThreadHandler) {
            override fun onChange(selfChange: Boolean) {
                brightness = getBrightness()
            }
        }

        contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            observer
        )

        onDispose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    TrackSlider(
        cornerRadius = 20.dp,
        value = brightness.toFloat(),
        valueRange = 0f..255f,
        enabled = canWrite,
        onValueChange = { value ->
            brightness = value.toInt()
            if (canWrite) {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                onChange?.invoke()
            }
        }
    ) {
        StreamSliderLabelRow(
            icon = Icons.Default.Brightness6,
            name = stringResource(R.string.brightness),
            value = brightness,
            max = 255
        )
    }
}

@Composable
private fun StreamSliderLabelRow(icon: ImageVector, name: String, value: Int, max: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(12.dp, 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = name,
            modifier = Modifier.size(32.dp),
        )
        StreamSliderTextContent(name = name, valueText = "$value/$max")
    }
}
