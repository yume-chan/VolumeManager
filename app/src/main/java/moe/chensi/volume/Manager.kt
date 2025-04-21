package moe.chensi.volume

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.joor.Reflect
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method
import java.util.Objects

@SuppressLint("PrivateApi")
class Manager(private val context: Context, private val dataStore: DataStore<Preferences>) {
    companion object {
        private val getClientPidMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getClientPid")
        private val getPlayerProxyMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getPlayerProxy")
        private val setVolumeMethod: Method =
            Class.forName("android.media.PlayerProxy").getDeclaredMethod(
                "setVolume", Float::class.javaPrimitiveType
            )

        fun getShizukuService(name: String, type: String): Any {
            val binder = SystemServiceHelper.getSystemService(name)
            val wrapper = ShizukuBinderWrapper(binder)
            return Reflect.onClass("$type\$Stub").call("asInterface", wrapper).get()
        }

    }

    private var _shizukuReady by mutableStateOf(false)
    val shizukuReady
        get() = _shizukuReady

    private var _shizukuPermission by mutableStateOf(false)
    val shizukuPermission
        get() = _shizukuPermission

    private val packageManager: PackageManager = context.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val apps = mutableStateMapOf<String, App>()

    init {
        Shizuku.addBinderReceivedListenerSticky {
            if (Shizuku.isPreV11()) {
                return@addBinderReceivedListenerSticky
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _shizukuPermission = true
                start()
            } else {
                _shizukuPermission = false
            }

            _shizukuReady = true
        }

        Shizuku.addBinderDeadListener {
            _shizukuReady = false
        }

        Shizuku.addRequestPermissionResultListener { _, grantResult ->
            _shizukuPermission = grantResult == PackageManager.PERMISSION_GRANTED
            if (_shizukuPermission) {
                start()
            }
        }
    }

    data class Player(val config: AudioPlaybackConfiguration, val player: Any)

    data class App(
        val packageName: String,
        val name: String,
        val icon: Drawable,
        val players: MutableList<Player>,
        val dataStore: DataStore<Preferences>
    ) {
        companion object {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }

        private var _volume by mutableFloatStateOf(1f)
        fun setVolume(value: Float, initializing: Boolean) {
            if (initializing) {
                volume = value
            } else {
                _volume = value
            }
        }

        var volume
            get() = _volume
            set(value) {
                _volume = value

                for (player in players) {
                    setVolumeMethod.invoke(player.player, value)
                }

                scope.launch {
                    dataStore.edit { preferences ->
                        preferences[floatPreferencesKey(packageName)] = volume
                    }
                }
            }
    }

    private fun start() {
        Reflect.onClass("android.app.ActivityThread").set(
            "sPackageManager", getShizukuService("package", "android.content.pm.IPackageManager")
        )

        scope.launch {
            var initializing = true

            dataStore.data.collect { preferences ->
                for ((key, volume) in preferences.asMap()) {
                    val packageName = key.name
                    apps.getOrPut(packageName) {
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        App(
                            packageName,
                            appInfo.loadLabel(packageManager).toString(),
                            appInfo.loadIcon(packageManager),
                            mutableStateListOf(),
                            dataStore
                        )
                    }.setVolume(volume as Float, initializing)
                }

                if (initializing) {
                    val audioManager =
                        Objects.requireNonNull(context.getSystemService(AudioManager::class.java)!!)
                    Reflect.on(audioManager).set(
                        "sService",
                        getShizukuService(Context.AUDIO_SERVICE, "android.media.IAudioService")
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

                    initializing = false
                }
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    fun processAudioPlaybackConfigurations(
        apps: MutableMap<String, App>, configs: List<AudioPlaybackConfiguration>
    ) {
        val activityService =
            Reflect.on(getShizukuService(Context.ACTIVITY_SERVICE, "android.app.IActivityManager"))

        val runningProcesses = activityService.call("getRunningAppProcesses")
            .get<List<ActivityManager.RunningAppProcessInfo>>()

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
                    dataStore
                ).also { apps[packageName] = it }
            }

            setVolumeMethod.invoke(player, app.volume)
            app.players.add(Player(config, player))
        }
    }
}