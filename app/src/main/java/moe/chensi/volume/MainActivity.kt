@file:OptIn(ExperimentalMaterial3Api::class)

package moe.chensi.volume

import android.annotation.SuppressLint
import android.companion.virtual.VirtualDeviceManager
import android.content.pm.PackageManager
import android.os.Build
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

        Log.i(TAG, "packageName: $packageName")

        val writeSecureSettingsPermission = packageManager.checkPermission(
            android.Manifest.permission.WRITE_SECURE_SETTINGS, packageName
        )

        Log.i(TAG, "Permission state before: $writeSecureSettingsPermission")

        if (writeSecureSettingsPermission == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= 34) {
                permissionManager.call(
                    "grantRuntimePermission",
                    packageName,
                    android.Manifest.permission.WRITE_SECURE_SETTINGS,
                    Reflect.onClass(VirtualDeviceManager::class.java)
                        .get("PERSISTENT_DEVICE_ID_DEFAULT")
                )
            } else {
                permissionManager.call(
                    "grantRuntimePermission",
                    packageName,
                    android.Manifest.permission.WRITE_SECURE_SETTINGS,
                    Reflect.onClass(UserHandle::class.java).get("CURRENT")
                )
            }
        }

        Log.i(
            TAG, "Permission state after: ${
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

        Log.i(
            TAG, "enabled accessibility services after: ${
                Settings.Secure.getString(
                    contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            }"
        )
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        application = super.getApplication() as MyApplication
        val manager = Manager(this@MainActivity, application.dataStore)
        var accessibilityServiceEnabled by mutableStateOf<Boolean?>(null)

        setContent {
            LaunchedEffect(manager.shizukuPermission) {
                if (manager.shizukuPermission == true) {
                    try {
                        enableAccessibilityService("$packageName/${Service::class.java.canonicalName}")
                        accessibilityServiceEnabled = true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to enable accessibility service", e)
                        accessibilityServiceEnabled = false
                    }
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
                            ready = manager.shizukuReady,
                        )

                        if (manager.shizukuReady) {
                            when (manager.shizukuPermission) {
                                true -> {
                                    Text("Permission granted")

                                    when (accessibilityServiceEnabled) {
                                        true -> {
                                            Text("Accessibility service enabled")
                                        }

                                        false -> {
                                            Text("Accessibility service failed to enable")
                                        }

                                        null -> {}
                                    }

                                    AppVolumeList(manager.apps.values)
                                }

                                false -> {
                                    Button(onClick = { Shizuku.requestPermission(0) }) {
                                        Text(text = "Request permission")
                                    }
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
