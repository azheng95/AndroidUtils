package com.azheng.androidutils

/**
 * 通用数据校验工具类
 * 功能：检查各种数据类型的非空、有效内容等情况
 */
object ValidationUtils {
    /**
     * 校验Boolean是否有效（非null且为true）
     */
    fun isValid(value: Boolean?): Boolean {
        return value != null && value
    }

    /**
     * 校验字符串是否有效（非null/非空/非纯空格）
     * @param value 需要校验的字符串
     * @return 有效返回true，无效返回false
     */
    fun isValid(value: String?): Boolean {
        return !value.isNullOrBlank()
    }

    /**
     * 校验Int是否有效（非null/-1）
     * @param value 需要校验的整型
     */
    fun isValid(value: Int?): Boolean {
        return value != null && value != -1
    }

    /**
     * 校验Long是否有效（非null/-1L）
     * @param value 需要校验的长整型
     */
    fun isValid(value: Long?): Boolean {
        return value != null && value != -1L
    }

    /**
     * 校验Float是否有效（非null/-1f/非NaN）
     * @param value 需要校验的浮点数
     */
    fun isValid(value: Float?): Boolean {
        return value != null && value != -1f && !value.isNaN()
    }

    /**
     * 校验集合是否有效（非null/非空）
     * @param collection 需要校验的集合
     */
    fun <T> isValid(collection: Collection<T>?): Boolean {
        return !collection.isNullOrEmpty()
    }

    /**
     * 通用对象校验（支持嵌套校验）
     * @param obj 需要校验的对象
     * @return 有效返回true，无效返回false
     */
    fun isValid(obj: Any?): Boolean {
        return when (obj) {
            null -> false
            is Boolean -> isValid(obj)
            is String -> isValid(obj)
            is Int -> isValid(obj)
            is Long -> isValid(obj)
            is Float -> isValid(obj)
            is Collection<*> -> isValid(obj)
            else -> true // 非基本类型默认有效
        }
    }

    // 扩展函数版本（更符合Kotlin风格）
    fun Boolean?.isValidExt(): Boolean = this != null && this
    fun String?.isValidExt(): Boolean = !this.isNullOrBlank()
    fun Int?.isValidExt(): Boolean = this != null && this != -1
    fun Long?.isValidExt(): Boolean = this != null && this != -1L
    fun Float?.isValidExt(): Boolean = this != null && this != -1f && !this.isNaN()
    fun <T> Collection<T>?.isValidExt(): Boolean = !this.isNullOrEmpty()
    fun Any?.isValidExt(): Boolean = when (this) {
        null -> false
        is Boolean -> isValidExt()
        is String -> isValidExt()
        is Int -> isValidExt()
        is Long -> isValidExt()
        is Float -> isValidExt()
        is Collection<*> -> isValidExt()
        else -> true // 非基本类型默认有效
    }
}
