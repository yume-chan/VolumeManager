package moe.chensi.volume.system

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
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

    /**
     * Sets default display brightness.
     *
     * @param value Brightness in [0f, 1f].
     * Uses reflection to call DisplayManager hidden setBrightness API.
     */
    @EnableBinderProxy
    fun setDefaultDisplayBrightness(value: Float) {
        displayManagerReflect.call("setBrightness", Display.DEFAULT_DISPLAY, value)
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
