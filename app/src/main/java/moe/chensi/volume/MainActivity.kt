@file:OptIn(ExperimentalMaterial3Api::class)

package moe.chensi.volume

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import moe.chensi.volume.ui.theme.VolumeManagerTheme
import org.joor.Reflect
import rikka.shizuku.Shizuku

@SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "VolumeManager.Activity"

        private const val SERVICE_NAME_SEPARATOR = ":"
    }

    private lateinit var application: MyApplication

    private fun enableAccessibilityService(name: String) {
        val permissionManager = Reflect.on(getSystemService("permission"))
        permissionManager.set(
            "mPermissionManager",
            Manager.getShizukuService("permissionmgr", "android.permission.IPermissionManager")
        )

        permissionManager.call(
            "grantRuntimePermission",
            packageName,
            android.Manifest.permission.WRITE_SECURE_SETTINGS,
            Reflect.onClass(UserHandle::class.java).call("of", 0).get()
        )

        Log.i(
            TAG, "Permission state: ${
                packageManager.checkPermission(
                    android.Manifest.permission.WRITE_SECURE_SETTINGS, packageName
                )
            }"
        )

        Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)

        var enabledAccessibilityServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledAccessibilityServices.isNullOrBlank()) {
            Log.i(TAG, "enabled accessibility services is empty")
            enabledAccessibilityServices = name
        } else if (enabledAccessibilityServices.contains(name)) {
            Log.i(TAG, "enabled accessibility services already includes $name")
            return
        } else {
            Log.i(TAG, "enabled accessibility services doesn't include $name")
            enabledAccessibilityServices += "$SERVICE_NAME_SEPARATOR$name"
        }

        Settings.Secure.putString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            enabledAccessibilityServices
        )
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        application = super.getApplication() as MyApplication

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

            var manager by remember { mutableStateOf<Manager?>(null) }

            LaunchedEffect(shizukuPermission) {
                if (!shizukuPermission) {
                    return@LaunchedEffect
                }

                if (manager != null) {
                    return@LaunchedEffect
                }

                enableAccessibilityService("$packageName/${Service::class.java.canonicalName}")

                manager = Manager(this@MainActivity, application.dataStore)
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

                                if (manager != null) {
                                    AppVolumeList(manager!!.apps.values)
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
