package moe.chensi.volume.system

import android.app.NotificationManager
import android.content.Context
import moe.chensi.volume.EnableBinderProxy
import moe.chensi.volume.ToggleableBinderProxy
import org.joor.Reflect
import java.util.WeakHashMap

class NotificationManagerProxy private constructor(context: Context) {
    companion object {
        private val cache = WeakHashMap<Context, NotificationManagerProxy>()

        operator fun invoke(context: Context): NotificationManagerProxy {
            return cache.getOrPut(context) { NotificationManagerProxy(context) }
        }
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)!!

    init {
        val service = Reflect.onClass(NotificationManager::class.java).call("getService").get<Any>()
        ToggleableBinderProxy.wrap(service)
    }

    @EnableBinderProxy
    fun getCurrentInterruptionFilter(): Int {
        return notificationManager.currentInterruptionFilter
    }

    @EnableBinderProxy
    fun setInterruptionFilter(filter: Int) {
        notificationManager.setInterruptionFilter(filter)
    }
}
