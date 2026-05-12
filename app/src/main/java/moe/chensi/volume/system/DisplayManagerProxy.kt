package moe.chensi.volume.system

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.Display
import moe.chensi.volume.EnableBinderProxy
import moe.chensi.volume.ToggleableBinderProxy
import org.joor.Reflect
import java.util.WeakHashMap

class DisplayManagerProxy private constructor(context: Context) {
    companion object {
        private const val AUTO_BRIGHTNESS_ADJ_KEY = "screen_auto_brightness_adj"
        private val cache = WeakHashMap<Context, DisplayManagerProxy>()

        operator fun invoke(context: Context): DisplayManagerProxy {
            return cache.getOrPut(context) { DisplayManagerProxy(context) }
        }
    }

    private val displayManager = context.getSystemService(DisplayManager::class.java)!!
    private val contentResolver = context.contentResolver
    private val displayManagerGlobalReflect =
        Reflect.onClass("android.hardware.display.DisplayManagerGlobal").call("getInstance")
    private val displayManagerReflect = Reflect.on(displayManager)

    init {
        val service = displayManagerGlobalReflect.get<Any>("mDm")
        ToggleableBinderProxy.wrap(service)
    }

    private data class BrightnessInfoValue(
        val brightness: Float,
        val brightnessMinimum: Float,
        val brightnessMaximum: Float
    )

    @EnableBinderProxy
    private fun getDefaultDisplayBrightnessInfo(): BrightnessInfoValue? {
        val display = runCatching {
            displayManagerReflect.call("getDisplay", Display.DEFAULT_DISPLAY).get<Any?>()
        }.getOrNull() ?: return null

        val brightnessInfo = runCatching {
            Reflect.on(display).call("getBrightnessInfo").get<Any?>()
        }.getOrNull() ?: return null

        return runCatching {
            BrightnessInfoValue(
                brightness = Reflect.on(brightnessInfo).get<Float>("brightness"),
                brightnessMinimum = Reflect.on(brightnessInfo).get<Float>("brightnessMinimum"),
                brightnessMaximum = Reflect.on(brightnessInfo).get<Float>("brightnessMaximum")
            )
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    @EnableBinderProxy
    fun getDefaultDisplayBrightnessGammaPercentage(): Float {
        val brightnessInfo = getDefaultDisplayBrightnessInfo()
        if (brightnessInfo == null) {
            Log.w(
                "DisplayManagerProxy",
                "getDefaultDisplayBrightnessPercentage: brightnessInfo is null"
            )
            return 0f
        }
        Log.d(
            "DisplayManagerProxy",
            "getDefaultDisplayBrightnessPercentage: ${brightnessInfo.brightness} ${brightnessInfo.brightnessMinimum} ${brightnessInfo.brightnessMaximum}"
        )

        val gamma = BrightnessUtils.convertLinearToGammaPercentage(
            brightnessInfo.brightness,
            brightnessInfo.brightnessMinimum,
            brightnessInfo.brightnessMaximum
        )
        Log.d("DisplayManagerProxy", "getDefaultDisplayBrightnessPercentage: $gamma")

        return gamma
    }

    /**
     * Sets default display brightness.
     *
     * @param value Brightness in [0f, 1f].
     * Uses reflection to call DisplayManager hidden setBrightness API.
     */
    @SuppressLint("MissingPermission")
    @EnableBinderProxy
    fun setDefaultDisplayBrightnessGammaPercentage(value: Float) {
        Log.d("DisplayManagerProxy", "setDefaultDisplayBrightnessPercentage: $value")

        val brightnessInfo = getDefaultDisplayBrightnessInfo() ?: return

        val brightness = BrightnessUtils.convertGammaPercentageToLinear(
            value, brightnessInfo.brightnessMinimum, brightnessInfo.brightnessMaximum
        )
        Log.d("DisplayManagerProxy", "setDefaultDisplayBrightnessPercentage: $brightness")

        displayManagerReflect.call("setBrightness", Display.DEFAULT_DISPLAY, brightness)
    }

    @EnableBinderProxy
    fun isAutoBrightnessEnabled(): Boolean {
        val mode = Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        return mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    }

    @EnableBinderProxy
    fun setAutoBrightnessEnabled(enabled: Boolean) {
        val mode = if (enabled) {
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        } else {
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        }

        val updated = Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            mode
        )
        if (!updated) {
            Log.w("DisplayManagerProxy", "setAutoBrightnessEnabled failed")
        }
    }

    @EnableBinderProxy
    fun getAutoBrightnessBias(): Float {
        return Settings.System.getFloat(
            contentResolver,
            AUTO_BRIGHTNESS_ADJ_KEY,
            0f
        ).coerceIn(-1f, 1f)
    }

    @EnableBinderProxy
    fun setAutoBrightnessBias(value: Float) {
        val adjusted = value.coerceIn(-1f, 1f)
        displayManagerReflect.call("setTemporaryAutoBrightnessAdjustment", adjusted)

        val updated = Settings.System.putFloat(
            contentResolver,
            AUTO_BRIGHTNESS_ADJ_KEY,
            adjusted
        )
        if (!updated) {
            Log.w("DisplayManagerProxy", "setAutoBrightnessBias failed")
        }
    }

    @EnableBinderProxy
    fun registerDisplayListener(listener: DisplayManager.DisplayListener, handler: Handler?) {
        displayManager.registerDisplayListener(listener, handler)
    }

    @EnableBinderProxy
    fun unregisterDisplayListener(listener: DisplayManager.DisplayListener) {
        displayManager.unregisterDisplayListener(listener)
    }
}
