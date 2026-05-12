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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import moe.chensi.volume.R
import moe.chensi.volume.system.DisplayManagerProxy
import moe.chensi.volume.system.NotificationManagerProxy
import kotlin.math.abs
import kotlin.math.roundToInt

object SystemSliderIds {
    const val Media = "media"
    const val Ring = "ring"
    const val Call = "call"
    const val Alarm = "alarm"
    const val Notification = "notification"
    const val Brightness = "brightness"
}

private const val BRIGHTNESS_CHANGE_TOLERANCE = 0.001f

private fun isCallMode(mode: Int): Boolean {
    return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemVolumePanel(
    audioManager: AudioManager,
    notificationManagerProxy: NotificationManagerProxy,
    displayManagerProxy: DisplayManagerProxy,
    showCallVolumeAlways: Boolean,
    applyVisibilityFilter: Boolean,
    allowVisibilityConfig: Boolean,
    isSliderVisible: (String) -> Boolean,
    onSliderVisibilityChange: (String, Boolean) -> Unit,
    onChange: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    var inCallMode by remember { mutableStateOf(isCallMode(audioManager.mode)) }

    DisposableEffect(audioManager, showCallVolumeAlways) {
        if (showCallVolumeAlways) {
            return@DisposableEffect onDispose { }
        }

        val listener = AudioManager.OnModeChangedListener { mode ->
            inCallMode = isCallMode(mode)
        }
        audioManager.addOnModeChangedListener(executor, listener)
        onDispose {
            audioManager.removeOnModeChangedListener(listener)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!applyVisibilityFilter || isSliderVisible(SystemSliderIds.Call)) {
            if (!showCallVolumeAlways && inCallMode || showCallVolumeAlways) {
                StreamVolumeSlider(
                    streamType = AudioManager.STREAM_VOICE_CALL,
                    icon = Icons.Default.PhoneInTalk,
                    name = stringResource(R.string.stream_call),
                    audioManager = audioManager,
                    footer = {
                        SliderVisibilityFooter(
                            sliderId = SystemSliderIds.Call,
                            sliderName = stringResource(R.string.stream_call),
                            allowVisibilityConfig = allowVisibilityConfig,
                            isVisible = isSliderVisible(SystemSliderIds.Call),
                            onSliderVisibilityChange = onSliderVisibilityChange
                        )
                    },
                    onChange = onChange
                )
            }
        }

        if (!applyVisibilityFilter || isSliderVisible(SystemSliderIds.Media)) {
            StreamVolumeSlider(
                streamType = AudioManager.STREAM_MUSIC,
                icon = Icons.Default.VolumeUp,
                name = stringResource(R.string.stream_media),
                audioManager = audioManager,
                footer = {
                    SliderVisibilityFooter(
                        sliderId = SystemSliderIds.Media,
                        sliderName = stringResource(R.string.stream_media),
                        allowVisibilityConfig = allowVisibilityConfig,
                        isVisible = isSliderVisible(SystemSliderIds.Media),
                        onSliderVisibilityChange = onSliderVisibilityChange
                    )
                },
                onChange = onChange
            )
        }

        if (!applyVisibilityFilter || isSliderVisible(SystemSliderIds.Ring)) {
            StreamVolumeSlider(
                streamType = AudioManager.STREAM_RING,
                icon = Icons.Default.RingVolume,
                name = stringResource(R.string.stream_ring),
                audioManager = audioManager,
                footer = {
                    RingFooter(
                        audioManager = audioManager,
                        notificationManagerProxy = notificationManagerProxy,
                        sliderVisible = isSliderVisible(SystemSliderIds.Ring),
                        allowVisibilityConfig = allowVisibilityConfig,
                        onSliderVisibilityChange = onSliderVisibilityChange,
                        onChange = onChange
                    )
                },
                onChange = onChange
            )
        }

        if (!applyVisibilityFilter || isSliderVisible(SystemSliderIds.Alarm)) {
            StreamVolumeSlider(
                streamType = AudioManager.STREAM_ALARM,
                icon = Icons.Default.Alarm,
                name = stringResource(R.string.stream_alarm),
                audioManager = audioManager,
                footer = {
                    SliderVisibilityFooter(
                        sliderId = SystemSliderIds.Alarm,
                        sliderName = stringResource(R.string.stream_alarm),
                        allowVisibilityConfig = allowVisibilityConfig,
                        isVisible = isSliderVisible(SystemSliderIds.Alarm),
                        onSliderVisibilityChange = onSliderVisibilityChange
                    )
                },
                onChange = onChange
            )
        }

        if (!applyVisibilityFilter || isSliderVisible(SystemSliderIds.Notification)) {
            StreamVolumeSlider(
                streamType = AudioManager.STREAM_NOTIFICATION,
                icon = Icons.Default.NotificationsNone,
                name = stringResource(R.string.stream_notification),
                audioManager = audioManager,
                footer = {
                    SliderVisibilityFooter(
                        sliderId = SystemSliderIds.Notification,
                        sliderName = stringResource(R.string.stream_notification),
                        allowVisibilityConfig = allowVisibilityConfig,
                        isVisible = isSliderVisible(SystemSliderIds.Notification),
                        onSliderVisibilityChange = onSliderVisibilityChange
                    )
                },
                onChange = onChange
            )
        }

        if (!applyVisibilityFilter || isSliderVisible(SystemSliderIds.Brightness)) {
            BrightnessSlider(
                displayManagerProxy = displayManagerProxy,
                sliderVisible = isSliderVisible(SystemSliderIds.Brightness),
                allowVisibilityConfig = allowVisibilityConfig,
                onSliderVisibilityChange = onSliderVisibilityChange,
                onChange = onChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RingFooter(
    audioManager: AudioManager,
    notificationManagerProxy: NotificationManagerProxy,
    sliderVisible: Boolean,
    allowVisibilityConfig: Boolean,
    onSliderVisibilityChange: (String, Boolean) -> Unit,
    onChange: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var ringerMode by remember { mutableIntStateOf(audioManager.ringerMode) }
    var interruptionFilter by remember { mutableIntStateOf(notificationManagerProxy.getCurrentInterruptionFilter()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                ringerMode = audioManager.ringerMode
                interruptionFilter = notificationManagerProxy.getCurrentInterruptionFilter()
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

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            notificationManagerProxy.setInterruptionFilter(
                if (it) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
            )
            interruptionFilter = notificationManagerProxy.getCurrentInterruptionFilter()
            onChange?.invoke()
        }

        SliderVisibilityToggle(
            sliderId = SystemSliderIds.Ring,
            sliderName = stringResource(R.string.stream_ring),
            allowVisibilityConfig = allowVisibilityConfig,
            isVisible = sliderVisible,
            onSliderVisibilityChange = onSliderVisibilityChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderVisibilityFooter(
    sliderId: String,
    sliderName: String,
    allowVisibilityConfig: Boolean,
    isVisible: Boolean,
    onSliderVisibilityChange: (String, Boolean) -> Unit
) {
    if (!allowVisibilityConfig) {
        return
    }

    Row(horizontalArrangement = Arrangement.End) {
        SliderVisibilityToggle(
            sliderId = sliderId,
            sliderName = sliderName,
            allowVisibilityConfig = allowVisibilityConfig,
            isVisible = isVisible,
            onSliderVisibilityChange = onSliderVisibilityChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderVisibilityToggle(
    sliderId: String,
    sliderName: String,
    allowVisibilityConfig: Boolean,
    isVisible: Boolean,
    onSliderVisibilityChange: (String, Boolean) -> Unit
) {
    if (!allowVisibilityConfig) {
        return
    }

    ToggleButton(
        checked = isVisible,
        checkedDescription = stringResource(R.string.hide_slider, sliderName),
        checkedIcon = Icons.Default.Visibility,
        uncheckedDescription = stringResource(R.string.show_slider, sliderName),
        uncheckedIcon = Icons.Default.VisibilityOff
    ) {
        onSliderVisibilityChange(sliderId, it)
    }
}

@Composable
private fun BrightnessSlider(
    displayManagerProxy: DisplayManagerProxy,
    sliderVisible: Boolean,
    allowVisibilityConfig: Boolean,
    onSliderVisibilityChange: (String, Boolean) -> Unit,
    onChange: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    fun readBrightness(): Float {
        return displayManagerProxy.getDefaultDisplayBrightness()
    }

    fun readMaxBrightness(): Float {
        return displayManagerProxy.getDefaultDisplayMaxBrightness().coerceAtLeast(1f)
    }

    var maxBrightness by remember { mutableFloatStateOf(readMaxBrightness()) }
    var brightness by remember { mutableFloatStateOf(readBrightness()) }
    val mainThreadHandler = remember { Handler(Looper.getMainLooper()) }

    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(mainThreadHandler) {
            override fun onChange(selfChange: Boolean) {
                maxBrightness = readMaxBrightness()
                brightness = readBrightness()
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
        value = brightness,
        valueRange = 0f..maxBrightness,
        onValueChange = { value ->
            if (abs(brightness - value) < BRIGHTNESS_CHANGE_TOLERANCE) {
                return@TrackSlider
            }
            brightness = value
            displayManagerProxy.setDefaultDisplayBrightness(value)
            onChange?.invoke()
        }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Brightness6,
                contentDescription = stringResource(R.string.brightness),
                modifier = Modifier.size(32.dp),
            )
            StreamSliderTextContent(
                name = stringResource(R.string.brightness),
                valueText = "${brightness.roundToInt()}/${maxBrightness.roundToInt()}"
            )
        }
    }

    SliderVisibilityFooter(
        sliderId = SystemSliderIds.Brightness,
        sliderName = stringResource(R.string.brightness),
        allowVisibilityConfig = allowVisibilityConfig,
        isVisible = sliderVisible,
        onSliderVisibilityChange = onSliderVisibilityChange
    )
}
