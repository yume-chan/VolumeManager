@file:OptIn(ExperimentalMaterial3Api::class)

package moe.chensi.volume

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import moe.chensi.volume.compose.AppVolumeList
import moe.chensi.volume.compose.CrashReportDialog
import moe.chensi.volume.compose.ToggleButton
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

    @Suppress("SameParameterValue")
    @SuppressLint("MissingPermission")
    private fun grantSelfPermission(permission: String) {
        var state = this@MainActivity.checkSelfPermission(permission)
        if (state == PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Grant permission via `PackageManager` doesn't work on some Samsung devices
        val process = Reflect.onClass(Shizuku::class.java).call(
            "newProcess", arrayOf("pm", "grant", packageName, permission), null, null
        ).get<ShizukuRemoteProcess>()
        process.waitFor()

        state = this@MainActivity.checkSelfPermission(permission)
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

    val powerManager by lazy { getSystemService(PowerManager::class.java)!! }
    var isIgnoringBatteryOptimization by mutableStateOf(false)
    private fun checkBatteryOptimization() {
        isIgnoringBatteryOptimization =
            powerManager.isIgnoringBatteryOptimizations(applicationInfo.packageName)
    }

    @SuppressLint("DiscouragedPrivateApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        application = super.getApplication() as MyApplication
        val manager = application.manager

        CrashHandler.ensureInitialized(this)
        val showCrashReport = CrashHandler.hasCrashReport() && CrashHandler.readCrashReport() != null

        checkBatteryOptimization()

        setContent {
            var showAll by remember { mutableStateOf(false) }
            var crashReport by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(showCrashReport) {
                if (showCrashReport) {
                    crashReport = CrashHandler.readCrashReport()
                }
            }

            if (crashReport != null) {
                crashReport?.let { report ->
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { },
                        properties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        VolumeManagerTheme {
                            CrashReportDialog(
                                crashReport = report,
                                onDismiss = {
                                    CrashHandler.clearCrashReport()
                                    crashReport = null
                                }
                            )
                        }
                    }
                }
            }

            VolumeManagerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(), topBar = {
                        TopAppBar(title = { Text("Volume Manager") }, actions = {
                            if (manager.shizukuStatus == Manager.ShizukuStatus.Connected) {
                                ToggleButton(
                                    checked = showAll,
                                    checkedIcon = Icons.Default.Visibility,
                                    checkedDescription = "Hide inactive or hidden apps",
                                    uncheckedIcon = Icons.Default.VisibilityOff,
                                    uncheckedDescription = "Show all apps"
                                ) {
                                    showAll = it
                                }
                            }

                            if (BuildConfig.DEBUG) {
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Below, 12.dp
                                    ),
                                    tooltip = { PlainTooltip { Text("Trigger a crash for testing") } },
                                    state = rememberTooltipState()
                                ) {
                                    IconButton(onClick = { throw RuntimeException("Test crash triggered from UI") }) {
                                        Icon(
                                            Icons.Default.BugReport,
                                            contentDescription = stringResource(R.string.test_crash)
                                        )
                                    }
                                }
                            }
                        })
                    }) { innerPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        when (manager.shizukuStatus) {
                            Manager.ShizukuStatus.Uninstalled -> {
                                val context = LocalContext.current
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(
                                        16.dp, Alignment.CenterVertically
                                    )
                                ) {
                                    Text("Shizuku not installed")
                                    Text(
                                        textAlign = TextAlign.Center,
                                        text = "Please install Shizuku from the Play Store or GitHub"
                                    )
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                "https://play.google.com/store/apps/details?id=${Manager.SHIZUKU_PACKAGE_NAME}".toUri()
                                            )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("Get Shizuku on Play Store")
                                    }
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                "https://github.com/RikkaApps/Shizuku/releases".toUri()
                                            )
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("Get Shizuku on GitHub")
                                    }
                                }
                            }

                            Manager.ShizukuStatus.Disconnected -> Column(
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

                            Manager.ShizukuStatus.PermissionDenied -> Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(
                                    16.dp, Alignment.CenterVertically
                                )
                            ) {
                                Text("Shizuku is installed and enabled")
                                Text(
                                    textAlign = TextAlign.Center,
                                    text = "Allow App Volume Manager to access Shizuku?"
                                )

                                Button(onClick = { Shizuku.requestPermission(0) }) {
                                    Text(text = "Request permission")
                                }
                            }

                            Manager.ShizukuStatus.Connected -> {
                                ServiceStatus()
                                AppVolumeList(manager.apps.values, showAll)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        checkBatteryOptimization()
    }


    data class ErrorInfo(val message: String, val stack: String)

    @SuppressLint("BatteryLife")
    fun openBatterySettings() {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.fromParts("package", applicationInfo.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    @Composable
    fun ServiceStatus() {
        var permissionGranted by remember { mutableStateOf(false) }
        var serviceEnabled by remember { mutableStateOf(false) }
        var errorInfo by remember { mutableStateOf<ErrorInfo?>(null) }

        LaunchedEffect(0) {
            try {
                grantSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                permissionGranted = true
            } catch (e: Exception) {
                Log.e(TAG, "Can't add WRITE_SECURE_SETTINGS permission", e)
                errorInfo = ErrorInfo(e.message!!, e.stackTraceToString())
                return@LaunchedEffect
            }

            try {
                enableAccessibilityService(
                    ComponentName(this@MainActivity, Service::class.java).flattenToString()
                )
                serviceEnabled = true
            } catch (e: Exception) {
                Log.e(TAG, "Can't enable accessibility service", e)
            }
        }

        errorInfo?.let { info ->
            val context = LocalContext.current

            AlertDialog(
                onDismissRequest = { errorInfo = null },
                title = { Text("Can't add permission") },
                text = { Text(info.message) },
                confirmButton = {
                    Button(onClick = { errorInfo = null }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val clip = ClipData.newPlainText("error_message", info.stack)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy full message")
                    }
                })
        }

        Column {
            Text(text = "Permission granted: ${if (permissionGranted) "Yes" else "No"}")
            Text(text = "Service enabled: ${if (serviceEnabled) "Yes" else "No"}")
        }

        Log.i(TAG, "Manufacturer: ${Build.MANUFACTURER}")

        if (!isIgnoringBatteryOptimization) {
            Button(onClick = { openBatterySettings() }) {
                Text(text = "Disable battery optimization")
            }
        }
    }
}
