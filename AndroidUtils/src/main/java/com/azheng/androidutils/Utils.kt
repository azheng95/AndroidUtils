package com.azheng.androidutils

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.azheng.androidutils.strings.StringUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application工具类
 * 用于获取全局Application实例
 *
 * 使用方式：
 * ```kotlin
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Utils.init(this)
 *     }
 * }
 * ```
 *
 * 注意：初始化后无需任何手动管理，Activity生命周期会自动处理
 */
object Utils {

    private val isInitialized = AtomicBoolean(false)
    private var application: Application? = null

    /**
     * 初始化方法，在Application的onCreate中调用
     * @param app Application实例
     *
     * 注意：
     * - 仅需调用一次，重复调用会被忽略
     * - 进程重启后会自动重新初始化（因为静态变量会被清空）
     */
    fun init(app: Application) {
        if (isInitialized.compareAndSet(false, true)) {
            application = app
            // 每次都尝试初始化ActivityUtils（内部自行判断是否需要注册）
            ActivityUtils.init(app)
            // 初始化StringManager
            StringUtils.getInstance().init(
                application = app,
                contextProvider = {
                    // 优先返回当前Activity的Context（用于语言切换场景）
                    // 如果没有Activity，则返回Application Context
                    ActivityUtils.getTopActivity() ?: app
                }
            )
        }
    }

    /**
     * 获取Application实例
     */
    fun getApplication(): Application {
        return application ?: throw IllegalStateException(
            "Utils未初始化，请在Application的onCreate方法中调用Utils.init(this)"
        )
    }

    /**
     * 获取ApplicationContext
     */
    fun getApplicationContext(): Context {
        return getApplication().applicationContext
    }

    /**
     * 判断是否已初始化
     */
    fun isInitialized(): Boolean = isInitialized.get()

    /**
     * 重置所有工具类状态 - 仅用于测试
     *
     * ⚠️ 此方法仅供单元测试使用！
     *
     * 正常使用中不需要调用此方法，以下场景都会自动处理：
     * - 使用 FLAG_ACTIVITY_CLEAR_TASK 重启界面 → onActivityDestroyed 自动清理
     * - 用户按返回键退出后再进入 → 状态保持，无需处理
     * - 进程被系统杀死后恢复 → 静态变量自动清空，init() 重新初始化
     *
     * @see VisibleForTesting
     */
    @VisibleForTesting
    fun reset() {
        ActivityUtils.reset()
        application = null
        isInitialized.set(false)
    }
}
