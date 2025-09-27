package moe.chensi.volume

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.joor.Reflect
import rikka.shizuku.Shizuku
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
class Manager(context: Context, private val dataStore: DataStore<Preferences>) {
    companion object {
        private val getClientPidMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getClientPid")
        private val getPlayerProxyMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getPlayerProxy")
        private val setVolumeMethod: Method =
            Class.forName("android.media.PlayerProxy").getDeclaredMethod(
                "setVolume", Float::class.javaPrimitiveType
            )

    }

    private var _shizukuReady by mutableStateOf(false)
    val shizukuReady
        get() = _shizukuReady

    private var _shizukuPermission by mutableStateOf(false)
    val shizukuPermission
        get() = _shizukuPermission

    val audioManager = context.getSystemService(AudioManager::class.java)!!.apply {
        Reflect.onClass(AudioManager::class.java).call("getService").get<Any>()
            .apply { ToggleableBinderProxy.wrap(this) }
    }
    private val activityManager = context.getSystemService(ActivityManager::class.java)!!.apply {
        Reflect.onClass(ActivityManager::class.java).call("getService").get<Any>()
            .apply { ToggleableBinderProxy.wrap(this) }
    }
    private val packageManager: PackageManager = context.packageManager.apply {
        Reflect.onClass("android.app.ActivityThread").call("getPackageManager").get<Any>()
            .apply { ToggleableBinderProxy.wrap(this) }
    }
    private val userManager = context.getSystemService(UserManager::class.java)!!
        .apply { Reflect.on(this).get<Any>("mService").apply { ToggleableBinderProxy.wrap(this) } }
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
        val dataStore: DataStore<Preferences>
    ) {
        companion object {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }

        val players: MutableList<Player> = mutableListOf()

        private var _volume by mutableFloatStateOf(1f)
        fun updateVolume(value: Float, initializing: Boolean) {
            _volume = value

            if (initializing) {
                for (player in players) {
                    setVolumeMethod.invoke(player.player, value)
                }
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

        private var _hidden by mutableStateOf(false)
        var hidden: Boolean
            get() = _hidden
            set(value) {
                _hidden = value

                scope.launch {
                    dataStore.edit { preferences ->
                        preferences[booleanPreferencesKey("hidden:$packageName")] = value
                    }
                }
            }

        fun updateHidden(value: Boolean) {
            _hidden = value
        }
    }

    @SuppressLint("MissingPermission")
    @EnableBinderProxy
    private fun createApp(packageName: String): App {
        for (user in Reflect.on(userManager).call("getUserHandles", true).get<List<UserHandle>>()) {
            try {
                val appInfo = Reflect.on(packageManager)
                    .call("getApplicationInfoAsUser", packageName, 0, user).get<ApplicationInfo>()

                Log.d(
                    "VolumeManager", "Found app info for: userId: $user, packageName: $packageName"
                )

                return App(
                    packageName,
                    appInfo.loadLabel(packageManager).toString(),
                    packageManager.getDrawable(packageName, appInfo.icon, appInfo)
                        ?: packageManager.defaultActivityIcon,
                    dataStore
                )
            } catch (_: PackageManager.NameNotFoundException) {
                continue
            }
        }

        Log.d("VolumeManager", "Can't find app info for: packageName: $packageName")
        return App(
            packageName, packageName, packageManager.defaultActivityIcon, dataStore
        )
    }

    fun getOrCreateApp(packageName: String): App {
        return apps.getOrPut(packageName) { createApp(packageName) }
    }

    @EnableBinderProxy
    private fun initialize() {
        val playbackConfigurations = audioManager.activePlaybackConfigurations

        processAudioPlaybackConfigurations(playbackConfigurations)

        audioManager.registerAudioPlaybackCallback(
            object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                    for (app in apps.values) {
                        app.players.clear()
                    }
                    processAudioPlaybackConfigurations(configs)
                }
            }, null
        )
    }

    private fun start() {
        scope.launch {
            var initializing = true

            dataStore.data.collect { preferences ->
                for ((key, value) in preferences.asMap()) {
                    if (key.name.startsWith("hidden:") && value is Boolean) {
                        val packageName = key.name.substringAfter("hidden:")
                        getOrCreateApp(packageName).updateHidden(value)
                    } else if (value is Float) {
                        val packageName = key.name
                        getOrCreateApp(packageName).updateVolume(value, initializing)
                    }
                }

                if (initializing) {
                    initializing = false
                    initialize()
                }
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    @EnableBinderProxy
    fun processAudioPlaybackConfigurations(configs: List<AudioPlaybackConfiguration>) {
        val runningProcesses = activityManager.runningAppProcesses

        for (config in configs) {
            val player = getPlayerProxyMethod.invoke(config) ?: continue

            val pid = getClientPidMethod.invoke(config) as Int
            val process = runningProcesses.find { process -> process.pid == pid } ?: continue

            val packageName = process.processName.split(":")[0]
            // It's possible to get the user ID from `process.uid / UserHandle.PER_USER_RANGE`.
            // But since we need to get app info from all users at startup anyway,
            // let's just reuse the method here.
            val app = getOrCreateApp(packageName)

            setVolumeMethod.invoke(player, app.volume)
            app.players.add(Player(config, player))
        }
    }
}
