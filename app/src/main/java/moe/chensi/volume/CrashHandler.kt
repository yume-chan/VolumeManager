package moe.chensi.volume

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val TAG = "VolumeManager.CrashHandler"
    private const val CRASH_FILE_NAME = "last_crash.txt"
    private var originalHandler: Thread.UncaughtExceptionHandler? = null
    private var context: Context? = null
    private var crashFile: File? = null

    fun ensureInitialized(context: Context) {
        if (crashFile != null) return

        this.context = context.applicationContext ?: context
        crashFile = File(this.context!!.cacheDir, CRASH_FILE_NAME)
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    fun hasCrashReport(): Boolean {
        return crashFile?.exists() ?: false
    }

    fun readCrashReport(): String? {
        val file = crashFile ?: return null
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }

    fun clearCrashReport() {
        crashFile?.delete()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG, "Uncaught exception in thread ${t.name}", e)

        try {
            val crashInfo = formatCrashInfo(t, e)
            crashFile?.writeText(crashInfo)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to write crash report", ex)
        }

        originalHandler?.uncaughtException(t, e)
    }

    @Suppress("DEPRECATION")
    private fun formatCrashInfo(thread: Thread, throwable: Throwable): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val writer = StringWriter()
        val printer = PrintWriter(writer)
        throwable.printStackTrace(printer)

        return buildString {
            appendLine("=== CRASH REPORT ===")
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name} (id: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message ?: "null"}")
            appendLine()
            appendLine("=== STACK TRACE ===")
            appendLine(writer.toString())
            appendLine()
            appendLine("=== DEVICE INFO ===")
            appendLine("Manufacturer: ${android.os.Build.MANUFACTURER}")
            appendLine("Model: ${android.os.Build.MODEL}")
            appendLine("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("App Version: ${context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)?.versionName ?: "unknown"}")
        }
    }
}
