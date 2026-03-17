package com.azheng.androidutils.strings

import android.content.Context
import androidx.annotation.StringRes
import java.util.Locale
import java.util.regex.Pattern

// ==================== 资源相关扩展 ====================

/**
 * Context扩展 - 获取指定Locale的字符串
 */
fun Context.getLocalizedString(@StringRes resId: Int, locale: Locale): String {
    return LocaleHelper.getLocalizedContext(this, locale).getString(resId)
}

/**
 * Int(ResId)扩展 - 直接获取字符串
 */
fun Int.asString(): String = StringUtils.get(this)

/**
 * Int(ResId)扩展 - 获取格式化字符串
 */
fun Int.asString(vararg args: Any): String = StringUtils.get(this, *args)

// ==================== 字符串通用扩展 ====================

/**
 * 判断字符串是否为空或空白
 */
fun String?.isNullOrBlankExt(): Boolean = this.isNullOrBlank()

/**
 * 安全的字符串截取
 */
fun String.safeSubstring(startIndex: Int, endIndex: Int = this.length): String {
    if (this.isEmpty()) return this
    val start = startIndex.coerceIn(0, this.length)
    val end = endIndex.coerceIn(start, this.length)
    return this.substring(start, end)
}

/**
 * 省略显示（超过指定长度添加省略号）
 */
fun String.ellipsize(maxLength: Int, ellipsis: String = "..."): String {
    return if (this.length <= maxLength) this
    else this.take(maxLength - ellipsis.length) + ellipsis
}

/**
 * 首字母大写
 */
fun String.capitalizeFirst(): String {
    return if (this.isEmpty()) this
    else this[0].uppercaseChar() + this.substring(1)
}

/**
 * 驼峰转下划线
 */
fun String.camelToSnake(): String {
    return this.replace(Regex("([a-z])([A-Z])")) {
        "${it.groupValues[1]}_${it.groupValues[2]}"
    }.lowercase()
}

/**
 * 下划线转驼峰
 */
fun String.snakeToCamel(): String {
    return this.split("_").mapIndexed { index, s ->
        if (index == 0) s.lowercase()
        else s.lowercase().capitalizeFirst()
    }.joinToString("")
}

/**
 * 移除所有空白字符
 */
fun String.removeAllWhitespace(): String = this.replace("\\s".toRegex(), "")

/**
 * 检查是否为有效邮箱
 */
fun String.isValidEmail(): Boolean {
    val emailPattern = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
    )
    return emailPattern.matcher(this).matches()
}

/**
 * 检查是否为有效手机号（中国大陆）
 */
fun String.isValidChinesePhone(): Boolean {
    return this.matches(Regex("^1[3-9]\\d{9}$"))
}

/**
 * 手机号脱敏（中间4位替换为****）
 */
fun String.maskPhone(): String {
    return if (this.length == 11) {
        this.replaceRange(3, 7, "****")
    } else this
}

/**
 * 邮箱脱敏
 */
fun String.maskEmail(): String {
    val atIndex = this.indexOf('@')
    return if (atIndex > 2) {
        this.replaceRange(1, atIndex - 1, "***")
    } else this
}

/**
 * 半角转全角
 */
fun String.toFullWidth(): String {
    val chars = this.toCharArray()
    for (i in chars.indices) {
        when {
            chars[i].code == 32 -> chars[i] = 12288.toChar() // 空格
            chars[i].code in 33..126 -> chars[i] = (chars[i].code + 65248).toChar()
        }
    }
    return String(chars)
}

/**
 * 全角转半角
 */
fun String.toHalfWidth(): String {
    val chars = this.toCharArray()
    for (i in chars.indices) {
        when {
            chars[i].code == 12288 -> chars[i] = 32.toChar() // 空格
            chars[i].code in 65281..65374 -> chars[i] = (chars[i].code - 65248).toChar()
        }
    }
    return String(chars)
}

/**
 * 提取字符串中的数字
 */
fun String.extractNumbers(): String = this.replace(Regex("[^0-9]"), "")

/**
 * 提取字符串中的字母
 */
fun String.extractLetters(): String = this.replace(Regex("[^a-zA-Z]"), "")

/**
 * 字符串模板替换
 * 例如: "Hello {name}, you are {age} years old".template(mapOf("name" to "John", "age" to "25"))
 */
fun String.template(params: Map<String, Any?>): String {
    var result = this
    params.forEach { (key, value) ->
        result = result.replace("{$key}", value?.toString() ?: "")
    }
    return result
}

/**
 * 将字符串按指定分隔符分割并trim
 */
fun String.splitAndTrim(delimiter: String = ","): List<String> {
    return this.split(delimiter).map { it.trim() }.filter { it.isNotEmpty() }
}

/**
 * 反转字符串
 */
fun String.reverse(): String = this.reversed()

/**
 * 重复字符串n次
 */
fun String.repeatTimes(n: Int): String = this.repeat(n.coerceAtLeast(0))

/**
 * 检查字符串是否只包含中文
 */
fun String.isChineseOnly(): Boolean = this.matches(Regex("^[\\u4e00-\\u9fa5]+$"))

/**
 * 检查字符串是否包含中文
 */
fun String.containsChinese(): Boolean = this.contains(Regex("[\\u4e00-\\u9fa5]"))

/**
 * 获取字符串的字节长度（UTF-8）
 */
fun String.byteLength(): Int = this.toByteArray(Charsets.UTF_8).size

/**
 * 安全的toInt，失败返回默认值
 */
fun String?.toIntSafe(default: Int = 0): Int = this?.toIntOrNull() ?: default

/**
 * 安全的toLong，失败返回默认值
 */
fun String?.toLongSafe(default: Long = 0L): Long = this?.toLongOrNull() ?: default

/**
 * 安全的toDouble，失败返回默认值
 */
fun String?.toDoubleSafe(default: Double = 0.0): Double = this?.toDoubleOrNull() ?: default
