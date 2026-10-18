package moe.chensi.volume.compose

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.chensi.volume.CrashHandler
import moe.chensi.volume.R

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
                Text(text = stringResource(R.string.crash_detected_title))
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.crash_detected_description),
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
                                Text(text = stringResource(R.string.show_full_report))
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
                            Text(text = stringResource(R.string.show_less))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        val clip = ClipData.newPlainText(
                            context.getString(R.string.crash_report_clipboard_label), crashReport
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT
                        ).show()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.copy_report_and_close))
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
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
