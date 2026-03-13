package moe.chensi.volume.system

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import moe.chensi.volume.BuildConfig
import org.joor.Reflect
import rikka.shizuku.Shizuku

object AudioSystemProxy {
    private const val TAG = "VolumeManager.AudioSystem"

    class AudioSystemService : AudioSystem.Stub() {
        private val audioSystemClass by lazy {
            Reflect.onClass("android.media.AudioSystem")
        }

        override fun isStreamActive(streamType: Int, inPastMs: Int): Boolean {
            Log.d(TAG, "isStreamActive: enter: $streamType, $inPastMs")
            // `isStreamActive` can only be called from system UIDs
            val result = audioSystemClass.call("isStreamActive", streamType, inPastMs).get<Boolean>()
            Log.d(TAG, "isStreamActive: exit: $result")
            return result
        }
    }

    private var service: AudioSystem? = null

    init {
        val componentName = ComponentName(
            BuildConfig.APPLICATION_ID,
            AudioSystemService::class.java.name
        )

        val args = Shizuku.UserServiceArgs(componentName)
            .version(BuildConfig.VERSION_CODE)
            .processNameSuffix("service")

        Shizuku.bindUserService(
            args, object : ServiceConnection {
                override fun onServiceConnected(
                    name: ComponentName?, binder: IBinder?
                ) {
                    Log.d(TAG, "onServiceConnected $binder")
                    service = AudioSystem.Stub.asInterface(binder)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Log.d(TAG, "onServiceDisconnected")
                    service = null
                }
            })
    }

    fun isStreamActive(streamType: Int, inPastMs: Int): Boolean {
        Log.d(TAG, "isStreamActive: proxy: $service $streamType $inPastMs")
        return service?.isStreamActive(streamType, inPastMs) ?: false
    }
}
