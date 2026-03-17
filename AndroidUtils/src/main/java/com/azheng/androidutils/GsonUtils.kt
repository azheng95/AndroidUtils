package com.azheng.androidutils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hjq.gson.factory.GsonFactory
import java.io.Reader
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

/**
 * Gson 工具类
 *
 * 对 [GsonFactory] 的封装，提供便捷的 JSON 序列化/反序列化方法
 * - 支持多个 Gson 实例管理
 * - 提供安全的反序列化方法（自动处理异常）
 * - 提供常用 Type 构建工具
 *
 * @author azheng
 */
object GsonUtils {

    private const val KEY_DELEGATE = "delegateGson"
    private const val KEY_LOG_UTILS = "logUtilsGson"

    /** Gson 实例缓存，使用 ConcurrentHashMap 保证线程安全 */
    private val gsons: MutableMap<String, Gson> = ConcurrentHashMap()

    /** Type 缓存，避免重复创建相同的 Type 对象 */
    private val typeCache: MutableMap<String, Type> = ConcurrentHashMap()

    /** 解析异常回调 */
    @Volatile
    var parseExceptionHandler: ((Throwable, String?) -> Unit)? = null

    /** 默认 Gson 实例（序列化 null 值，禁用 HTML 转义） */
    private val defaultGson: Gson by lazy {
        GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create()
    }

    /** 日志输出专用 Gson 实例（格式化输出） */
    private val logGson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    init {
        // GsonFactory.getSingletonGson() 内部有双重检查锁，不会返回 null
        gsons[KEY_DELEGATE] = GsonFactory.getSingletonGson()
    }

    /**
     * 获取主要使用的 Gson 实例
     *
     * 优先返回代理的 Gson，如果未设置则返回默认 Gson
     */
    val gson: Gson
        get() = gsons[KEY_DELEGATE] ?: defaultGson

    /**
     * 获取日志工具专用的 Gson 实例（格式化输出）
     */
    val gson4LogUtils: Gson
        get() = gsons[KEY_LOG_UTILS] ?: logGson

    // ============ Gson 实例管理 ============

    /**
     * 设置代理 Gson 实例
     *
     * @param delegate 要设置的 Gson 实例，为 null 时忽略
     */
    fun setGsonDelegate(delegate: Gson?) {
        delegate?.let { gsons[KEY_DELEGATE] = it }
    }

    /**
     * 设置指定 key 的 Gson 实例
     *
     * @param key 缓存键（不能为空）
     * @param gson Gson 实例（不能为 null）
     */
    fun setGson(key: String, gson: Gson?) {
        if (key.isNotEmpty() && gson != null) {
            gsons[key] = gson
        }
    }

    /**
     * 获取指定 key 的 Gson 实例
     *
     * @param key 缓存键
     * @return 对应的 Gson 实例，不存在返回 null
     */
    fun getGson(key: String): Gson? = gsons[key]

    /**
     * 移除指定 key 的 Gson 实例
     *
     * @param key 缓存键
     * @return 被移除的 Gson 实例，不存在返回 null
     */
    fun removeGson(key: String): Gson? = gsons.remove(key)

    /**
     * 检查是否存在指定 key 的 Gson 实例
     *
     * @param key 缓存键
     * @return 是否存在
     */
    fun containsGson(key: String): Boolean = gsons.containsKey(key)

    /**
     * 清空所有缓存的 Gson 实例（不影响默认和日志 Gson）
     */
    fun clearGsons() {
        gsons.clear()
        // 重新注册 GsonFactory 的单例
        gsons[KEY_DELEGATE] = GsonFactory.getSingletonGson()
    }

    /**
     * 获取所有已注册的 Gson 实例 key
     *
     * @return 不可变的 key 集合
     */
    fun getRegisteredKeys(): Set<String> = gsons.keys.toSet()

    // ============ 序列化 ============

    /**
     * 将对象序列化为 JSON 字符串
     *
     * @param any 要序列化的对象
     * @return JSON 字符串
     */
    fun toJson(any: Any?): String = gson.toJson(any)

    /**
     * 将对象序列化为 JSON 字符串（指定类型）
     *
     * @param src 要序列化的对象
     * @param typeOfSrc 对象的类型
     * @return JSON 字符串
     */
    fun toJson(src: Any?, typeOfSrc: Type): String = gson.toJson(src, typeOfSrc)

    /**
     * 将对象序列化为格式化的 JSON 字符串（用于日志输出）
     *
     * @param any 要序列化的对象
     * @return 格式化的 JSON 字符串
     */
    fun toPrettyJson(any: Any?): String = gson4LogUtils.toJson(any)

    // ============ 反序列化（基础版） ============

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param type 目标类型的 Class
     * @return 反序列化后的对象
     * @throws com.google.gson.JsonSyntaxException JSON 格式错误时抛出
     */
    fun <T> fromJson(json: String?, type: Class<T>): T = gson.fromJson(json, type)

    /**
     * 将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param type 目标类型
     * @return 反序列化后的对象
     * @throws com.google.gson.JsonSyntaxException JSON 格式错误时抛出
     */
    fun <T> fromJson(json: String?, type: Type): T = gson.fromJson(json, type)

    /**
     * 从 Reader 反序列化为对象
     *
     * @param reader 输入流
     * @param type 目标类型的 Class
     * @return 反序列化后的对象
     */
    fun <T> fromJson(reader: Reader, type: Class<T>): T = gson.fromJson(reader, type)

    /**
     * 从 Reader 反序列化为对象
     *
     * @param reader 输入流
     * @param type 目标类型
     * @return 反序列化后的对象
     */
    fun <T> fromJson(reader: Reader, type: Type): T = gson.fromJson(reader, type)

    // ============ 安全解析辅助方法 ============

    /**
     * 通用的安全解析方法（内部使用）
     *
     * 解析失败时返回默认值，并通过 [parseExceptionHandler] 回调异常
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值
     * @param parse 实际的解析逻辑
     * @return 解析结果或默认值
     */
    @PublishedApi
    internal inline fun <T> safeFromJson(
        json: String?,
        defaultValue: T,
        crossinline parse: () -> T
    ): T {
        if (json.isNullOrEmpty()) return defaultValue
        return runCatching { parse() }
            .onFailure { e ->
                parseExceptionHandler?.invoke(e, json) ?: e.printStackTrace()
            }
            .getOrDefault(defaultValue)
    }

    // ============ 反序列化（安全版 - 对象） ============

    /**
     * 安全地将 JSON 字符串反序列化为对象
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为 null
     * @return 解析结果或默认值
     */
    inline fun <reified T> fromJsonToObject(
        json: String?,
        defaultValue: T? = null
    ): T? = safeFromJson(json, defaultValue) {
        fromJson(json, T::class.java)
    }

    /**
     * 安全地将 JSON 字符串反序列化为非空对象
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值
     * @return 解析结果或默认值
     */
    inline fun <reified T : Any> fromJsonToObjectOrDefault(
        json: String?,
        defaultValue: T
    ): T = safeFromJson(json, defaultValue) {
        fromJson(json, T::class.java)
    }

    // ============ 反序列化（安全版 - List） ============

    /**
     * 安全地将 JSON 字符串反序列化为 List
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空列表
     * @return 解析结果或默认值
     */
    inline fun <reified T> fromJsonToList(
        json: String?,
        defaultValue: List<T> = emptyList()
    ): List<T> = safeFromJson(json, defaultValue) {
        fromJson(json, getListType(T::class.java))
    }

    /**
     * 安全地将 JSON 字符串反序列化为 MutableList
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空可变列表
     * @return 解析结果或默认值
     */
    inline fun <reified T> fromJsonToMutableList(
        json: String?,
        defaultValue: MutableList<T> = mutableListOf()
    ): MutableList<T> = safeFromJson(json, defaultValue) {
        fromJson<MutableList<T>>(json, getListType(T::class.java))
    }

    // ============ 反序列化（安全版 - Set） ============

    /**
     * 安全地将 JSON 字符串反序列化为 Set
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空集合
     * @return 解析结果或默认值
     */
    inline fun <reified T> fromJsonToSet(
        json: String?,
        defaultValue: Set<T> = emptySet()
    ): Set<T> = safeFromJson(json, defaultValue) {
        fromJson(json, getSetType(T::class.java))
    }

    /**
     * 安全地将 JSON 字符串反序列化为 MutableSet
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空可变集合
     * @return 解析结果或默认值
     */
    inline fun <reified T> fromJsonToMutableSet(
        json: String?,
        defaultValue: MutableSet<T> = mutableSetOf()
    ): MutableSet<T> = safeFromJson(json, defaultValue) {
        fromJson(json, getSetType(T::class.java))
    }

    // ============ 反序列化（安全版 - Map） ============

    /**
     * 安全地将 JSON 字符串反序列化为 Map
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空映射
     * @return 解析结果或默认值
     */
    inline fun <reified K, reified V> fromJsonToMap(
        json: String?,
        defaultValue: Map<K, V> = emptyMap()
    ): Map<K, V> = safeFromJson(json, defaultValue) {
        fromJson(json, getMapType(K::class.java, V::class.java))
    }

    /**
     * 安全地将 JSON 字符串反序列化为 MutableMap
     *
     * @param json JSON 字符串
     * @param defaultValue 解析失败时返回的默认值，默认为空可变映射
     * @return 解析结果或默认值
     */
    inline fun <reified K, reified V> fromJsonToMutableMap(
        json: String?,
        defaultValue: MutableMap<K, V> = mutableMapOf()
    ): MutableMap<K, V> = safeFromJson(json, defaultValue) {
        fromJson(json, getMapType(K::class.java, V::class.java))
    }

    // ============ 反序列化（安全版 - TypeToken） ============

    /**
     * 使用 TypeToken 安全地反序列化 JSON
     *
     * 适用于复杂的泛型类型，例如：
     * ```
     * val type = object : TypeToken<List<Map<String, User>>>() {}
     * val result = GsonUtils.fromJsonSafe(json, type)
     * ```
     *
     * @param json JSON 字符串
     * @param typeToken 类型标记
     * @param defaultValue 解析失败时返回的默认值
     * @return 解析结果或默认值
     */
    fun <T> fromJsonSafe(
        json: String?,
        typeToken: TypeToken<T>,
        defaultValue: T? = null
    ): T? = safeFromJson(json, defaultValue) {
        fromJson(json, typeToken.type)
    }

    // ============ Type 工具（带缓存） ============

    /**
     * 获取 List<T> 的 Type（带缓存）
     *
     * @param type 元素类型
     * @return 参数化的 List Type
     */
    fun getListType(type: Type): Type =
        getCachedType("List<$type>") {
            TypeToken.getParameterized(List::class.java, type).type
        }

    /**
     * 获取 Set<T> 的 Type（带缓存）
     *
     * @param type 元素类型
     * @return 参数化的 Set Type
     */
    fun getSetType(type: Type): Type =
        getCachedType("Set<$type>") {
            TypeToken.getParameterized(Set::class.java, type).type
        }

    /**
     * 获取 Map<K, V> 的 Type（带缓存）
     *
     * @param keyType 键类型
     * @param valueType 值类型
     * @return 参数化的 Map Type
     */
    fun getMapType(keyType: Type, valueType: Type): Type =
        getCachedType("Map<$keyType,$valueType>") {
            TypeToken.getParameterized(Map::class.java, keyType, valueType).type
        }

    /**
     * 获取数组类型的 Type
     *
     * @param type 数组元素类型
     * @return 数组 Type
     */
    fun getArrayType(type: Type): Type =
        getCachedType("Array<$type>") {
            TypeToken.getArray(type).type
        }

    /**
     * 获取参数化类型的 Type
     *
     * @param rawType 原始类型
     * @param typeArguments 类型参数
     * @return 参数化 Type
     */
    fun getType(rawType: Type, vararg typeArguments: Type): Type {
        val key = "$rawType<${typeArguments.joinToString(",")}>"
        return getCachedType(key) {
            TypeToken.getParameterized(rawType, *typeArguments).type
        }
    }

    /**
     * 获取缓存的 Type，如果不存在则创建并缓存
     *
     * @param key 缓存键
     * @param creator Type 创建函数
     * @return Type 对象
     */
    private inline fun getCachedType(key: String, creator: () -> Type): Type =
        typeCache.getOrPut(key, creator)

    /**
     * 清空 Type 缓存
     */
    fun clearTypeCache() {
        typeCache.clear()
    }
}
