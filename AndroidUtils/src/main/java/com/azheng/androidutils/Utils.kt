package com.azheng.androidutils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Application工具类
 * 用于获取全局Application实例，安全管理Activity生命周期监听
 */
object Utils {

    // 原子布尔值保证多线程初始化安全
    private val isInitialized = AtomicBoolean(false)
    private val isListenerRegistered = AtomicBoolean(false) // 新增：标记监听是否已注册
    private val isUnregistered = AtomicBoolean(false)
    private lateinit var application: Application
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private val activeActivityCount = AtomicInteger(0)

    // 协程作用域：改为每次注册时重建，避免注销后失效
    private var mainCoroutineScope: CoroutineScope? = null
    private val delayUnregisterJob = AtomicReference<Job?>(null)

    /**
     * 初始化方法，在Application的onCreate中调用
     * @param app Application实例
     */
    fun init(app: Application) {
        if (isInitialized.compareAndSet(false, true)) {
            application = app
        }
        // 仅当监听未注册时才初始化，防止重复注册
        if (!isListenerRegistered.get() && !isUnregistered.get()) {
            initOtherUtils()
        }
    }

    /**
     * 获取Application实例
     */
    fun getApplication(): Application {
        check(isInitialized.get()) {
            "Utils未初始化，请在Application的onCreate方法中调用Utils.init(this)".trimIndent()
        }
        return application
    }

    /**
     * 获取ApplicationContext
     */
    fun getApplicationContext(): Context {
        return getApplication().applicationContext
    }

    /**
     * 手动注销ActivityLifecycleCallbacks（主动退出时调用）
     */
   private fun unregisterActivityLifecycleCallbacks() {
        if (isUnregistered.compareAndSet(false, true)) {
            // 1. 取消延迟注销任务
            delayUnregisterJob.get()?.cancel()
            delayUnregisterJob.set(null)

            // 2. 取消协程作用域（仅取消当前作用域）
            mainCoroutineScope?.cancel()
            mainCoroutineScope = null

            // 3. 注销生命周期监听
            lifecycleCallbacks?.let {
                application.unregisterActivityLifecycleCallbacks(it)
            }
            lifecycleCallbacks = null
            activeActivityCount.set(0)
            isListenerRegistered.set(false) // 重置注册标记
        }
    }

    private fun initOtherUtils() {
        // 1. 重建协程作用域（避免之前的作用域被cancel后失效）
        mainCoroutineScope = MainScope()

        // 2. 创建并保存生命周期监听实例
        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // 原子操作更新计数，避免竞态条件
                activeActivityCount.getAndIncrement()

                // 取消延迟注销任务（后台切前台时，终止未执行的注销）
                delayUnregisterJob.get()?.cancel()
                delayUnregisterJob.set(null)

                if (activity is AppCompatActivity && ActivityUtils.isAppInternalActivity(activity)) {
                    ActivityUtils.addActivity(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
                // 原子操作更新计数
                activeActivityCount.getAndDecrement()

                if (activity is AppCompatActivity && ActivityUtils.isAppInternalActivity(activity)) {
                    ActivityUtils.removeActivity(activity)
                }

                // 延迟注销：仅当无活跃Activity且未注销时触发
                if (activeActivityCount.get() <= 0 && !isUnregistered.get()) {
                    delayUnregisterJob.get()?.cancel() // 取消之前的任务
                    val job = mainCoroutineScope?.launch(Dispatchers.Main) {
                        delay(1000) // 延迟1秒
                        // 再次校验：确保仍无活跃Activity才注销
                        if (activeActivityCount.get() <= 0) {
                            unregisterActivityLifecycleCallbacks()
                        }
                    }
                    delayUnregisterJob.set(job)
                }

                // 任务被移除时直接注销
                if (isTaskRemoved(activity)) {
                    unregisterActivityLifecycleCallbacks()
                }
            }
        }

        // 3. 注册监听（仅注册一次）
        if (isListenerRegistered.compareAndSet(false, true)) {
            application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        }

    }

    /**
     * 判断Activity是否被移除任务栈
     */
    private fun isTaskRemoved(activity: Activity): Boolean {
        return try {
            val method = Activity::class.java.getMethod("isTaskRemoved")
            method.invoke(activity) as Boolean
        } catch (e: Exception) {
            false
        }
    }



    /**
     * 重置状态（可选：用于APP重启后重新初始化）
     */
    fun reset() {
        unregisterActivityLifecycleCallbacks()
        isInitialized.set(false)
        isUnregistered.set(false)
        isListenerRegistered.set(false)
    }
}