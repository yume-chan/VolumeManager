@file:OptIn(ExperimentalMaterial3Api::class)

package moe.chensi.volume

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.chensi.volume.ui.theme.VolumeManagerTheme
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

@SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
@Suppress("UNCHECKED_CAST")
class MainActivity : ComponentActivity() {
    private val getClientPidMethod: Method =
        AudioPlaybackConfiguration::class.java.getDeclaredMethod("getClientPid")
    private val getPlayerProxyMethod: Method =
        AudioPlaybackConfiguration::class.java.getDeclaredMethod("getPlayerProxy")
    private val setVolumeMethod: Method =
        Class.forName("android.media.PlayerProxy").getDeclaredMethod(
            "setVolume", Float::class.javaPrimitiveType
        )

    data class Player(val config: AudioPlaybackConfiguration, val player: Any)

    data class App(
        val packageName: String,
        val name: String,
        val icon: Drawable,
        val players: MutableList<Player>
    ) {
        var volume by mutableFloatStateOf(1f)

        suspend fun save(dataStore: DataStore<Preferences>) {
            dataStore.edit { preferences -> preferences[floatPreferencesKey(packageName)] = volume }
        }
    }

    private lateinit var application: MyApplication

    @SuppressLint("DiscouragedPrivateApi")
    fun processAudioPlaybackConfigurations(
        apps: MutableMap<String, App>, configs: List<AudioPlaybackConfiguration>
    ) {
        val activityService = Class.forName("android.app.IActivityManager\$Stub")
            .getDeclaredMethod("asInterface", IBinder::class.java).invoke(
                null,
                ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE))
            )
        val runningProcesses = activityService.javaClass.getDeclaredMethod("getRunningAppProcesses")
            .invoke(activityService) as List<ActivityManager.RunningAppProcessInfo>

        for (config in configs) {
            val player = getPlayerProxyMethod.invoke(config) ?: continue

            val pid = getClientPidMethod.invoke(config) as Int
            val process = runningProcesses.find { process -> process.pid == pid } ?: continue

            val packageName = process.processName.split(":")[0]
            val app: App = apps[packageName] ?: run {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                App(
                    packageName,
                    appInfo.loadLabel(packageManager).toString(),
                    appInfo.loadIcon(packageManager),
                    mutableStateListOf(),
                ).also { apps[packageName] = it }
            }

            setVolumeMethod.invoke(player, app.volume)
            app.players.add(Player(config, player))
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        application = super.getApplication() as MyApplication

        val sPackageManagerField =
            Class.forName("android.app.ActivityThread").getDeclaredField("sPackageManager")
        sPackageManagerField.isAccessible = true
        sPackageManagerField.set(
            null,
            Class.forName("android.content.pm.IPackageManager\$Stub")
                .getDeclaredMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))
        )

        val apps = mutableStateMapOf<String, App>()
        val scope = lifecycleScope

        setContent {
            var shizukuReady by remember { mutableStateOf(false) }
            var shizukuPermission by remember { mutableStateOf(false) }
            var shizukuPermissionDenied by remember { mutableStateOf(false) }

            LaunchedEffect("shizuku") {
                Shizuku.addBinderReceivedListenerSticky {
                    if (Shizuku.isPreV11()) {
                        return@addBinderReceivedListenerSticky
                    }

                    shizukuReady = true

                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        shizukuPermission = true
                    } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                        shizukuPermissionDenied = true
                    }
                }
                Shizuku.addBinderDeadListener {
                    shizukuReady = false

                }

                Shizuku.addRequestPermissionResultListener { _, grantResult ->
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        shizukuPermission = true
                    }
                }
            }

            LaunchedEffect(shizukuPermission) {
                if (!shizukuPermission) {
                    return@LaunchedEffect
                }

                scope.launch {
                    val preferences = application.dataStore.data.first()
                    for ((key, volume) in preferences.asMap()) {
                        val packageName = key.name
                        if (apps.containsKey(packageName)) {
                            apps[packageName]!!.volume = volume as Float
                        } else {
                            val appInfo = packageManager.getApplicationInfo(packageName, 0)
                            apps[packageName] = App(
                                packageName,
                                appInfo.loadLabel(packageManager).toString(),
                                appInfo.loadIcon(packageManager),
                                mutableStateListOf()
                            ).apply { this.volume = volume as Float }
                        }
                    }

                    val audioManager = getSystemService(AudioManager::class.java)
                    val sServiceField = audioManager.javaClass.getDeclaredField("sService")
                    sServiceField.isAccessible = true
                    sServiceField.set(
                        audioManager,
                        Class.forName("android.media.IAudioService\$Stub")
                            .getDeclaredMethod("asInterface", IBinder::class.java).invoke(
                                null, ShizukuBinderWrapper(
                                    SystemServiceHelper.getSystemService(
                                        Context.AUDIO_SERVICE
                                    )
                                )
                            )
                    )

                    val playbackConfigurations = audioManager.activePlaybackConfigurations

                    processAudioPlaybackConfigurations(apps, playbackConfigurations)

                    audioManager.registerAudioPlaybackCallback(
                        object : AudioManager.AudioPlaybackCallback() {
                            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                                for (app in apps.values) {
                                    app.players.clear()
                                }
                                processAudioPlaybackConfigurations(apps, configs)
                            }
                        }, null
                    )
                }
            }

            VolumeManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(),
                    topBar = { TopAppBar(title = { Text("Volume Manager") }) }) { innerPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        Greeting(
                            ready = shizukuReady,
                        )

                        if (shizukuReady) {
                            if (shizukuPermissionDenied) {
                                Text("Permission denied")
                            } else if (!shizukuPermission) {
                                Button(onClick = { Shizuku.requestPermission(0) }) {
                                    Text(text = "Request permission")
                                }
                            } else {
                                Text("Permission granted")

                                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(items = apps.values.filter { it.players.size != 0 }
                                        .toList(), key = { app -> app.packageName }) { app ->
                                        App(app)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun App(app: App) {
        val scope = rememberCoroutineScope()

        TrackSlider(value = app.volume, cornerRadius = 20.dp, onValueChange = { value ->
            app.volume = value

            Log.i("VolumeManager", "Set volume for ${app.name} to $value")
            for (player in app.players) {
                setVolumeMethod.invoke(player.player, value)
            }

            scope.launch {
                app.save(application.dataStore)
            }
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
}

@Composable
fun Greeting(ready: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = "Shizuku is ${if (ready) "ready" else "not ready"}", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VolumeManagerTheme {
        Greeting(false)
    }
}

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