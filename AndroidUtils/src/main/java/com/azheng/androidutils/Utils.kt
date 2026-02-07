package com.azheng.androidutils

import android.app.Application
import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Application工具类
 * 用于获取全局Application实例
 */
object Utils {

    private val isInitialized = AtomicBoolean(false)
    private lateinit var application: Application

    /**
     * 初始化方法，在Application的onCreate中调用
     * @param app Application实例
     */
    fun init(app: Application) {
        if (isInitialized.compareAndSet(false, true)) {
            application = app
        }
        // 每次都尝试初始化ActivityUtils（内部自行判断是否需要注册）
        ActivityUtils.init(app)
    }

    /**
     * 获取Application实例
     */
    fun getApplication(): Application {
        check(isInitialized.get()) {
            "Utils未初始化，请在Application的onCreate方法中调用Utils.init(this)"
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
     * ⚠️ 重置所有工具类状态
     *
     * **警告：此方法会清空所有 Activity 管理状态！**
     *
     * 仅在以下场景使用：
     * - 用户登出后重新初始化
     * - 单元测试隔离
     * - 进程异常重启恢复
     *
     * 使用示例：
     * ```kotlin
     * @OptIn(DangerousApi::class)
     * fun logout() {
     *     Utils.reset()
     *     Utils.init(app) // 必须重新初始化！
     * }
     * ```
     */
    @DangerousApi
    fun reset() {
        ActivityUtils.reset()
        isInitialized.set(false)
    }
}
// 定义注解
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "这是危险操作，会清空所有状态。仅在登出/测试/进程重启时使用！"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class DangerousApi
