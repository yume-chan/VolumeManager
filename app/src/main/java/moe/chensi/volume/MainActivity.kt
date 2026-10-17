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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.chensi.volume.compose.AboutDialog
import moe.chensi.volume.compose.AppVolumeList
import moe.chensi.volume.compose.CrashReportDialog
import moe.chensi.volume.compose.SystemVolumePanel
import moe.chensi.volume.compose.ToggleButton
import moe.chensi.volume.ui.theme.VolumeManagerTheme
import org.joor.Reflect
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

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
        val showCrashReport =
            CrashHandler.hasCrashReport() && CrashHandler.readCrashReport() != null

        checkBatteryOptimization()

        setContent {
            val floatingButtonEnabled by application.floatingButtonEnabled.collectAsState()
            val floatingButtonSize by application.floatingButtonSize.collectAsState()
            val systemVolumeButtonHooksEnabled by
                application.systemVolumeButtonHooksEnabled.collectAsState()
            var showAll by remember { mutableStateOf(false) }
            var crashReport by remember { mutableStateOf<String?>(null) }
            var showAboutDialog by remember { mutableStateOf(false) }

            LaunchedEffect(showCrashReport) {
                if (showCrashReport) {
                    crashReport = CrashHandler.readCrashReport()
                }
            }

            if (crashReport != null) {
                crashReport?.let { report ->
                    Dialog(
                        onDismissRequest = { }, properties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        VolumeManagerTheme {
                            CrashReportDialog(
                                crashReport = report, onDismiss = {
                                    CrashHandler.clearCrashReport()
                                    crashReport = null
                                })
                        }
                    }
                }
            }

            if (showAboutDialog) {
                Dialog(
                    onDismissRequest = { showAboutDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    VolumeManagerTheme {
                        AboutDialog(onDismiss = { showAboutDialog = false })
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
                                    checkedIcon = Icons.Default.Check,
                                    checkedDescription = "Save",
                                    uncheckedIcon = Icons.Default.Settings,
                                    uncheckedDescription = "Settings"
                                ) {
                                    showAll = it
                                }
                            }

                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below, 12.dp
                                ),
                                tooltip = { PlainTooltip { Text(stringResource(R.string.about)) } },
                                state = rememberTooltipState()
                            ) {
                                IconButton(onClick = { showAboutDialog = true }) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = stringResource(R.string.about)
                                    )
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
                            .padding(16.dp, 0.dp)
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
                                        }) {
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
                                        }) {
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
                                    text = "Make sure Shizuku is enabled"
                                )
                                Button(
                                    onClick = {
                                        val launchIntent = packageManager.getLaunchIntentForPackage(
                                            Manager.SHIZUKU_PACKAGE_NAME
                                        )
                                        if (launchIntent != null) {
                                            startActivity(launchIntent)
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Unable to open Shizuku",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ) {
                                    Text("Open Shizuku")
                                }
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
                                if (showAll) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .toggleable(
                                                    value = floatingButtonEnabled,
                                                    role = Role.Switch,
                                                    onValueChange = { enabled ->
                                                        application.setFloatingButtonEnabled(enabled)
                                                    }
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                text = stringResource(R.string.floating_button_toggle)
                                            )
                                            Switch(
                                                checked = floatingButtonEnabled,
                                                onCheckedChange = null
                                            )
                                        }

                                        if (floatingButtonEnabled) {
                                            Text(
                                                text = stringResource(
                                                    R.string.floating_button_size,
                                                    floatingButtonSize
                                                )
                                            )
                                            Slider(
                                                value = floatingButtonSize.toFloat(),
                                                onValueChange = { value ->
                                                    val size = value.roundToInt()
                                                    if (size != floatingButtonSize) {
                                                        application.setFloatingButtonSize(size)
                                                    }
                                                },
                                                valueRange = MyApplication.MIN_FLOATING_BUTTON_SIZE_DP.toFloat()..
                                                    MyApplication.MAX_FLOATING_BUTTON_SIZE_DP.toFloat(),
                                                steps = (MyApplication.MAX_FLOATING_BUTTON_SIZE_DP -
                                                    MyApplication.MIN_FLOATING_BUTTON_SIZE_DP) /
                                                    MyApplication.FLOATING_BUTTON_SIZE_STEP_DP - 1
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .toggleable(
                                                    value = systemVolumeButtonHooksEnabled,
                                                    role = Role.Switch,
                                                    onValueChange = { enabled ->
                                                        application.setSystemVolumeButtonHooksEnabled(
                                                            enabled
                                                        )
                                                    }
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                text = stringResource(
                                                    R.string.system_volume_button_hooks_toggle
                                                )
                                            )
                                            Switch(
                                                checked = systemVolumeButtonHooksEnabled,
                                                onCheckedChange = null
                                            )
                                        }
                                    }
                                }

                                ServiceStatus()

                                AppVolumeList(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                    apps = manager.apps.values,
                                    showEmpty = true,
                                    showAll = showAll,
                                    showVolumeButtonToggle = systemVolumeButtonHooksEnabled,
                                    onShowAll = { showAll = true },
                                    content = {
                                        item("system_volume_panel_main") {
                                            SystemVolumePanel(
                                                audioManager = manager.audioManager,
                                                notificationManagerProxy = manager.notificationManagerProxy,
                                                showCallVolumeAlways = true,
                                                applyVisibilityFilter = !showAll,
                                                allowVisibilityConfig = showAll,
                                                isSliderVisible = manager::isSystemSliderVisible,
                                                onSliderVisibilityChange = manager::setSystemSliderVisible,
                                            )
                                        }
                                    })
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
    private fun createBatteryOptimizationSettingsIntent() = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.fromParts("package", applicationInfo.packageName, null)
    )

    @Composable
    fun ServiceStatus() {
        var errorInfo by remember { mutableStateOf<ErrorInfo?>(null) }
        val batterySettingsLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            checkBatteryOptimization()
            lifecycleScope.launch {
                delay(2_000.milliseconds)
                checkBatteryOptimization()
            }
        }

        LaunchedEffect(0) {
            try {
                grantSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
            } catch (e: Exception) {
                Log.e(TAG, "Can't add WRITE_SECURE_SETTINGS permission", e)
                errorInfo = ErrorInfo(e.message!!, e.stackTraceToString())
                return@LaunchedEffect
            }

            try {
                enableAccessibilityService(
                    ComponentName(this@MainActivity, Service::class.java).flattenToString()
                )
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

        Log.i(TAG, "Manufacturer: ${Build.MANUFACTURER}")

        if (!isIgnoringBatteryOptimization) {
            Button(
                onClick = {
                    batterySettingsLauncher.launch(createBatteryOptimizationSettingsIntent())
                }
            ) {
                Text(text = "Disable battery optimization")
            }
        }
    }
}
