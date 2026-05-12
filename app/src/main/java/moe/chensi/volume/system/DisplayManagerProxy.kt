package moe.chensi.volume.system

import android.content.Context
import android.hardware.display.DisplayManager
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
    private val displayManagerReflect = Reflect.on(displayManager)

    init {
        val service = Reflect.onClass("android.hardware.display.DisplayManagerGlobal")
            .call("getInstance")
            .get<Any>()
            .run(Reflect::on)
            .get<Any>("mDm")
        ToggleableBinderProxy.wrap(service)
    }

    @EnableBinderProxy
    fun getDefaultDisplayBrightness(): Float {
        return displayManagerReflect.call("getBrightness", Display.DEFAULT_DISPLAY).get()
    }

    @EnableBinderProxy
    fun getDefaultDisplayMaxBrightness(): Float {
        return displayManager.getBrightnessInfo(Display.DEFAULT_DISPLAY)?.brightnessMaximum ?: 1f
    }

    @EnableBinderProxy
    fun setDefaultDisplayBrightness(value: Float) {
        displayManagerReflect.call("setBrightness", Display.DEFAULT_DISPLAY, value)
    }
}
