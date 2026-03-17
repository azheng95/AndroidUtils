package com.azheng.androidutils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager

/**
 * Android 屏幕工具类（兼容 API 21+，部分方法兼容更低版本）
 *
 * ## 功能概览
 * - 获取屏幕尺寸信息（物理尺寸、应用可用区域、Activity区域）
 * - 屏幕密度相关信息获取
 * - 全屏模式控制（沉浸式体验）
 * - 屏幕方向控制与检测
 * - 屏幕截图功能
 * - 锁屏状态检测
 * - 休眠时长设置
 * - 屏幕常亮控制
 *
 * ## 权限要求
 * - 休眠时长设置：需要 `WRITE_SETTINGS` 权限并引导用户手动授权
 *
 * ## 使用前提
 * - 需确保 `Utils.getApplication()` 已正确初始化
 *
 * @author azheng
 * @since 1.0
 */
object ScreenUtils {

    // =====================================================================
    // region 基础屏幕信息获取
    // =====================================================================

    /**
     * 获取物理屏幕宽度（像素）
     *
     * 返回设备屏幕的实际物理宽度，**包含**系统装饰区域（状态栏、导航栏等）
     *
     * @return 屏幕宽度（px）
     *
     * ### 示例
     * ```kotlin
     * val screenWidth = ScreenUtils.getScreenWidth()
     * Log.d("Screen", "物理屏幕宽度: $screenWidth px")
     * ```
     */
    fun getScreenWidth(): Int {
        return getScreenSize().x
    }

    /**
     * 获取物理屏幕高度（像素）
     *
     * 返回设备屏幕的实际物理高度，**包含**系统装饰区域（状态栏、导航栏等）
     *
     * @return 屏幕高度（px）
     *
     * ### 示例
     * ```kotlin
     * val screenHeight = ScreenUtils.getScreenHeight()
     * Log.d("Screen", "物理屏幕高度: $screenHeight px")
     * ```
     */
    fun getScreenHeight(): Int {
        return getScreenSize().y
    }

    /**
     * 获取应用窗口宽度（像素）
     *
     * 返回应用可见区域的宽度，**不包含**被系统UI占用的区域
     *
     * @return 应用窗口宽度（px），获取失败返回 -1
     *
     * ### 与 [getScreenWidth] 的区别
     * - `getScreenWidth()`: 返回完整物理屏幕宽度
     * - `getAppScreenWidth()`: 返回应用实际可用宽度（排除导航栏等）
     */
    fun getAppScreenWidth(): Int {
        return getAppScreenSize().x.takeIf { it > 0 } ?: -1
    }

    /**
     * 获取应用窗口高度（像素）
     *
     * 返回应用可见区域的高度，**不包含**被系统UI占用的区域
     *
     * @return 应用窗口高度（px），获取失败返回 -1
     *
     * ### 与 [getScreenHeight] 的区别
     * - `getScreenHeight()`: 返回完整物理屏幕高度
     * - `getAppScreenHeight()`: 返回应用实际可用高度（排除状态栏、导航栏等）
     */
    fun getAppScreenHeight(): Int {
        return getAppScreenSize().y.takeIf { it > 0 } ?: -1
    }

    /**
     * 获取 Activity 可用区域宽度（像素）
     *
     * 基于 Activity 的 `DisplayMetrics` 获取，适用于需要针对特定 Activity 计算布局的场景
     *
     * @param activity 目标 Activity
     * @return Activity 可用宽度（px）
     */
    fun getActivityScreenWidth(activity: Activity): Int {
        return activity.resources.displayMetrics.widthPixels
    }

    /**
     * 获取 Activity 可用区域高度（像素）
     *
     * 基于 Activity 的 `DisplayMetrics` 获取，适用于需要针对特定 Activity 计算布局的场景
     *
     * @param activity 目标 Activity
     * @return Activity 可用高度（px）
     */
    fun getActivityScreenHeight(activity: Activity): Int {
        return activity.resources.displayMetrics.heightPixels
    }

    /**
     * 获取屏幕密度比例
     *
     * density = dpi / 160，用于 dp 与 px 之间的转换
     *
     * | 密度等级 | DPI   | density |
     * |---------|-------|---------|
     * | ldpi    | 120   | 0.75    |
     * | mdpi    | 160   | 1.0     |
     * | hdpi    | 240   | 1.5     |
     * | xhdpi   | 320   | 2.0     |
     * | xxhdpi  | 480   | 3.0     |
     * | xxxhdpi | 640   | 4.0     |
     *
     * @return 屏幕密度比例值
     *
     * ### 示例：dp 转 px
     * ```kotlin
     * val px = (dp * ScreenUtils.getScreenDensity()).toInt()
     * ```
     */
    fun getScreenDensity(): Float {
        return Utils.getApplication().resources.displayMetrics.density
    }

    /**
     * 获取屏幕密度 DPI
     *
     * 每英寸像素点数（Dots Per Inch）
     *
     * @return 屏幕 DPI 值（如 160、240、320、480 等）
     */
    fun getScreenDensityDpi(): Int {
        return Utils.getApplication().resources.displayMetrics.densityDpi
    }

    // endregion

    // =====================================================================
    // region 屏幕显示模式控制
    // =====================================================================

    /**
     * 设置全屏模式（沉浸式）
     *
     * 隐藏状态栏和导航栏，支持沉浸式体验。用户可通过从屏幕边缘滑动临时显示系统栏。
     *
     * ### 兼容性说明
     * - **API 30+**: 使用 `WindowInsetsController`（推荐）
     * - **API 30 以下**: 使用 `systemUiVisibility`（已废弃但仍有效）
     *
     * @param activity 目标 Activity
     *
     * ### 示例
     * ```kotlin
     * // 进入全屏
     * ScreenUtils.setFullScreen(this)
     *
     * // 退出全屏
     * ScreenUtils.setNonFullScreen(this)
     * ```
     */
    fun setFullScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 推荐方案：使用 WindowInsetsController
            activity.window.insetsController?.let { controller ->
                // 隐藏状态栏和导航栏
                controller.hide(
                    WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                )
                // 设置行为：从边缘滑动时临时显示，然后自动隐藏
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // API 30 以下兼容方案
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN                 // 隐藏状态栏
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // 隐藏导航栏
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE   // 保持布局稳定
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY // 沉浸式粘性模式
                    )
        }
    }

    /**
     * 退出全屏模式
     *
     * 恢复显示状态栏和导航栏
     *
     * @param activity 目标 Activity
     */
    fun setNonFullScreen(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 推荐方案
            activity.window.insetsController?.show(
                WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
            )
        } else {
            // API 30 以下兼容方案
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    /**
     * 切换全屏状态
     *
     * 如果当前是全屏则退出，否则进入全屏
     *
     * @param activity 目标 Activity
     */
    fun toggleFullScreen(activity: Activity) {
        if (isFullScreen(activity)) {
            setNonFullScreen(activity)
        } else {
            setFullScreen(activity)
        }
    }

    /**
     * 判断当前是否为全屏模式
     *
     * @param activity 目标 Activity
     * @return `true`: 全屏模式，`false`: 非全屏模式
     */
    fun isFullScreen(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+：检查状态栏是否可见
            val windowInsets = activity.window.decorView.rootWindowInsets
            windowInsets?.isVisible(WindowInsets.Type.statusBars()) == false
        } else {
            // API 30 以下：检查 systemUiVisibility 标志
            @Suppress("DEPRECATION")
            (activity.window.decorView.systemUiVisibility and
                    View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        }
    }

    // endregion

    // =====================================================================
    // region 屏幕方向控制
    // =====================================================================

    /**
     * 设置屏幕为横屏模式
     *
     * @param activity 目标 Activity
     *
     * ### 注意
     * - 如需锁定横屏，建议同时在 AndroidManifest.xml 中设置
     * - 设置后 Activity 会重建，需要做好状态保存
     */
    fun setLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    /**
     * 设置屏幕为竖屏模式
     *
     * @param activity 目标 Activity
     *
     * ### 注意
     * - 如需锁定竖屏，建议同时在 AndroidManifest.xml 中设置
     * - 设置后 Activity 会重建，需要做好状态保存
     */
    fun setPortrait(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    /**
     * 解锁屏幕方向（跟随系统/传感器）
     *
     * @param activity 目标 Activity
     */
    fun setOrientationUnspecified(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    /**
     * 判断当前是否为横屏
     *
     * @return `true`: 横屏，`false`: 非横屏
     */
    fun isLandscape(): Boolean {
        return Utils.getApplication().resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
    }

    /**
     * 判断当前是否为竖屏
     *
     * @return `true`: 竖屏，`false`: 非竖屏
     */
    fun isPortrait(): Boolean {
        return Utils.getApplication().resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
    }

    /**
     * 获取屏幕旋转角度
     *
     * @param activity 目标 Activity
     * @return 旋转角度：0°、90°、180°、270°
     *
     * ### 角度说明
     * - **0°**: 自然方向（通常为竖屏）
     * - **90°**: 逆时针旋转 90°
     * - **180°**: 旋转 180°（倒置）
     * - **270°**: 顺时针旋转 90°
     */
    @SuppressLint("NewApi")
    fun getScreenRotation(activity: Activity): Int {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 推荐方案
            activity.display?.rotation ?: Surface.ROTATION_0
        } else {
            // API 30 以下兼容方案
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay.rotation
        }

        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    // endregion

    // =====================================================================
    // region 截屏功能
    // =====================================================================

    /**
     * 截取当前 Activity 屏幕（异步方式，推荐）
     *
     * ### 兼容性说明
     * - **API 26+**: 使用 `PixelCopy`（推荐，支持硬件加速视图、SurfaceView 等）
     * - **API 26 以下**: 使用 `DrawingCache`（已废弃但可用，部分视图可能无法正确截取）
     *
     * @param activity 要截取的 Activity
     * @param callback 截图完成回调，参数为截取的 Bitmap，失败时为 null
     *
     * ### 示例
     * ```kotlin
     * ScreenUtils.screenShot(this) { bitmap ->
     *     bitmap?.let {
     *         imageView.setImageBitmap(it)
     *         // 或保存到文件
     *     } ?: run {
     *         Toast.makeText(this, "截图失败", Toast.LENGTH_SHORT).show()
     *     }
     * }
     * ```
     */
    fun screenShot(activity: Activity, callback: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+ 使用 PixelCopy（支持硬件加速和 SurfaceView）
            try {
                val window = activity.window
                val view = window.decorView
                val bitmap = Bitmap.createBitmap(
                    view.width,
                    view.height,
                    Bitmap.Config.ARGB_8888
                )
                val rect = Rect(0, 0, view.width, view.height)

                PixelCopy.request(
                    window,
                    rect,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            callback(bitmap)
                        } else {
                            callback(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        } else {
            // API 26 以下使用 DrawingCache
            @Suppress("DEPRECATION")
            val bitmap = try {
                activity.window.decorView.rootView.let { view ->
                    view.isDrawingCacheEnabled = true
                    view.buildDrawingCache(true)
                    val result = view.drawingCache?.copy(Bitmap.Config.ARGB_8888, false)
                    view.isDrawingCacheEnabled = false
                    result
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            callback(bitmap)
        }
    }

    /**
     * 截取指定 View 内容
     *
     * @param view 要截取的 View
     * @return 截取的 Bitmap，失败返回 null
     *
     * ### 示例
     * ```kotlin
     * val bitmap = ScreenUtils.captureView(myCustomView)
     * ```
     */
    fun captureView(view: View): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            view.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // endregion

    // =====================================================================
    // region 锁屏与休眠
    // =====================================================================

    /**
     * 判断屏幕是否处于锁定状态
     *
     * @return `true`: 已锁屏，`false`: 未锁屏
     */
    fun isScreenLock(): Boolean {
        return try {
            val keyguardManager = Utils.getApplication()
                .getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.isKeyguardLocked ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 设置屏幕休眠时长
     *
     * ### 权限要求
     * 需要 `WRITE_SETTINGS` 权限，并引导用户手动授权：
     * ```kotlin
     * if (!Settings.System.canWrite(context)) {
     *     val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
     *         data = Uri.parse("package:${context.packageName}")
     *     }
     *     startActivity(intent)
     * }
     * ```
     *
     * @param millis 休眠时长（毫秒），常用值：
     *
     * | 时长 | 毫秒值 |
     * |------|--------|
     * | 15秒 | 15000  |
     * | 30秒 | 30000  |
     * | 1分钟 | 60000 |
     * | 2分钟 | 120000 |
     * | 5分钟 | 300000 |
     * | 10分钟 | 600000 |
     * | 永不休眠 | Int.MAX_VALUE |
     *
     * @return `true`: 设置成功，`false`: 设置失败（通常是权限问题）
     */
    fun setSleepDuration(millis: Long): Boolean {
        return try {
            Settings.System.putLong(
                Utils.getApplication().contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                millis
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 获取当前屏幕休眠时长
     *
     * @return 休眠时长（毫秒），获取失败返回 0
     */
    fun getSleepDuration(): Long {
        return try {
            Settings.System.getLong(
                Utils.getApplication().contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    // endregion

    // =====================================================================
    // region 屏幕常亮控制
    // =====================================================================

    /**
     * 保持屏幕常亮
     *
     * 在当前 Activity 显示期间保持屏幕常亮，Activity 销毁后自动失效
     *
     * @param activity 目标 Activity
     *
     * ### 注意
     * - 仅在当前 Activity 有效
     * - 不需要额外权限
     * - 适用于视频播放、阅读等场景
     */
    fun keepScreenOn(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 取消屏幕常亮
     *
     * @param activity 目标 Activity
     */
    fun cancelKeepScreenOn(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 判断是否设置了屏幕常亮
     *
     * @param activity 目标 Activity
     * @return `true`: 已设置常亮，`false`: 未设置
     */
    fun isKeepScreenOn(activity: Activity): Boolean {
        return (activity.window.attributes.flags and
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
    }

    // endregion

    // =====================================================================
    // region 私有辅助方法
    // =====================================================================

    /**
     * 获取物理屏幕尺寸（包含系统装饰区域）
     *
     * @return 包含宽高的 Point 对象
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getScreenSize(): Point {
        return when {
            // API 30+ 推荐方案：使用 WindowMetrics
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val windowManager = Utils.getApplication()
                    .getSystemService(WindowManager::class.java)
                val bounds = windowManager.currentWindowMetrics.bounds
                Point(bounds.width(), bounds.height())
            }
            // API 17-29 过渡方案：使用 getRealSize
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                Point().apply {
                    @Suppress("DEPRECATION")
                    (Utils.getApplication()
                        .getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                        ?.defaultDisplay?.getRealSize(this)
                }
            }
            // API 16 及以下遗留方案
            else -> {
                val metrics = Utils.getApplication().resources.displayMetrics
                @Suppress("DEPRECATION")
                Point(metrics.widthPixels, metrics.heightPixels)
            }
        }
    }

    /**
     * 获取应用窗口尺寸（排除系统装饰区域）
     *
     * @return 包含宽高的 Point 对象
     */
    @SuppressLint("ObsoleteSdkInt", "NewApi")
    private fun getAppScreenSize(): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ 推荐方案：计算排除系统装饰后的尺寸
            try {
                val windowManager = Utils.getApplication()
                    .getSystemService(WindowManager::class.java)
                val metrics = windowManager.currentWindowMetrics
                val windowInsets = metrics.windowInsets

                // 获取需要排除的 Insets（导航栏、刘海屏等）
                val insets = windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars() or
                            WindowInsets.Type.displayCutout()
                )

                Point(
                    metrics.bounds.width() - insets.left - insets.right,
                    metrics.bounds.height() - insets.top - insets.bottom
                )
            } catch (e: Exception) {
                Point()
            }
        } else {
            // API 30 以下方案：使用 getSize
            Point().apply {
                @Suppress("DEPRECATION")
                (Utils.getApplication()
                    .getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                    ?.defaultDisplay?.getSize(this)
            }
        }
    }
}
