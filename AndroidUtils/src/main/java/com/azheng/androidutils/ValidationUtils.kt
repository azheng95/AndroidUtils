package com.azheng.androidutils

/**
 * 通用数据校验扩展函数工具
 *
 * 功能说明：
 * - 提供统一的数据有效性校验方法
 * - 支持基本类型、字符串、集合等常见数据类型
 * - 采用 Kotlin 扩展函数风格，调用更简洁
 *
 * 设计约定：
 * - null 视为无效
 * - 数值类型中 -1 视为无效（常用于表示未设置/错误状态）
 * - 浮点数的 NaN 视为无效
 * - 空字符串/纯空格字符串视为无效
 * - 空集合/空Map/空数组视为无效
 *
 * @author azheng
 * @since 1.0.0
 */

/**
 * 校验 Boolean 是否有效
 *
 * 有效条件：非 null 且值为 true
 *
 * @receiver 可空的布尔值
 * @return 当值为 true 时返回 true，否则返回 false（包括 null 和 false）
 *
 * @sample
 * ```
 * val flag: Boolean? = true
 * flag.isValid()  // true
 *
 * val nullFlag: Boolean? = null
 * nullFlag.isValid()  // false
 * ```
 */
fun Boolean?.isValid(): Boolean = this == true

/**
 * 校验字符串是否有效
 *
 * 有效条件：非 null、非空字符串、非纯空格字符串
 *
 * @receiver 可空的字符串
 * @return 当字符串包含有效内容时返回 true，否则返回 false
 *
 * @sample
 * ```
 * "hello".isValid()    // true
 * "".isValid()         // false
 * "   ".isValid()      // false（纯空格）
 * null.isValid()       // false
 * ```
 */
fun String?.isValid(): Boolean = !this.isNullOrBlank()

/**
 * 校验 Int 是否有效
 *
 * 有效条件：非 null 且不等于 -1
 *
 * 说明：-1 通常在业务中表示"未设置"或"错误状态"，故视为无效值
 *
 * @receiver 可空的整型
 * @return 当值非 null 且不为 -1 时返回 true
 *
 * @sample
 * ```
 * val id: Int? = 100
 * id.isValid()     // true
 *
 * val errorId: Int? = -1
 * errorId.isValid() // false
 * ```
 */
fun Int?.isValid(): Boolean = this != null && this != -1

/**
 * 校验 Long 是否有效
 *
 * 有效条件：非 null 且不等于 -1L
 *
 * @receiver 可空的长整型
 * @return 当值非 null 且不为 -1L 时返回 true
 *
 * @sample
 * ```
 * val timestamp: Long? = System.currentTimeMillis()
 * timestamp.isValid()  // true
 *
 * val invalid: Long? = -1L
 * invalid.isValid()    // false
 * ```
 */
fun Long?.isValid(): Boolean = this != null && this != -1L

/**
 * 校验 Float 是否有效
 *
 * 有效条件：非 null、不等于 -1f、不是 NaN
 *
 * 说明：
 * - -1f 通常表示"未设置"
 * - NaN (Not a Number) 表示无效的浮点数运算结果
 *
 * @receiver 可空的单精度浮点数
 * @return 当值有效时返回 true
 *
 * @sample
 * ```
 * val price: Float? = 99.9f
 * price.isValid()              // true
 *
 * val nan: Float? = Float.NaN
 * nan.isValid()                // false
 * ```
 */
fun Float?.isValid(): Boolean = this != null && this != -1f && !this.isNaN()

/**
 * 校验 Double 是否有效
 *
 * 有效条件：非 null、不等于 -1.0、不是 NaN
 *
 * @receiver 可空的双精度浮点数
 * @return 当值有效时返回 true
 *
 * @sample
 * ```
 * val latitude: Double? = 39.9042
 * latitude.isValid()   // true
 * ```
 */
fun Double?.isValid(): Boolean = this != null && this != -1.0 && !this.isNaN()

/**
 * 校验集合是否有效
 *
 * 有效条件：非 null 且不为空（至少包含一个元素）
 *
 * @receiver 可空的集合（List、Set 等）
 * @return 当集合非空时返回 true
 *
 * @sample
 * ```
 * val list: List<String>? = listOf("a", "b")
 * list.isValid()       // true
 *
 * val empty: List<String>? = emptyList()
 * empty.isValid()      // false
 * ```
 */
fun <T> Collection<T>?.isValid(): Boolean = !this.isNullOrEmpty()

/**
 * 校验 Map 是否有效
 *
 * 有效条件：非 null 且不为空（至少包含一个键值对）
 *
 * @receiver 可空的 Map
 * @return 当 Map 非空时返回 true
 *
 * @sample
 * ```
 * val map: Map<String, Int>? = mapOf("key" to 1)
 * map.isValid()        // true
 * ```
 */
fun <K, V> Map<K, V>?.isValid(): Boolean = !this.isNullOrEmpty()

/**
 * 校验数组是否有效
 *
 * 有效条件：非 null 且不为空（至少包含一个元素）
 *
 * @receiver 可空的数组
 * @return 当数组非空时返回 true
 *
 * @sample
 * ```
 * val arr: Array<String>? = arrayOf("a", "b")
 * arr.isValid()        // true
 * ```
 */
fun <T> Array<T>?.isValid(): Boolean = !this.isNullOrEmpty()

/**
 * 通用对象校验（运行时类型判断）
 *
 * 根据对象的实际类型，自动选择对应的校验规则。
 * 适用于类型不确定的场景（如处理 Any 类型参数）。
 *
 * 注意：由于需要运行时类型判断，性能略低于具体类型的扩展函数，
 * 建议在类型明确时使用具体的 isValid() 方法。
 *
 * @receiver 可空的任意对象
 * @return 根据对象类型判断有效性，未知类型默认返回 true
 *
 * @sample
 * ```
 * fun processData(data: Any?) {
 *     if (data.isValidAny()) {
 *         // 处理有效数据
 *     }
 * }
 * ```
 */
fun Any?.isValidAny(): Boolean = when (this) {
    null -> false                       // null 始终无效
    is Boolean -> isValid()             // 委托给 Boolean 扩展
    is String -> isValid()              // 委托给 String 扩展
    is Int -> isValid()                 // 委托给 Int 扩展
    is Long -> isValid()                // 委托给 Long 扩展
    is Float -> isValid()               // 委托给 Float 扩展
    is Double -> isValid()              // 委托给 Double 扩展
    is Collection<*> -> isValid()       // 委托给 Collection 扩展
    is Map<*, *> -> isValid()           // 委托给 Map 扩展
    is Array<*> -> isValid()            // 委托给 Array 扩展
    else -> true                        // 其他类型：非 null 即有效
}

/**
 * 静态方法调用入口（兼容 Java 调用及旧代码迁移）
 *
 * 提供传统的静态方法调用方式，内部委托给扩展函数实现。
 * 推荐新代码直接使用扩展函数风格。
 *
 * Java 调用示例：
 * ```java
 * boolean valid = ValidationUtils.INSTANCE.isValid(myString);
 * ```
 *
 * Kotlin 调用示例：
 * ```kotlin
 * ValidationUtils.isValid(myString)
 * ```
 */
object ValidationUtils {

    /** 校验 Boolean 是否有效 */
    fun isValid(value: Boolean?): Boolean = value.isValid()

    /** 校验字符串是否有效（非 null/非空/非纯空格） */
    fun isValid(value: String?): Boolean = value.isValid()

    /** 校验 Int 是否有效（非 null 且不为 -1） */
    fun isValid(value: Int?): Boolean = value.isValid()

    /** 校验 Long 是否有效（非 null 且不为 -1L） */
    fun isValid(value: Long?): Boolean = value.isValid()

    /** 校验 Float 是否有效（非 null/-1f/NaN） */
    fun isValid(value: Float?): Boolean = value.isValid()

    /** 校验 Double 是否有效（非 null/-1.0/NaN） */
    fun isValid(value: Double?): Boolean = value.isValid()

    /** 校验集合是否有效（非 null 且非空） */
    fun <T> isValid(collection: Collection<T>?): Boolean = collection.isValid()

    /** 校验 Map 是否有效（非 null 且非空） */
    fun <K, V> isValid(map: Map<K, V>?): Boolean = map.isValid()

    /** 校验数组是否有效（非 null 且非空） */
    fun <T> isValid(array: Array<T>?): Boolean = array.isValid()

    /** 通用对象校验（运行时类型判断） */
    fun isValid(obj: Any?): Boolean = obj.isValidAny()
}
