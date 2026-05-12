package moe.chensi.volume.compose

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import moe.chensi.volume.BuildConfig
import moe.chensi.volume.R

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(R.string.about))
            },
            text = {
                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.alignByBaseline()
                        )
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                val clip =
                                    ClipData.newPlainText("VolumeManager", BuildConfig.VERSION_NAME)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.alignByBaseline()
                        ) {
                            Text("Copy")
                        }
                    }
                    Text(
                        text = stringResource(R.string.about_author, "yume-chan"),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            },
            confirmButton = {
                Column {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/yume-chan/VolumeManager".toUri()
                            )
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.about_github))
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        )
    }
}
