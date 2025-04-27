package moe.chensi.volume

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.IBinder
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
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuBinderWrapper
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.joor.Reflect
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
class Manager(private val context: Context, private val dataStore: DataStore<Preferences>) {
    companion object {
        private const val DHIZUKU = true

        private val getClientPidMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getClientPid")
        private val getPlayerProxyMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getPlayerProxy")
        private val setVolumeMethod: Method =
            Class.forName("android.media.PlayerProxy").getDeclaredMethod(
                "setVolume", Float::class.javaPrimitiveType
            )

        fun wrapBinder(proxy: Any) {
            val reflect = Reflect.on(proxy)
            var binder = reflect.get<IBinder>("mRemote")
            binder = if (DHIZUKU) {
                if (binder is DhizukuBinderWrapper) {
                    return
                } else {
                    Dhizuku.binderWrapper(binder)
                }
            } else {
                if (binder is ShizukuBinderWrapper) {
                    return
                } else {
                    ShizukuBinderWrapper(binder)
                }
            }
            reflect.set("mRemote", binder)
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
        if (DHIZUKU) {
            Dhizuku.init()
            _shizukuReady = true

            if (Dhizuku.isPermissionGranted()) {
                _shizukuPermission = true
                start()
            }
        } else {
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
        Reflect.onClass("android.app.ActivityThread").apply {
            call("getPackageManager")
            wrapBinder(get("sPackageManager"))
        }

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
                    Reflect.onClass(AudioManager::class.java).apply {
                        call("getService")
                        wrapBinder(get("sService"))
                    }
                    val audioManager = context.getSystemService(AudioManager::class.java)!!

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
        val activityService = Reflect.onClass(ActivityManager::class.java).call("getService")
            .apply { wrapBinder(get()) }

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

    fun requestPermission() {
        if (DHIZUKU) {
            Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                override fun onRequestPermission(grantResult: Int) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        _shizukuPermission = true
                        start()
                    }
                }
            })
        } else {
            Shizuku.requestPermission(0)
        }
    }
}
