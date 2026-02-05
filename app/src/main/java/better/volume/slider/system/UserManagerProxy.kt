package better.volume.slider.system

import android.content.Context
import android.os.IBinder
import android.os.UserManager
import org.joor.Reflect
import rikka.shizuku.ShizukuBinderWrapper

class UserManagerProxy(context: Context) {
    private val userManager = Reflect.on(context.getSystemService(UserManager::class.java)!!)

    init {
        // Can't use `userManager.field("mService")` as it will have type of `IUserManager`
        // instead of concrete type `IUserManager.Stub.Proxy`
        val mService = Reflect.on(userManager.get<Any>("mService"))
        val mRemote = mService.get<IBinder>("mRemote")
        val wrapper = mRemote as? ShizukuBinderWrapper ?: ShizukuBinderWrapper(mRemote)
        mService.set("mRemote", wrapper)
    }

    fun getUserIds(): List<Int> {
        return userManager.call("getUsers").get<List<Any>>()
            .map { value -> Reflect.on(value).get("id") }
    }
}