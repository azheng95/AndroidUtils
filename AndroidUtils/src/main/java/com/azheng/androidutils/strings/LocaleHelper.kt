package com.azheng.androidutils.strings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 语言切换辅助类
 * 负责创建指定Locale的Context
 */
object LocaleHelper {

    /**
     * 根据Locale获取对应的Context
     */
    fun getLocalizedContext(baseContext: Context, locale: Locale): Context {
        val config = Configuration(baseContext.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
            baseContext.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            baseContext.createConfigurationContext(config)
        }
    }

    /**
     * 获取系统当前Locale
     */
    fun getSystemLocale(context: Context): Locale {
        val config = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }

    /**
     * 获取应用当前Locale（可能被用户手动设置）
     */
    fun getAppLocale(context: Context): Locale {
        return context.resources.configuration.let { config ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.locales[0]
            } else {
                @Suppress("DEPRECATION")
                config.locale
            }
        }
    }
}
