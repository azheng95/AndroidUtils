package com.azheng.androidutils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Activity管理工具类
 * 负责管理Activity栈和生命周期监听
 *
 * 特性：
 * - 自动管理Activity生命周期，无需手动添加/移除
 * - 支持同一Class的多个实例
 * - 线程安全
 * - 自动处理任务栈清理（如 FLAG_ACTIVITY_CLEAR_TASK）
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
     * 使用同步List保证线程安全，保持插入顺序，支持同一Class的多个实例
     */
    private val activities: MutableList<AppCompatActivity> =
        Collections.synchronizedList(mutableListOf())

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
                    addActivityInternal(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                if (activity is AppCompatActivity) {
                    removeActivityInternal(activity)
                    // 定期检查列表中是否有已销毁但未移除的 Activity
                    cleanupDestroyedActivities()
                }
            }
        }

        if (isListenerRegistered.compareAndSet(false, true)) {
            app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        }
    }
    private fun cleanupDestroyedActivities() {
        synchronized(activities) {
            activities.removeAll { it.isDestroyed }
        }
    }
    /**
     * 重置状态 - 仅用于测试
     *
     * ⚠️ 注意：正常使用中不需要调用此方法！
     *
     * 以下场景会自动处理，无需手动重置：
     * - FLAG_ACTIVITY_CLEAR_TASK 重启界面
     * - 用户按返回键退出后再进入
     * - 进程被系统杀死后恢复
     */
    @VisibleForTesting
    internal fun reset() {
        // 先注销回调，再清空引用
        lifecycleCallbacks?.let {
            application?.unregisterActivityLifecycleCallbacks(it)
        }
        lifecycleCallbacks = null

        synchronized(activities) {
            activities.clear()
        }

        // 最后重置标志位和application引用
        isListenerRegistered.set(false)
        isInitialized.set(false)
        application = null
    }

    // ==================== 内部Activity栈管理方法 ====================

    /**
     * 内部添加Activity（基于实例引用，支持同类多实例）
     */
    private fun addActivityInternal(activity: AppCompatActivity) {
        synchronized(activities) {
            // 避免重复添加同一实例
            if (!activities.contains(activity)) {
                activities.add(activity)
            }
        }
    }

    /**
     * 内部移除指定Activity实例
     */
    private fun removeActivityInternal(activity: AppCompatActivity) {
        synchronized(activities) {
            activities.remove(activity)
        }
    }

    // ==================== 公开的Activity栈管理方法 ====================

    /**
     * 移除指定Class的所有Activity实例（仅从管理列表移除，不finish）
     */
    fun <T : AppCompatActivity> removeActivitiesByClass(clz: Class<T>?) {
        clz ?: return
        synchronized(activities) {
            activities.removeAll { it.javaClass == clz }
        }
    }

    /**
     * 判断指定Class的Activity是否存在（任意一个存活即返回true）
     */
    fun <T : AppCompatActivity> isActivityExist(clz: Class<T>?): Boolean {
        clz ?: return false
        return synchronized(activities) {
            activities.any { it.javaClass == clz && it.isAlive() }
        }
    }

    /**
     * 获取指定Class的第一个Activity实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AppCompatActivity> getActivity(clazz: Class<T>?): T? {
        clazz ?: return null
        return synchronized(activities) {
            activities.firstOrNull { it.javaClass == clazz && it.isAlive() } as? T
        }
    }

    /**
     * 获取指定Class的所有Activity实例
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AppCompatActivity> getActivitiesByClass(clazz: Class<T>?): List<T> {
        clazz ?: return emptyList()
        return synchronized(activities) {
            activities.filter { it.javaClass == clazz && it.isAlive() }.toList() as List<T>
        }
    }

    /**
     * 判断Activity实例是否已添加到管理列表
     */
    fun isActivityAdded(activity: AppCompatActivity?): Boolean {
        activity ?: return false
        return synchronized(activities) {
            activities.contains(activity)
        }
    }

    /**
     * 判断指定Class是否有Activity在管理列表中
     */
    fun <T : AppCompatActivity> isClassAdded(clz: Class<T>?): Boolean {
        clz ?: return false
        return synchronized(activities) {
            activities.any { it.javaClass == clz }
        }
    }

    /**
     * 结束指定Activity实例
     * 注意：从列表移除由onActivityDestroyed回调自动处理
     */
    fun finishActivity(activity: AppCompatActivity?) {
        activity ?: return
        if (activity.isAlive()) {
            activity.finish()
        }
    }

    /**
     * 结束指定Class的所有Activity实例
     */
    fun <T : AppCompatActivity> finishActivitiesByClass(clz: Class<T>?) {
        clz ?: return
        val toFinish: List<AppCompatActivity>
        synchronized(activities) {
            toFinish = activities.filter { it.javaClass == clz && it.isAlive() }.toList()
        }
        toFinish.forEach { it.finish() }
    }

    /**
     * 结束指定Class的第一个Activity（栈底）
     */
    fun <T : AppCompatActivity> finishFirstActivity(clz: Class<T>?) {
        clz ?: return
        val activity: AppCompatActivity?
        synchronized(activities) {
            activity = activities.firstOrNull { it.javaClass == clz && it.isAlive() }
        }
        activity?.finish()
    }

    /**
     * 结束指定Class的最后一个Activity（栈顶）
     */
    fun <T : AppCompatActivity> finishLastActivity(clz: Class<T>?) {
        clz ?: return
        val activity: AppCompatActivity?
        synchronized(activities) {
            activity = activities.lastOrNull { it.javaClass == clz && it.isAlive() }
        }
        activity?.finish()
    }

    /**
     * 结束所有Activity
     * @param removeFromRecents 是否同时从最近任务中移除，默认false
     */
    @JvmOverloads
    fun finishAllActivity(removeFromRecents: Boolean = false) {
        val toFinish: List<AppCompatActivity>
        synchronized(activities) {
            if (activities.isEmpty()) return
            toFinish = activities.filter { it.isAlive() }.reversed().toList()
        }
        toFinish.forEach {
            if (removeFromRecents) {
                it.finishAndRemoveTask()
            } else {
                it.finish()
            }
        }
    }

    /**
     * 结束所有Activity除指定实例
     */
    fun finishAllExcept(activity: AppCompatActivity?) {
        activity ?: return
        val toFinish: List<AppCompatActivity>
        synchronized(activities) {
            if (activities.isEmpty()) return
            toFinish = activities.filter { it !== activity && it.isAlive() }.toList()
        }
        toFinish.forEach { it.finish() }
    }

    /**
     * 结束所有Activity除指定Class的所有实例
     */
    fun <T : AppCompatActivity> finishAllExceptClass(clz: Class<T>?) {
        clz ?: return
        val toFinish: List<AppCompatActivity>
        synchronized(activities) {
            if (activities.isEmpty()) return
            toFinish = activities.filter { it.javaClass != clz && it.isAlive() }.toList()
        }
        toFinish.forEach { it.finish() }
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
                ?: return getTopActivity()

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
            getTopActivity()
        } catch (e: Exception) {
            e.printStackTrace()
            getTopActivity()
        }
    }

    /**
     * 获取栈顶Activity（最后添加的存活Activity）
     */
    fun getTopActivity(): AppCompatActivity? {
        return synchronized(activities) {
            activities.lastOrNull { it.isAlive() }
        }
    }

    /**
     * 获取栈底Activity（最先添加的存活Activity）
     */
    fun getBottomActivity(): AppCompatActivity? {
        return synchronized(activities) {
            activities.firstOrNull { it.isAlive() }
        }
    }

    /**
     * 获取指定Class的实例数量
     */
    fun <T : AppCompatActivity> getActivityCount(clz: Class<T>?): Int {
        clz ?: return 0
        return synchronized(activities) {
            activities.count { it.javaClass == clz && it.isAlive() }
        }
    }

    /**
     * 获取管理列表中Activity总数
     */
    fun getActivityCount(): Int {
        return synchronized(activities) {
            activities.count { it.isAlive() }
        }
    }

    /**
     * 判断Activity是否为应用内Activity
     * 通过包名校验，支持多模块项目（子模块包名以主包名为前缀）
     */
    fun isAppInternalActivity(activity: Activity?): Boolean {
        activity ?: return false
        val app = application ?: return false
        val appPackageName = app.packageName
        val activityPackageName = activity.javaClass.`package`?.name ?: return false
        return activityPackageName.startsWith(appPackageName)
    }

    /**
     * 获取Activity列表（返回不可修改的List副本）
     */
    fun getActivities(): List<AppCompatActivity> {
        return synchronized(activities) {
            activities.toList()
        }
    }
}
