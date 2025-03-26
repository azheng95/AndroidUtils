package com.azheng.androidutils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hjq.gson.factory.GsonFactory
import java.io.Reader
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * @date 2025/3/25.
 * description：
 */
/**
 * Gson 工具类，提供 JSON 序列化和反序列化功能
 * 基于 Gson 库 2.12.1 版本
 */
object GsonUtils {
    init {
        //默认使用Gson 解析容错适配器
        setGsonDelegate()
    }

    // 常量键值定义，用于在 GSONS Map 中存储不同用途的 Gson 实例
    private const val KEY_DEFAULT = "defaultGson"      // 默认 Gson 实例的键
    private const val KEY_DELEGATE = "delegateGson"    // 委托 Gson 实例的键
    private const val KEY_LOG_UTILS = "logUtilsGson"   // 日志专用 Gson 实例的键

    // 线程安全的 Map，用于存储不同配置的 Gson 实例
    private val GSONS: MutableMap<String, Gson?> = ConcurrentHashMap()

    /**
     * 设置 Gson 的委托实例，优先使用此实例进行序列化/反序列化
     *
     * @param delegate Gson 委托实例
     * GsonFactory.getSingletonGson() 容错框架
     */
    fun setGsonDelegate(delegate: Gson? = GsonFactory.getSingletonGson()) {
        if (delegate == null) return
        GSONS[KEY_DELEGATE] = delegate
    }

    /**
     * 使用自定义键设置 Gson 实例
     *
     * @param key  自定义键名
     * @param gson Gson 实例
     */
    fun setGson(key: String, gson: Gson?) {
        if (key.isEmpty() || gson == null) return
        GSONS[key] = gson
    }

    /**
     * 获取指定键名的 Gson 实例
     *
     * @param key 键名
     * @return 对应的 Gson 实例，如果不存在则返回 null
     */
    fun getGson(key: String): Gson? = GSONS[key]

    /**
     * 获取默认 Gson 实例
     * 优先返回委托实例，如果不存在则返回或创建默认实例
     *
     * @return Gson 实例
     */
    val gson: Gson
        get() {
            // 优先使用委托实例
            GSONS[KEY_DELEGATE]?.let { return it }

            // 获取或创建默认实例
            var gsonDefault = GSONS[KEY_DEFAULT]
            if (gsonDefault == null) {
                gsonDefault = createGson()
                GSONS[KEY_DEFAULT] = gsonDefault
            }
            return gsonDefault
        }

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param object 要序列化的对象
     * @return 序列化后的 JSON 字符串
     */
    fun toJson(any: Any?): String = gson.toJson(any)

    /**
     * 将对象按指定类型序列化为 JSON 字符串
     *
     * @param src       要序列化的对象
     * @param typeOfSrc 对象的泛型类型
     * @return 序列化后的 JSON 字符串
     */
    fun toJson(src: Any?, typeOfSrc: Type): String = gson.toJson(src, typeOfSrc)

    /**
     * 使用指定的 Gson 实例将对象序列化为 JSON 字符串
     *
     * @param gson   指定的 Gson 实例
     * @param object 要序列化的对象
     * @return 序列化后的 JSON 字符串
     */
    fun toJson(gson: Gson, any: Any?): String = gson.toJson(any)

    /**
     * 使用指定的 Gson 实例将对象按指定类型序列化为 JSON 字符串
     *
     * @param gson      指定的 Gson 实例
     * @param src       要序列化的对象
     * @param typeOfSrc 对象的泛型类型
     * @return 序列化后的 JSON 字符串
     */
    fun toJson(gson: Gson, src: Any?, typeOfSrc: Type): String = gson.toJson(src, typeOfSrc)

    /**
     * 将 JSON 字符串反序列化为指定类型的对象
     *
     * @param json JSON 字符串
     * @param type 目标类型的 Class
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(json: String?, type: Class<T>): T = gson.fromJson(json, type)

    /**
     * 将 JSON 字符串反序列化为指定泛型类型的对象
     *
     * @param json JSON 字符串
     * @param type 目标的泛型类型
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(json: String?, type: Type): T = gson.fromJson(json, type)

    /**
     * 将 Reader 中的 JSON 内容反序列化为指定类型的对象
     *
     * @param reader Reader 对象
     * @param type   目标类型的 Class
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(reader: Reader, type: Class<T>): T = gson.fromJson(reader, type)

    /**
     * 将 Reader 中的 JSON 内容反序列化为指定泛型类型的对象
     *
     * @param reader Reader 对象
     * @param type   目标的泛型类型
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(reader: Reader, type: Type): T = gson.fromJson(reader, type)

    /**
     * 使用指定的 Gson 实例将 JSON 字符串反序列化为指定类型的对象
     *
     * @param gson 指定的 Gson 实例
     * @param json JSON 字符串
     * @param type 目标类型的 Class
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(gson: Gson, json: String?, type: Class<T>): T = gson.fromJson(json, type)

    /**
     * 使用指定的 Gson 实例将 JSON 字符串反序列化为指定泛型类型的对象
     *
     * @param gson 指定的 Gson 实例
     * @param json JSON 字符串
     * @param type 目标的泛型类型
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(gson: Gson, json: String?, type: Type): T = gson.fromJson(json, type)

    /**
     * 使用指定的 Gson 实例将 Reader 中的 JSON 内容反序列化为指定类型的对象
     *
     * @param gson   指定的 Gson 实例
     * @param reader Reader 对象
     * @param type   目标类型的 Class
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(gson: Gson, reader: Reader, type: Class<T>): T = gson.fromJson(reader, type)

    /**
     * 使用指定的 Gson 实例将 Reader 中的 JSON 内容反序列化为指定泛型类型的对象
     *
     * @param gson   指定的 Gson 实例
     * @param reader Reader 对象
     * @param type   目标的泛型类型
     * @return 反序列化后的对象实例
     */
    fun <T> fromJson(gson: Gson, reader: Reader, type: Type): T = gson.fromJson(reader, type)

    /**
     * 获取包含指定元素类型的 List 的泛型类型
     *
     * @param type 元素类型
     * @return List<type> 的泛型类型
     */
    fun getListType(type: Type): Type =
        TypeToken.getParameterized(MutableList::class.java, type).type

    /**
     * 获取包含指定元素类型的 Set 的泛型类型
     *
     * @param type 元素类型
     * @return Set<type> 的泛型类型
     */
    fun getSetType(type: Type): Type = TypeToken.getParameterized(MutableSet::class.java, type).type

    /**
     * 获取包含指定键值类型的 Map 的泛型类型
     *
     * @param keyType   键的类型
     * @param valueType 值的类型
     * @return Map<keyType, valueType> 的泛型类型
     */
    fun getMapType(keyType: Type, valueType: Type): Type =
        TypeToken.getParameterized(MutableMap::class.java, keyType, valueType).type

    /**
     * 获取指定元素类型的数组类型
     *
     * @param type 元素类型
     * @return type[] 的数组类型
     */
    fun getArrayType(type: Type): Type = TypeToken.getArray(type).type

    /**
     * 获取指定原始类型和类型参数的泛型类型
     *
     * @param rawType       原始类型
     * @param typeArguments 类型参数
     * @return 泛型类型
     */
    fun getType(rawType: Type, vararg typeArguments: Type): Type =
        TypeToken.getParameterized(rawType, *typeArguments).type

    /**
     * 获取用于日志输出的 Gson 实例
     * 该实例配置了美化输出和序列化 null 值
     *
     * @return 用于日志的 Gson 实例
     */
    val gson4LogUtils: Gson
        get() {
            var gson4LogUtils = GSONS[KEY_LOG_UTILS]
            if (gson4LogUtils == null) {
                gson4LogUtils = GsonBuilder()
                    .setPrettyPrinting()  // 美化输出
                    .serializeNulls()     // 序列化 null 值
                    .create()
                GSONS[KEY_LOG_UTILS] = gson4LogUtils
            }
            return gson4LogUtils!!
        }

    /**
     * 创建默认的 Gson 实例
     * 配置了序列化 null 值和禁用 HTML 转义
     *
     * @return 默认配置的 Gson 实例
     */
    private fun createGson(): Gson = GsonBuilder()
        .serializeNulls()        // 序列化 null 值
        .disableHtmlEscaping()   // 禁用 HTML 转义
        .create()

    /**
     * 将 JSON 字符串反序列化为指定类型的可变列表，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空列表
     * @return 反序列化后的可变列表，解析失败时返回默认值
     */
    inline fun <reified T> fromJsonToMutableList(
        json: String?,
        defaultValue: MutableList<T> = mutableListOf()
    ): MutableList<T> {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJson(json, getListType(T::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            // 可以选择记录异常信息
            // Log.e("GsonUtils", "解析JSON失败: ${e.message}")
            defaultValue
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的不可变列表，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空列表
     * @return 反序列化后的不可变列表，解析失败时返回默认值
     */
    inline fun <reified T> fromJsonToList(
        json: String?,
        defaultValue: List<T> = emptyList()
    ): List<T> {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJsonToMutableList<T>(json, defaultValue.toMutableList()).toList()
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的可变集合，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空集合
     * @return 反序列化后的可变集合，解析失败时返回默认值
     */
    inline fun <reified T> fromJsonToMutableSet(
        json: String?,
        defaultValue: MutableSet<T> = mutableSetOf()
    ): MutableSet<T> {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJson(json, getSetType(T::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的不可变集合，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空集合
     * @return 反序列化后的不可变集合，解析失败时返回默认值
     */
    inline fun <reified T> fromJsonToSet(
        json: String?,
        defaultValue: Set<T> = emptySet()
    ): Set<T> {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJsonToMutableSet<T>(json, defaultValue.toMutableSet()).toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定键值类型的可变映射，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空映射
     * @return 反序列化后的可变映射，解析失败时返回默认值
     */
    inline fun <reified K, reified V> fromJsonToMutableMap(
        json: String?,
        defaultValue: MutableMap<K, V> = mutableMapOf()
    ): MutableMap<K, V> {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJson(json, getMapType(K::class.java, V::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定键值类型的不可变映射，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空映射
     * @return 反序列化后的不可变映射，解析失败时返回默认值
     */
    inline fun <reified K, reified V> fromJsonToMap(
        json: String?,
        defaultValue: Map<K, V> = emptyMap()
    ): Map<K, V> {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJsonToMutableMap<K, V>(json, defaultValue.toMutableMap()).toMap()
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    /**
     * 将 JSON 字符串反序列化为指定类型的对象，具有容错处理
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为 null
     * @return 反序列化后的对象，解析失败时返回默认值
     */
    inline fun <reified T> fromJsonToObject(
        json: String?,
        defaultValue: T? = null
    ): T? {
        if (json.isNullOrEmpty()) return defaultValue

        return try {
            fromJson<T>(json, T::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

}