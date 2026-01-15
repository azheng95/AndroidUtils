package com.azheng.androidutils

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * Description: 管理所有的栈中的Activity
 */
class ActivityUtils private constructor() {

    companion object {
        /**
         * 存放应用内Activity的列表（仅存储AppCompatActivity）
         * Kotlin中用mutableMapOf + LinkedHashMap保证插入顺序
         */
        private val activities: MutableMap<Class<*>, AppCompatActivity> = LinkedHashMap()

        /**
         * 添加Activity（仅需传入Activity实例）
         */
        fun addActivity(activity: AppCompatActivity?) {
            activity ?: return
            if (isAppInternalActivity(activity)) {
                activities[activity.javaClass] = activity
            }
        }

        /**
         * 判断Activity是否存在（且未销毁）
         * 仅需传入Activity::class
         */
        fun <T : AppCompatActivity> isActivityExist(clz: Class<T>?): Boolean {
            clz ?: return false
            val activity = getActivity(clz)
            activity ?: return false
            return !(activity.isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed))
        }

        /**
         * 获得指定activity实例（仅应用内的AppCompatActivity）
         */
        @Suppress("UNCHECKED_CAST")
        fun <T : AppCompatActivity> getActivity(clazz: Class<T>?): T? {
            clazz ?: return null
            return activities[clazz] as T?
        }

        /**
         * 判断Activity是否已添加到管理列表
         */
        fun <T : AppCompatActivity> isAddActivity(clz: Class<T>?): Boolean {
            clz ?: return false
            return activities.containsKey(clz)
        }

        /**
         * 移除指定Class的Activity（仅需传入Activity::class）
         */
        fun <T : AppCompatActivity> removeActivity(clz: Class<T>?) {
            clz ?: return
            activities.remove(clz)
        }

        /**
         * 重载：通过Activity实例移除（兼容原有调用）
         */
        fun removeActivity(activity: AppCompatActivity?) {
            activity ?: return
            removeActivity(activity.javaClass)
        }

        /**
         * 结束指定Class的Activity（仅需传入Activity::class）
         */
        fun <T : AppCompatActivity> finishActivity(clz: Class<T>?) {
            clz ?: return
            val activity = getActivity(clz) ?: return
            if (activity.isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed)) {
                return
            }
            removeActivity(clz)
            activity.finish()
        }

        /**
         * 重载：通过Activity实例结束（兼容原有调用）
         */
        fun finishActivity(activity: AppCompatActivity?) {
            activity ?: return
            finishActivity(activity.javaClass)
        }

        /**
         * 结束所有Activity并清空管理列表
         */
        fun finishAllActivity() {
            if (activities.isEmpty()) return
            // 1. 逆序结束Activity（更符合任务栈逻辑，避免异常）
            activities.values.reversed().forEach { activity ->
                if (!activity.isFinishing && !activity.isDestroyed) {
                    // 高版本用finishAndRemoveTask，彻底移除任务栈
                    activity.finishAndRemoveTask()
                }
            }
            activities.clear()
        }

        /**
         * 结束所有Activity除指定Class的Activity（仅需传入Activity::class）
         */
        fun <T : AppCompatActivity> finishAllExceptThisActivity(clz: Class<T>?) {
            clz ?: return
            if (activities.isEmpty()) return

            // 保存需要保留的Activity实例
            val keepActivity = getActivity(clz) ?: run {
                finishAllActivity()
                return
            }

            // 遍历并结束其他Activity
            activities.entries.forEach { (activityClz, _) ->
                if (activityClz != clz) {
                    finishActivity(activityClz as Class<out AppCompatActivity>)
                }
            }

            activities.clear()
            addActivity(keepActivity)
        }

        /**
         * 重载：通过Activity实例保留（兼容原有调用）
         */
        fun finishAllExceptThisActivity(activity: AppCompatActivity?) {
            activity ?: return
            finishAllExceptThisActivity(activity.javaClass)
        }

        /**
         * 获取当前活跃的应用内AppCompatActivity
         */
        /**
         * 获取当前活跃的应用内AppCompatActivity
         */
        fun getCurrentActivity(): AppCompatActivity? {
            return try {
                // 1. 通过反射获取ActivityThread中的mActivities
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentActivityThreadMethod: Method = activityThreadClass.getMethod("currentActivityThread")
                val activityThread = currentActivityThreadMethod.invoke(null)

                val activitiesField: Field = activityThreadClass.getDeclaredField("mActivities")
                activitiesField.isAccessible = true
                val systemActivities: Map<*, *> = activitiesField.get(activityThread) as Map<*, *>

                // 2. 遍历所有ActivityRecord，找到符合条件的Activity
                // 修复点：处理activityRecord的可空性（Any? -> Any）
                systemActivities.values.forEach { activityRecord ->
                    // 第一步：判空，空则跳过当前循环
                    activityRecord ?: return@forEach

                    // 第二步：安全获取Class（非空断言，因为已判空）
                    val activityRecordClass = activityRecord.javaClass

                    // 反射获取paused字段并判断
                    val pausedField: Field = try {
                        activityRecordClass.getDeclaredField("paused")
                    } catch (e: NoSuchFieldException) {
                        // 字段不存在则跳过
                        return@forEach
                    }
                    pausedField.isAccessible = true
                    val isPaused = try {
                        pausedField.getBoolean(activityRecord)
                    } catch (e: IllegalAccessException) {
                        // 无法访问字段则视为暂停，跳过
                        true
                    }
                    if (isPaused) return@forEach

                    // 获取Activity实例
                    val activityField: Field = try {
                        activityRecordClass.getDeclaredField("activity")
                    } catch (e: NoSuchFieldException) {
                        // 字段不存在则跳过
                        return@forEach
                    }
                    activityField.isAccessible = true
                    val activity = try {
                        activityField.get(activityRecord) as Activity?
                    } catch (e: IllegalAccessException) {
                        // 无法访问字段则返回null
                        null
                    }

                    // 三重校验：非空 + 应用内Activity + 是AppCompatActivity子类
                    if (activity != null && isAppInternalActivity(activity) && activity is AppCompatActivity) {
                        return activity
                    }
                }
                // 反射未找到，兜底获取本地最新Activity
                getLatestActivityFromLocal()
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                getLatestActivityFromLocal()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                getLatestActivityFromLocal()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                getLatestActivityFromLocal()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                getLatestActivityFromLocal()
            } catch (e: NoSuchFieldException) {
                e.printStackTrace()
                getLatestActivityFromLocal()
            }
        }
        /**
         * 从本地管理列表中获取最新的Activity（兜底方案）
         */
        private fun getLatestActivityFromLocal(): AppCompatActivity? {
            if (activities.isEmpty()) return null

            var latestActivity: AppCompatActivity? = null
            activities.values.forEach { activity ->
                if (activity != null && !activity.isFinishing && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed)) {
                    latestActivity = activity
                }
            }
            return latestActivity
        }

        /**
         * 判断Activity是否为应用内Activity（通过包名校验）
         * 重载1：传入Activity实例
         */
        fun isAppInternalActivity(activity: Activity?): Boolean {
            activity ?: return false
            val appPackageName = getAppPackageName(activity)
            return appPackageName == activity.packageName
        }

        /**
         * 判断Activity是否为应用内Activity（通过包名校验）
         * 重载2：仅传入Activity::class
         */
        fun <T : Activity> isAppInternalActivity(clz: Class<T>?): Boolean {
            clz ?: return false
            val appContext = getApplicationContext() ?: return false
            val appPackageName = appContext.packageName
            val activityPackageName = clz.`package`?.name ?: return false
            return appPackageName == activityPackageName
        }

        /**
         * 获取Activity列表（返回不可修改的Map，防止外部篡改）
         */
        fun getActivities(): Map<Class<*>, AppCompatActivity> {
            // Kotlin中通过toMap()转为不可变Map
            return activities.toMap()
        }

        /**
         * 获取应用包名（工具方法，避免重复代码）
         */
        private fun getAppPackageName(context: Context): String {
            return context.applicationContext.packageName
        }

        /**
         * 获取全局Application Context（兜底方案）
         */
        private fun getApplicationContext(): Context? {
            return try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentActivityThreadMethod: Method = activityThreadClass.getMethod("currentActivityThread")
                val activityThread = currentActivityThreadMethod.invoke(null)
                val appContextField: Field = activityThreadClass.getDeclaredField("mInitialApplication")
                appContextField.isAccessible = true
                appContextField.get(activityThread) as Context
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}