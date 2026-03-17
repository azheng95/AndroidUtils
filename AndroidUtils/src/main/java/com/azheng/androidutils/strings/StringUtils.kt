package com.azheng.androidutils.strings

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 字符串工具 - 单例模式
 *
 * 特性：
 * 1. 支持多语言动态切换
 * 2. 支持格式化字符串
 * 3. 支持指定Locale获取字符串
 * 4. 缓存机制优化性能
 * 5. 线程安全
 */
class StringUtils private constructor() : IStringProvider {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: StringUtils? = null

        @JvmStatic
        fun getInstance(): StringUtils {
            return instance ?: synchronized(this) {
                instance ?: StringUtils().also { instance = it }
            }
        }

        /**
         * 便捷访问方法
         */
        @JvmStatic
        fun get(@StringRes resId: Int): String = getInstance().getString(resId)

        @JvmStatic
        fun get(@StringRes resId: Int, vararg args: Any): String =
            getInstance().getString(resId, *args)

        @JvmStatic
        fun getWithLocale(@StringRes resId: Int, locale: Locale): String =
            getInstance().getString(resId, locale)
    }

    // 使用Application Context，避免内存泄漏
    private var appContext: Context? = null

    // Context提供者，支持动态获取最新Context（语言切换后）
    private var contextProvider: (() -> Context)? = null

    // 当前自定义Locale（可选）
    @Volatile
    private var customLocale: Locale? = null

    // 缓存已创建的Locale Context
    private val localeContextCache = ConcurrentHashMap<Locale, WeakReference<Context>>()

    // 初始化标志
    private val isInitialized = AtomicBoolean(false)

    /**
     * 初始化 - 在Application中调用
     *
     * @param application Application实例
     * @param contextProvider 可选的Context提供者，用于获取最新的Context（语言切换场景）
     */
    fun init(
        application: Application,
        contextProvider: (() -> Context)? = null
    ) {
        // 防止重复初始化
        if (isInitialized.compareAndSet(false, true)) {
            this.appContext = application.applicationContext
            this.contextProvider = contextProvider
        }
    }

    /**
     * 设置自定义Locale（用于APP内切换语言）
     */
    fun setCustomLocale(locale: Locale?) {
        this.customLocale = locale
        // 清空缓存，确保使用新Locale
        localeContextCache.clear()
    }

    /**
     * 获取当前使用的Context
     */
    private fun getCurrentContext(): Context {
        checkInitialized()

        // 优先使用contextProvider获取最新Context
        val baseContext = contextProvider?.invoke() ?: requireAppContext()

        // 如果设置了自定义Locale，创建对应的Context
        return customLocale?.let { locale ->
            getOrCreateLocalizedContext(baseContext, locale)
        } ?: baseContext
    }

    /**
     * 获取Application Context，确保非空
     */
    private fun requireAppContext(): Context {
        return appContext ?: throw IllegalStateException(
            "StringManager not initialized! Please call StringManager.getInstance().init(application) in your Application class."
        )
    }

    /**
     * 获取或创建指定Locale的Context（带缓存）
     */
    private fun getOrCreateLocalizedContext(baseContext: Context, locale: Locale): Context {
        // 尝试从缓存获取
        localeContextCache[locale]?.get()?.let { return it }

        // 创建新的Context并缓存
        val localizedContext = LocaleHelper.getLocalizedContext(baseContext, locale)
        localeContextCache[locale] = WeakReference(localizedContext)
        return localizedContext
    }

    private fun checkInitialized() {
        check(isInitialized.get()) {
            "StringManager not initialized! Please call StringManager.getInstance().init(application) in your Application class."
        }
    }

    // ==================== IStringProvider 接口实现 ====================

    override fun getString(@StringRes resId: Int): String {
        return getCurrentContext().getString(resId)
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return getCurrentContext().getString(resId, *formatArgs)
    }

    override fun getString(@StringRes resId: Int, locale: Locale): String {
        checkInitialized()
        val localizedContext = getOrCreateLocalizedContext(requireAppContext(), locale)
        return localizedContext.getString(resId)
    }

    override fun getString(
        @StringRes resId: Int,
        locale: Locale,
        vararg formatArgs: Any
    ): String {
        checkInitialized()
        val localizedContext = getOrCreateLocalizedContext(requireAppContext(), locale)
        return localizedContext.getString(resId, *formatArgs)
    }

    override fun getStringArray(@ArrayRes resId: Int): Array<String> {
        return getCurrentContext().resources.getStringArray(resId)
    }

    override fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String {
        return getCurrentContext().resources.getQuantityString(resId, quantity)
    }

    override fun getQuantityString(
        @PluralsRes resId: Int,
        quantity: Int,
        vararg formatArgs: Any
    ): String {
        return getCurrentContext().resources.getQuantityString(resId, quantity, *formatArgs)
    }

    override fun getCurrentLocale(): Locale {
        return customLocale ?: LocaleHelper.getAppLocale(getCurrentContext())
    }

    // ==================== 额外实用方法 ====================

    /**
     * 安全获取字符串，资源不存在时返回默认值
     */
    fun getStringSafely(@StringRes resId: Int, default: String = ""): String {
        return try {
            getString(resId)
        } catch (e: Exception) {
            default
        }
    }

    /**
     * 根据资源名称获取字符串（动态资源场景）
     */
    fun getStringByName(name: String, defType: String = "string"): String? {
        val context = getCurrentContext()
        val resId = context.resources.getIdentifier(name, defType, context.packageName)
        return if (resId != 0) getString(resId) else null
    }

    /**
     * 批量获取字符串
     */
    fun getStrings(vararg resIds: Int): List<String> {
        return resIds.map { getString(it) }
    }

    /**
     * 获取带HTML标签的字符串
     */
    fun getHtmlString(@StringRes resId: Int): CharSequence {
        val htmlText = getString(resId)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            android.text.Html.fromHtml(htmlText)
        }
    }

}
