package moe.chensi.volume

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun CrashReportDialog(
    crashReport: String,
    onDismiss: () -> Unit
) {
    var showFullReport by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Unexpected Crash Detected")
            },
            text = {
                Column {
                    Text(
                        text = "The app encountered an unexpected error on the previous run. Please report this issue to help improve the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (!showFullReport) {
                        val previewText = crashReport.lines().take(10).joinToString("\n")
                        val isTruncated = crashReport.lines().count() > 10

                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        )

                        if (isTruncated) {
                            TextButton(onClick = { showFullReport = true }) {
                                Text(text = "Show full report")
                            }
                        }
                    } else {
                        Text(
                            text = crashReport,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        )

                        TextButton(onClick = { showFullReport = false }) {
                            Text(text = "Show less")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val clip = ClipData.newPlainText("crash_report", crashReport)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Crash report copied to clipboard", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy Report & Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        CrashHandler.clearCrashReport()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        )
    }
}
