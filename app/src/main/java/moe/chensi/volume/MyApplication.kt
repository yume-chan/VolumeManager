package moe.chensi.volume

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuProvider

class MyApplication : Application() {
    companion object {
        const val MIN_FLOATING_BUTTON_SIZE_DP = 40
        const val MAX_FLOATING_BUTTON_SIZE_DP = 96
        const val DEFAULT_FLOATING_BUTTON_SIZE_DP = 56
        const val FLOATING_BUTTON_SIZE_STEP_DP = 4

        private const val FLOATING_BUTTON_PREFERENCES = "floating_button"
        private const val KEY_FLOATING_BUTTON_ENABLED = "enabled"
        private const val KEY_FLOATING_BUTTON_SIZE_DP = "size_dp"
        private const val SYSTEM_VOLUME_HOOKS_PREFERENCES = "system_volume_hooks"
        private const val KEY_SYSTEM_VOLUME_HOOKS_ENABLED = "enabled"
    }

    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_volumes")
    private val floatingButtonPreferences by lazy {
        getSharedPreferences(FLOATING_BUTTON_PREFERENCES, MODE_PRIVATE)
    }
    private val systemVolumeHooksPreferences by lazy {
        getSharedPreferences(SYSTEM_VOLUME_HOOKS_PREFERENCES, MODE_PRIVATE)
    }

    private val _floatingButtonEnabled = MutableStateFlow(false)
    val floatingButtonEnabled = _floatingButtonEnabled.asStateFlow()

    private val _floatingButtonSize = MutableStateFlow(DEFAULT_FLOATING_BUTTON_SIZE_DP)
    val floatingButtonSize = _floatingButtonSize.asStateFlow()

    private val _systemVolumeButtonHooksEnabled = MutableStateFlow(true)
    val systemVolumeButtonHooksEnabled = _systemVolumeButtonHooksEnabled.asStateFlow()

    val manager by lazy {
        Manager(this, dataStore)
    }

    override fun onCreate() {
        super.onCreate()

        _floatingButtonEnabled.value =
            floatingButtonPreferences.getBoolean(KEY_FLOATING_BUTTON_ENABLED, false)
        _floatingButtonSize.value = floatingButtonPreferences.getInt(
            KEY_FLOATING_BUTTON_SIZE_DP,
            DEFAULT_FLOATING_BUTTON_SIZE_DP
        ).coerceIn(MIN_FLOATING_BUTTON_SIZE_DP, MAX_FLOATING_BUTTON_SIZE_DP)
        _systemVolumeButtonHooksEnabled.value =
            systemVolumeHooksPreferences.getBoolean(KEY_SYSTEM_VOLUME_HOOKS_ENABLED, true)
    }

    fun setFloatingButtonEnabled(enabled: Boolean) {
        floatingButtonPreferences.edit {
            putBoolean(KEY_FLOATING_BUTTON_ENABLED, enabled)
        }
        _floatingButtonEnabled.value = enabled
    }

    fun setFloatingButtonSize(sizeDp: Int) {
        val size = sizeDp.coerceIn(MIN_FLOATING_BUTTON_SIZE_DP, MAX_FLOATING_BUTTON_SIZE_DP)
        floatingButtonPreferences.edit {
            putInt(KEY_FLOATING_BUTTON_SIZE_DP, size)
        }
        _floatingButtonSize.value = size
    }

    fun setSystemVolumeButtonHooksEnabled(enabled: Boolean) {
        systemVolumeHooksPreferences.edit {
            putBoolean(KEY_SYSTEM_VOLUME_HOOKS_ENABLED, enabled)
        }
        _systemVolumeButtonHooksEnabled.value = enabled
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        val processName = getProcessName()
        Log.i("Application", "process name = $processName")
        ShizukuProvider.enableMultiProcessSupport(!processName.contains(":"))
        HiddenApiBypass.addHiddenApiExemptions("")
        
        CrashHandler.ensureInitialized(this)
    }
}