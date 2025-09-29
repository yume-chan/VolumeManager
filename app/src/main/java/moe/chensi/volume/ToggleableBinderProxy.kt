package moe.chensi.volume

import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import com.flyjingfish.android_aop_annotation.ProceedJoinPoint
import com.flyjingfish.android_aop_annotation.anno.AndroidAopPointCut
import com.flyjingfish.android_aop_annotation.base.BasePointCut
import org.joor.Reflect
import rikka.shizuku.ShizukuBinderWrapper
import java.io.FileDescriptor

@AndroidAopPointCut(EnableBinderProxyCut::class)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableBinderProxy()

class EnableBinderProxyCut : BasePointCut<EnableBinderProxy> {
    override fun invoke(joinPoint: ProceedJoinPoint, anno: EnableBinderProxy): Any? {
        return ToggleableBinderProxy.withEnabled { joinPoint.proceed() }
    }
}

class ToggleableBinderProxy(private val base: IBinder) : IBinder {
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

    override fun getInterfaceDescriptor(): String? {
        return base.interfaceDescriptor
    }

    override fun pingBinder(): Boolean {
        return base.pingBinder()
    }

    override fun isBinderAlive(): Boolean {
        return base.isBinderAlive
    }

    override fun queryLocalInterface(descriptor: String): IInterface? {
        return base.queryLocalInterface(descriptor)
    }

    override fun dump(fd: FileDescriptor, args: Array<out String?>?) {
        return base.dump(fd, args)
    }

    override fun dumpAsync(
        fd: FileDescriptor, args: Array<out String?>?
    ) {
        return base.dumpAsync(fd, args)
    }

    override fun transact(
        code: Int, data: Parcel, reply: Parcel?, flags: Int
    ): Boolean {
        return (if (enabled) shizukuWrapper else base).transact(code, data, reply, flags)
    }

    override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) {
        return base.linkToDeath(recipient, flags)
    }

    override fun unlinkToDeath(
        recipient: IBinder.DeathRecipient, flags: Int
    ): Boolean {
        return base.unlinkToDeath(recipient, flags)
    }
}