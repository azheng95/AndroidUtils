package com.azheng.androidutils.strings

import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Locale

/**
 * 字符串提供者接口
 * 遵循依赖倒置原则，方便测试和扩展
 */
interface IStringProvider {

    fun getString(@StringRes resId: Int): String

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String

    fun getString(@StringRes resId: Int, locale: Locale): String

    fun getString(@StringRes resId: Int, locale: Locale, vararg formatArgs: Any): String

    fun getStringArray(@ArrayRes resId: Int): Array<String>

    fun getQuantityString(@PluralsRes resId: Int, quantity: Int): String

    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any): String

    fun getCurrentLocale(): Locale
}
