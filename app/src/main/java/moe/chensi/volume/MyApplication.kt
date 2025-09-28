package moe.chensi.volume

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuProvider

class MyApplication : Application() {
    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_volumes")
    val manager by lazy {
        Manager(this, dataStore)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        val processName = getProcessName()
        Log.i("Application", "process name = $processName")
        ShizukuProvider.enableMultiProcessSupport(!processName.contains(":"))
        HiddenApiBypass.addHiddenApiExemptions("")
    }
}