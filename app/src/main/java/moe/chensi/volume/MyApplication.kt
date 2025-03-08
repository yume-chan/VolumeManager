package moe.chensi.volume

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.lsposed.hiddenapibypass.HiddenApiBypass

class MyApplication : Application() {
    val dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_volumes")

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        HiddenApiBypass.addHiddenApiExemptions("")
    }
}