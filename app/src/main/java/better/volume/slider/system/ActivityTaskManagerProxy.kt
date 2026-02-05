package better.volume.slider.system

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import better.volume.slider.EnableBinderProxy
import better.volume.slider.ToggleableBinderProxy
import org.joor.Reflect

class ActivityTaskManagerProxy(context: Context) {
    @SuppressLint("WrongConstant")
    val activityTaskManager: Reflect =
        context.getSystemService("activity_task").run(Reflect::on)

    init {
        val service = activityTaskManager.call("getService").get<Any>()
        ToggleableBinderProxy.wrap(service)
    }

    data class Task(val app: String, val activityName: ComponentName)

    @EnableBinderProxy
    fun getForegroundTask(): Task? {
        val tasks = activityTaskManager.call("getTasks", 1)
            .get<List<ActivityManager.RunningTaskInfo>>()
        if (tasks.isEmpty()) {
            return null
        }

        val taskInfo = tasks[0]
        val topActivity = taskInfo.topActivity ?: return null
        val topActivityInfo =
            Reflect.on(taskInfo).get<ActivityInfo?>("topActivityInfo") ?: return null

        return Task(topActivityInfo.packageName, topActivity)
    }
}