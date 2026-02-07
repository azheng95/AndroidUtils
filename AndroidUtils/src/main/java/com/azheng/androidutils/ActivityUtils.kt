package com.azheng.androidutils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Activity管理工具类
 * 负责管理Activity栈和生命周期监听
 */
object ActivityUtils {

    // ==================== 生命周期监听相关 ====================

    private val isInitialized = AtomicBoolean(false)
    private val isListenerRegistered = AtomicBoolean(false)
    private var application: Application? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    // ==================== Activity栈管理相关 ====================

    /**
     * 存放应用内Activity的列表（仅存储AppCompatActivity）
     * 使用同步的LinkedHashMap保证线程安全和插入顺序
     */
    private val activities: MutableMap<Class<*>, AppCompatActivity> =
        Collections.synchronizedMap(LinkedHashMap())

    // ==================== 扩展函数 ====================

    /**
     * 判断Activity是否存活（未finishing且未destroyed）
     */
    private fun AppCompatActivity.isAlive(): Boolean {
        return !isFinishing && !isDestroyed
    }

    // ==================== 初始化方法 ====================

    /**
     * 初始化方法，由Utils.init()调用
     * @param app Application实例
     */
    internal fun init(app: Application) {
        if (isInitialized.compareAndSet(false, true)) {
            application = app
        }
        // 未注册则注册
        if (!isListenerRegistered.get()) {
            registerLifecycleCallbacks()
        }
    }

    /**
     * 注册生命周期监听
     */
    private fun registerLifecycleCallbacks() {
        val app = application ?: return

        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is AppCompatActivity && isAppInternalActivity(activity)) {
                    addActivity(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity is AppCompatActivity && isAppInternalActivity(activity)) {
                    removeActivity(activity)
                }
            }
        }

        if (isListenerRegistered.compareAndSet(false, true)) {
            app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }

    /**
     * 重置状态（用于APP重启后重新初始化）
     */
    internal fun reset() {
        // 注销生命周期监听
        lifecycleCallbacks?.let {
            application?.unregisterActivityLifecycleCallbacks(it)
        }
        lifecycleCallbacks = null

        // 清空Activity列表
        synchronized(activities) {
            activities.clear()
        }

        // 重置状态标志
        isInitialized.set(false)
        isListenerRegistered.set(false)
        application = null
    }

    // ==================== Activity栈管理方法 ====================

    /**
     * 添加Activity（仅需传入Activity实例）
     */
    fun addActivity(activity: AppCompatActivity?) {
        activity ?: return
        if (isAppInternalActivity(activity)) {
            synchronized(activities) {
                activities[activity.javaClass] = activity
            }
        }
    }

    /**
     * 判断Activity是否存在（且未销毁）
     */
    fun <T : AppCompatActivity> isActivityExist(clz: Class<T>?): Boolean {
        return getActivity(clz)?.isAlive() == true
    }

    /**
     * 获得指定activity实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AppCompatActivity> getActivity(clazz: Class<T>?): T? {
        clazz ?: return null
        return synchronized(activities) {
            activities[clazz] as? T
        }
    }

    /**
     * 判断Activity是否已添加到管理列表
     */
    fun <T : AppCompatActivity> isAddActivity(clz: Class<T>?): Boolean {
        clz ?: return false
        return synchronized(activities) {
            activities.containsKey(clz)
        }
    }

    /**
     * 移除指定Class的Activity
     */
    fun <T : AppCompatActivity> removeActivity(clz: Class<T>?) {
        clz ?: return
        synchronized(activities) {
            activities.remove(clz)
        }
    }

    /**
     * 重载：通过Activity实例移除
     */
    fun removeActivity(activity: AppCompatActivity?) {
        activity ?: return
        removeActivity(activity.javaClass)
    }

    /**
     * 结束指定Class的Activity
     */
    fun <T : AppCompatActivity> finishActivity(clz: Class<T>?) {
        clz ?: return
        val activity = getActivity(clz)?.takeIf { it.isAlive() } ?: return
        removeActivity(clz)
        activity.finish()
    }

    /**
     * 重载：通过Activity实例结束
     */
    fun finishActivity(activity: AppCompatActivity?) {
        activity ?: return
        finishActivity(activity.javaClass)
    }

    /**
     * 结束所有Activity并清空管理列表
     */
    fun finishAllActivity() {
        synchronized(activities) {
            if (activities.isEmpty()) return

            // 逆序结束Activity（更符合任务栈逻辑）
            activities.values.reversed().forEach { activity ->
                if (activity.isAlive()) {
                    activity.finishAndRemoveTask()
                }
            }
            activities.clear()
        }
    }

    /**
     * 结束所有Activity除指定Class的Activity
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AppCompatActivity> finishAllExceptThisActivity(clz: Class<T>?) {
        clz ?: return

        synchronized(activities) {
            if (activities.isEmpty()) return

            val keepActivity = activities[clz] as? T ?: run {
                // 如果指定的Activity不存在，结束所有
                activities.values.reversed().forEach { activity ->
                    if (activity.isAlive()) {
                        activity.finishAndRemoveTask()
                    }
                }
                activities.clear()
                return
            }

            // 结束除指定Activity外的所有Activity
            activities.entries.toList().forEach { (activityClz, activity) ->
                if (activityClz != clz && activity.isAlive()) {
                    activity.finish()
                }
            }

            activities.clear()
            activities[clz] = keepActivity
        }
    }

    /**
     * 重载：通过Activity实例保留
     */
    fun finishAllExceptThisActivity(activity: AppCompatActivity?) {
        activity ?: return
        finishAllExceptThisActivity(activity.javaClass)
    }

    /**
     * 获取当前活跃的应用内AppCompatActivity
     */
    fun getCurrentActivity(): AppCompatActivity? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod = activityThreadClass.getMethod("currentActivityThread")
            val activityThread = currentActivityThreadMethod.invoke(null)

            val activitiesField = activityThreadClass.getDeclaredField("mActivities")
            activitiesField.isAccessible = true
            val systemActivities = activitiesField.get(activityThread) as? Map<*, *>
                ?: return getLatestActivityFromLocal()

            for (activityRecord in systemActivities.values) {
                activityRecord ?: continue

                val activityRecordClass = activityRecord.javaClass

                val pausedField = activityRecordClass.getDeclaredField("paused")
                    .apply { isAccessible = true }
                if (pausedField.getBoolean(activityRecord)) continue

                val activityField = activityRecordClass.getDeclaredField("activity")
                    .apply { isAccessible = true }
                val activity = activityField.get(activityRecord) as? Activity ?: continue

                if (isAppInternalActivity(activity) && activity is AppCompatActivity) {
                    return activity
                }
            }
            getLatestActivityFromLocal()
        } catch (e: Exception) {
            e.printStackTrace()
            getLatestActivityFromLocal()
        }
    }

    /**
     * 从本地管理列表中获取最新的Activity（兜底方案）
     */
    private fun getLatestActivityFromLocal(): AppCompatActivity? {
        return synchronized(activities) {
            if (activities.isEmpty()) return null
            activities.values.lastOrNull { it.isAlive() }
        }
    }

    /**
     * 判断Activity是否为应用内Activity（通过包名校验）
     */
    fun isAppInternalActivity(activity: Activity?): Boolean {
        activity ?: return false
        val appPackageName = activity.applicationContext.packageName
        val activityPackageName = activity.javaClass.`package`?.name ?: return false
        return activityPackageName.startsWith(appPackageName)
    }

    /**
     * 判断Activity是否为应用内Activity（仅传入Class）
     */
    fun <T : Activity> isAppInternalActivity(clz: Class<T>?): Boolean {
        clz ?: return false
        val appContext = getApplicationContext() ?: return false
        val appPackageName = appContext.packageName
        val activityPackageName = clz.`package`?.name ?: return false
        return activityPackageName.startsWith(appPackageName)
    }

    /**
     * 获取Activity列表（返回不可修改的Map副本）
     */
    fun getActivities(): Map<Class<*>, AppCompatActivity> {
        return synchronized(activities) {
            activities.toMap()
        }
    }

    /**
     * 获取全局Application Context（兜底方案）
     */
    private fun getApplicationContext(): Context? {
        // 优先使用已初始化的application
        application?.let { return it.applicationContext }

        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThreadMethod: Method = activityThreadClass.getMethod("currentActivityThread")
            val activityThread = currentActivityThreadMethod.invoke(null)
            val appContextField: Field = activityThreadClass.getDeclaredField("mInitialApplication")
            appContextField.isAccessible = true
            appContextField.get(activityThread) as? Context
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
