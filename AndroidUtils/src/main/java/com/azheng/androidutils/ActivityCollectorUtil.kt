package com.azheng.androidutils

import android.annotation.TargetApi
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.lang.reflect.InvocationTargetException

/**
 * Activity 收集管理器
 * 用于管理应用中所有的 Activity
 */
object ActivityCollectorUtil {

    /**
     * 存放 activity 的映射表
     */
    private val activities = LinkedHashMap<Class<*>, AppCompatActivity>()

    /**
     * 添加 Activity
     * @param activity 要添加的 Activity 实例
     * @param clz Activity 的类
     */
    fun addActivity(activity: AppCompatActivity, clz: Class<*>) {
        activities[clz] = activity
    }

    /**
     * 添加 Activity 的扩展函数版本
     */
    fun AppCompatActivity.addToCollector() {
        addActivity(this, this.javaClass)
    }

    /**
     * 判断一个 Activity 是否存在
     * @param clz Activity 的类
     * @return 如果 Activity 存在且未销毁返回 true，否则返回 false
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun <T : AppCompatActivity> isActivityExist(clz: Class<T>): Boolean {
        val activity = getActivity(clz) ?: return false
        return !(activity.isFinishing || activity.isDestroyed)
    }

    /**
     * 获得指定 activity 实例
     * @param clazz Activity 的类对象
     * @return 对应的 Activity 实例，如果不存在则返回 null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : AppCompatActivity> getActivity(clazz: Class<T>): T? {
        return activities[clazz] as? T
    }

    /**
     * 获取指定 Activity 的扩展函数版本
     */
    inline fun <reified T : AppCompatActivity> getActivityOfType(): T? {
        return getActivity(T::class.java)
    }

    /**
     * 判断 Activity 是否已经创建
     * @param clz Activity 的类
     * @return 如果已创建返回 true，否则返回 false
     */
    fun isAddActivity(clz: Class<*>): Boolean {
        return activities.containsKey(clz)
    }

    /**
     * 移除 activity
     * @param activity 要移除的 Activity
     */
    fun removeActivity(activity: AppCompatActivity) {
        if (activities.containsValue(activity)) {
            activities.remove(activity.javaClass)
        }
    }

    /**
     * 移除 Activity 的扩展函数版本
     */
    fun AppCompatActivity.removeFromCollector() {
        removeActivity(this)
    }

    /**
     * 结束并移除 activity
     * @param activity 要结束的 Activity
     */
    fun finishActivity(activity: AppCompatActivity) {
        if (activities.containsValue(activity)) {
            activities.remove(activity.javaClass)
        }
        activity.finish()
    }

    /**
     * 结束 Activity 的扩展函数版本
     */
    fun AppCompatActivity.finishAndRemove() {
        finishActivity(this)
    }

    /**
     * 移除所有的 Activity
     */
    fun removeAllActivity() {
        activities.values.forEach { activity ->
            if (!activity.isFinishing) {
                activity.finish()
            }
        }
        activities.clear()
    }

    /**
     * 移除所有 Activity 除当前 activity
     * @param activity 要保留的 Activity
     */
    fun removeAllExceptThisActivity(activity: AppCompatActivity) {
        activities.values.forEach { 
            if (!it.isFinishing && it != activity) {
                it.finish()
            }
        }
        activities.clear()
        addActivity(activity, activity.javaClass)
    }

    /**
     * 保留当前 Activity 的扩展函数版本
     */
    fun AppCompatActivity.removeAllExceptThis() {
        removeAllExceptThisActivity(this)
    }

    /**
     * 获取当前 Activity
     * @return 当前正在运行的 Activity，如果获取失败返回 null
     */
    fun getCurrentActivity(): AppCompatActivity? {
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val activitiesField = activityThreadClass.getDeclaredField("mActivities").apply {
                isAccessible = true
            }
            
            val activities = activitiesField.get(activityThread) as Map<*, *>

            for (activityRecord in activities.values) {
                val activityRecordClass = activityRecord?.javaClass ?: continue
                val pausedField = activityRecordClass.getDeclaredField("paused").apply {
                    isAccessible = true
                }
                if (!pausedField.getBoolean(activityRecord)) {
                    val activityField = activityRecordClass.getDeclaredField("activity").apply {
                        isAccessible = true
                    }
                    return activityField.get(activityRecord) as? AppCompatActivity
                }
            }

        } catch (e: Exception) {
            when (e) {
                is ClassNotFoundException,
                is InvocationTargetException,
                is NoSuchMethodException,
                is NoSuchFieldException,
                is IllegalAccessException -> e.printStackTrace()
            }
        }
        return null
    }

    /**
     * 获取所有已注册的 Activity
     * @return 包含所有 Activity 的映射表
     */
    fun getActivities(): Map<Class<*>, AppCompatActivity> {
        return activities.toMap()
    }
}
