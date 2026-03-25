package moe.chensi.volume.system

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import moe.chensi.volume.EnableBinderProxy
import moe.chensi.volume.ToggleableBinderProxy
import org.joor.Reflect
import java.util.WeakHashMap

class PackageManagerProxy private constructor(context: Context) {
    companion object {
        private val cache = WeakHashMap<Context, PackageManagerProxy>()

        private const val MATCH_ANY_USER = 0x00000400

        fun get(context: Context): PackageManagerProxy {
            return cache.getOrPut(context) { PackageManagerProxy(context) }
        }
    }

    private val userManager = UserManagerProxy(context)

    private val packageManager = context.packageManager
    private val reflect = Reflect.on(packageManager)

    init {
        val service =
            Reflect.onClass("android.app.ActivityThread").call("getPackageManager").get<Any>()
        ToggleableBinderProxy.wrap(service)
    }

    val defaultActivityIcon by lazy { packageManager.defaultActivityIcon }

    val defaultActivityIconImageBitmap by lazy {
        defaultActivityIcon.toBitmap(128, 128).asImageBitmap()
    }

    @EnableBinderProxy
    fun getInstalledPackagesForAllUsers(): List<PackageInfo> {
        return packageManager.getInstalledPackages(MATCH_ANY_USER or PackageManager.GET_ACTIVITIES)
    }

    @EnableBinderProxy
    fun getDrawable(packageName: String, resId: Int, appInfo: ApplicationInfo): Drawable? {
        return packageManager.getDrawable(packageName, resId, appInfo)
    }

    @EnableBinderProxy
    fun loadLabel(appInfo: ApplicationInfo): String {
        return appInfo.loadLabel(packageManager).toString()
    }
}