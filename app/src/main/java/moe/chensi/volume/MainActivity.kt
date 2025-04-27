@file:OptIn(ExperimentalMaterial3Api::class)

package moe.chensi.volume

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.chensi.volume.ui.theme.VolumeManagerTheme
import org.joor.Reflect
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess

@SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "VolumeManager.Activity"

        private const val SERVICE_NAME_SEPARATOR = ":"
    }

    private lateinit var application: MyApplication

    @SuppressLint("MissingPermission")
    private fun grantSelfPermission(permission: String) {
        var state = packageManager.checkPermission(permission, packageName)
        if (state == PackageManager.PERMISSION_GRANTED) {
            return
        }

        val process = Reflect.onClass(Shizuku::class.java).call(
            "newProcess", arrayOf("pm", "grant", packageName, permission), null, null
        ).get<ShizukuRemoteProcess>()
        process.waitFor()

        state = packageManager.checkPermission(permission, packageName)
        if (state == PackageManager.PERMISSION_GRANTED) {
            return
        }

        throw SecurityException("Can't grant self permission $permission")
    }

    private fun enableAccessibilityService(name: String) {
        Settings.Secure.putInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)

        var enabledAccessibilityServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (enabledAccessibilityServices.isNullOrBlank()) {
            enabledAccessibilityServices = name
        } else if (enabledAccessibilityServices.contains(name)) {
            return
        } else {
            enabledAccessibilityServices += SERVICE_NAME_SEPARATOR + name
        }

        Settings.Secure.putString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            enabledAccessibilityServices
        )

        enabledAccessibilityServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledAccessibilityServices == null || !enabledAccessibilityServices.contains(name)) {
            throw SecurityException("Can't enable accessibility service $name")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        application = super.getApplication() as MyApplication
        val manager = Manager(this@MainActivity, application.dataStore)

        setContent {
            VolumeManagerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = { TopAppBar(title = { Text("Volume Manager") }) }) { innerPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        if (manager.shizukuReady) {
                            if (manager.shizukuPermission) {
                                AccessibilityService()
                                AppVolumeList(manager.apps.values)
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(
                                        16.dp, Alignment.CenterVertically
                                    )
                                ) {
                                    Text("Shizuku is installed and enabled")
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = "Allow volume manager to access Shizuku?"
                                    )

                                    Button(onClick = {
                                        manager.requestPermission()
                                    }) {
                                        Text(text = "Add permission")
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(
                                    16.dp, Alignment.CenterVertically
                                )
                            ) {
                                Text("Waiting for Shizuku...")
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Make sure Shizuku is installed and enabled"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AccessibilityService() {
        var permissionGranted by remember { mutableStateOf(false) }
        var serviceEnabled by remember { mutableStateOf(false) }

        LaunchedEffect(0) {
            try {
                grantSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                permissionGranted = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to grant permission", e)
            }

            try {
                enableAccessibilityService("$packageName/${Service::class.java.canonicalName}")
                serviceEnabled = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable accessibility service", e)
            }
        }

        Column {
            Text(text = "Permission granted: $permissionGranted")
            Text(text = "Service enabled: $serviceEnabled")
        }
    }
}
