package com.azheng.androidutils

import android.app.Application
import android.content.Context

/**
 * Application工具类
 * 用于获取全局Application实例
 */
object Utils {

    private lateinit var application: Application

    /**
     * 初始化方法，在Application的onCreate中调用
     * @param app Application实例
     */
    fun init(app: Application) {
        if (!::application.isInitialized) {
            application = app
        }
    }

    /**
     * 获取Application实例
     * @return Application实例
     */
    fun getApplication(): Application {
        if (!::application.isInitialized) {
            throw IllegalStateException("AppUtils未初始化，请在Application的onCreate方法中调用AppUtils.init(this)")
        }
        return application
    }

    /**
     * 获取ApplicationContext
     * @return ApplicationContext
     */
    fun getApplicationContext(): Context {
        return getApplication().applicationContext
    }
}
