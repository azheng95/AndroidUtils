package com.azheng.androidutils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.Surface
import android.view.View
import android.view.WindowManager

/**
 * Android 屏幕工具类（兼容最新API）
 * 注意：部分功能需要对应权限或系统级授权
 */
object ScreenUtils {
    /* 基础屏幕信息获取 */
    @SuppressLint("ObsoleteSdkInt", "NewApi")
    // 获取物理屏幕宽度（px）
    fun getScreenWidth(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // ✅ 推荐方案：使用WindowMetrics（API 30+）
            Utils.getApplication().getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds.width()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // ⚠️ 过渡方案：兼容API 17-29设备
            val point = Point()
            (Utils.getApplication().getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.getRealSize(point)
            point.x
        } else {
            // ⚠️ 遗留方案：API 16及以下设备
            @Suppress("DEPRECATION")
            Utils.getApplication().resources.displayMetrics.widthPixels
        }
    }

    // 获取物理屏幕高度（px）
    fun getScreenHeight(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // ✅ 推荐方案：使用WindowMetrics（API 30+）
            Utils.getApplication().getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds.height()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // ⚠️ 过渡方案：兼容API 17-29设备
            val point = Point()
            (Utils.getApplication().getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .defaultDisplay.getRealSize(point)
            point.y
        } else {
            // ⚠️ 遗留方案：API 16及以下设备
            @Suppress("DEPRECATION")
            Utils.getApplication().resources.displayMetrics.heightPixels
        }
    }

    fun getAppScreenWidth(): Int {
        val wm =
            Utils.getApplication().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                ?: return -1
        val point = Point()
        wm.defaultDisplay.getSize(point)
        return point.x
    }

    fun getAppScreenHeight(): Int {
        val wm =
            Utils.getApplication().getSystemService(Context.WINDOW_SERVICE) as WindowManager
                ?: return -1
        val point = Point()
        wm.defaultDisplay.getSize(point)
        return point.y
    }


    // 获取应用Activity可用区域宽度（px）
    fun getActivityScreenWidth(activity: Activity): Int {
        return activity.resources.displayMetrics.widthPixels
    }

    // 获取应用Activity可用区域高度（px）
    fun getActivityScreenHeight(activity: Activity): Int {
        return activity.resources.displayMetrics.heightPixels
    }

    // 获取屏幕密度
    fun getScreenDensity(): Float {
        return Utils.getApplication().resources.displayMetrics.density
    }

    // 获取屏幕密度DPI
    fun getScreenDensityDpi(): Int {
        return Utils.getApplication().resources.displayMetrics.densityDpi
    }

    /* 屏幕显示模式控制 */

    // 设置全屏模式
    fun setFullScreen(activity: Activity) {
        activity.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    // 退出全屏模式
    fun setNonFullScreen(activity: Activity) {
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    // 切换全屏状态
    fun toggleFullScreen(activity: Activity) {
        if (isFullScreen(activity)) setNonFullScreen(activity) else setFullScreen(activity)
    }

    // 判断是否全屏
    fun isFullScreen(activity: Activity): Boolean {
        return (activity.window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
    }

    /* 屏幕方向控制 */

    fun setLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun setPortrait(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun isLandscape(): Boolean {
        return Utils.getApplication().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun isPortrait(): Boolean {
        return Utils.getApplication().resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    // 获取屏幕旋转角度（返回0/90/180/270）
    fun getScreenRotation(activity: Activity): Int {
        return when (activity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    /* 其他功能 */

    // 截屏功能（需要READ_FRAME_BUFFER权限）
    // region 其他优化功能
    @SuppressLint("NewApi")
    fun screenShot(activity: Activity): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val bounds = getSystemService<WindowManager>()!!
                .currentWindowMetrics.bounds
            val bitmap =
                Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
            PixelCopy.request(activity.window, bounds, bitmap, {}, Handler(Looper.getMainLooper()))
            bitmap
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.rootView.let {
                it.isDrawingCacheEnabled = true
                it.buildDrawingCache()
                it.drawingCache?.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    }

    // 判断是否锁屏
    fun isScreenLock(): Boolean {
        val keyguardManager =
            Utils.getApplication().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return keyguardManager.isKeyguardLocked
    }

    // 设置休眠时长（需要WRITE_SETTINGS权限）
    @SuppressLint("MissingPermission")
    fun setSleepDuration(millis: Long) {
        Settings.System.putLong(
            Utils.getApplication().contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            millis
        )
    }

    // 获取当前休眠时长
    fun getSleepDuration(): Long {
        return Settings.System.getLong(
            Utils.getApplication().contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            0
        )
    }

    // region 基础屏幕信息获取
    private inline fun <reified T> getSystemService(): T? {
        return Utils.getApplication().getSystemService(T::class.java)
    }

    private fun getWindowManager(): WindowManager? {
        return getSystemService()
    }

    private fun getRealSizeLegacy(): Point {
        return Point().apply {
            getWindowManager()?.defaultDisplay?.getRealSize(this)
        }
    }
}
