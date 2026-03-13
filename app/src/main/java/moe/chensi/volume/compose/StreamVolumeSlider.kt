package moe.chensi.volume.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import moe.chensi.volume.compose.TrackSlider
import java.util.concurrent.atomic.AtomicInteger

private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"

internal object VolumeChangeObserver {
    private val refCount = AtomicInteger(0)
    private var receiver: BroadcastReceiver? = null
    private var registeredContext: Context? = null
    private var _volumeChangedCount by mutableIntStateOf(0)
    val volumeChangedCount: Int get() = _volumeChangedCount

    @Synchronized
    fun startObserving(context: Context) {
        if (refCount.incrementAndGet() == 1) {
            registeredContext = context.applicationContext
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    _volumeChangedCount++
                }
            }
            registeredContext!!.registerReceiver(
                receiver!!,
                IntentFilter(VOLUME_CHANGED_ACTION),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }

    @Synchronized
    fun stopObserving() {
        if (refCount.decrementAndGet() == 0) {
            receiver?.let {
                registeredContext!!.unregisterReceiver(it)
                receiver = null
            }
            registeredContext = null
        }
    }

    fun notifyVolumeChanged() {
        _volumeChangedCount++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamVolumeSlider(
    streamType: Int,
    icon: ImageVector,
    name: String,
    audioManager: AudioManager,
    onChange: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var volume by remember { mutableIntStateOf(audioManager.getStreamVolume(streamType)) }

    DisposableEffect(context) {
        VolumeChangeObserver.startObserving(context)
        onDispose {
            VolumeChangeObserver.stopObserving()
        }
    }

    val volumeChangedCount = VolumeChangeObserver.volumeChangedCount

    LaunchedEffect(volumeChangedCount) {
        volume = audioManager.getStreamVolume(streamType)
    }

    TrackSlider(
        cornerRadius = 20.dp,
        value = volume.toFloat(),
        valueRange = 0f..audioManager.getStreamMaxVolume(streamType).toFloat(),
        onValueChange = { value ->
            volume = value.toInt()
            audioManager.setStreamVolume(streamType, value.toInt(), 0)
            onChange?.invoke()
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp, 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                modifier = Modifier.size(32.dp),
            )

            Text(text = name)
        }
    }
}
