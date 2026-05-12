package moe.chensi.volume.system

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.util.Log
import android.view.Display
import moe.chensi.volume.EnableBinderProxy
import moe.chensi.volume.ToggleableBinderProxy
import org.joor.Reflect
import java.util.WeakHashMap

class DisplayManagerProxy private constructor(context: Context) {
    companion object {
        private val cache = WeakHashMap<Context, DisplayManagerProxy>()

        operator fun invoke(context: Context): DisplayManagerProxy {
            return cache.getOrPut(context) { DisplayManagerProxy(context) }
        }
    }

    private val displayManager = context.getSystemService(DisplayManager::class.java)!!
    private val displayManagerGlobalReflect =
        Reflect.onClass("android.hardware.display.DisplayManagerGlobal").call("getInstance")
    private val displayManagerReflect = Reflect.on(displayManager)

    init {
        val service = displayManagerGlobalReflect.get<Any>("mDm")
        ToggleableBinderProxy.wrap(service)
    }

    @SuppressLint("MissingPermission")
    @EnableBinderProxy
    fun getDefaultDisplayBrightnessGammaPercentage(): Float {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val brightnessInfo = display.brightnessInfo
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

        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val brightnessInfo = display.brightnessInfo ?: return

        val brightness = BrightnessUtils.convertGammaPercentageToLinear(
            value, brightnessInfo.brightnessMinimum, brightnessInfo.brightnessMaximum
        )
        Log.d("DisplayManagerProxy", "setDefaultDisplayBrightnessPercentage: $brightness")

        displayManagerReflect.call("setBrightness", display.displayId, brightness)
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
