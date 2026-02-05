package better.volume.slider

import android.os.Build
import android.os.IBinder
import android.os.Parcel
import androidx.annotation.RequiresApi
import com.flyjingfish.android_aop_annotation.ProceedJoinPoint
import com.flyjingfish.android_aop_annotation.anno.AndroidAopPointCut
import com.flyjingfish.android_aop_annotation.base.BasePointCut
import org.joor.Reflect
import rikka.shizuku.ShizukuBinderWrapper
import java.util.concurrent.Executor

@AndroidAopPointCut(EnableBinderProxyCut::class)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableBinderProxy()

class EnableBinderProxyCut : BasePointCut<EnableBinderProxy> {
    override fun invoke(joinPoint: ProceedJoinPoint, anno: EnableBinderProxy): Any? {
        return ToggleableBinderProxy.withEnabled { joinPoint.proceed() }
    }
}

class ToggleableBinderProxy(private val base: IBinder) : IBinder by base {
    companion object {
        private val _enabled: ThreadLocal<Boolean> = ThreadLocal<Boolean>.withInitial { false }

        var enabled: Boolean
            get() = _enabled.get()!!
            set(value) = _enabled.set(value)

        fun <T> withEnabled(block: () -> T): T {
            val prev = enabled
            try {
                enabled = true
                return block()
            } finally {
                enabled = prev
            }
        }

        fun wrap(proxy: Any) {
            Reflect.on(proxy).apply {
                set("mRemote", get<IBinder>("mRemote").run {
                    this as? ToggleableBinderProxy ?: ToggleableBinderProxy(this)
                })
            }
        }
    }

    private val shizukuWrapper = ShizukuBinderWrapper(base)

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun addFrozenStateChangeCallback(
        executor: Executor,
        callback: IBinder.FrozenStateChangeCallback
    ) {
        base.addFrozenStateChangeCallback(executor, callback)
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    override fun removeFrozenStateChangeCallback(callback: IBinder.FrozenStateChangeCallback): Boolean {
        return base.removeFrozenStateChangeCallback(callback)
    }

    override fun transact(
        code: Int, data: Parcel, reply: Parcel?, flags: Int
    ): Boolean {
        return (if (enabled) shizukuWrapper else base).transact(code, data, reply, flags)
    }
}