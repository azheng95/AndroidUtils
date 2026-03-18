package com.azheng.androidutils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.hjq.gson.factory.GsonFactory
import com.hjq.gson.factory.ParseExceptionCallback
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
 * - 支持注册自定义 TypeAdapterFactory
 * - 支持注册 TypeHierarchyAdapter（处理抽象类/接口）
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

    /**
     * TypeHierarchyAdapter 注册表
     *
     * 用于存储通过 registerTypeHierarchyAdapter 注册的适配器
     * 这些适配器在创建 Gson 实例时会被应用
     *
     * Key: 基类/接口的 Class
     * Value: 对应的 TypeAdapter
     */
    private val typeHierarchyAdapters: MutableMap<Class<*>, TypeAdapter<*>> = ConcurrentHashMap()

    /**
     * TypeAdapter 注册表（精确类型匹配）
     *
     * 用于存储通过 registerTypeAdapter 注册的适配器
     *
     * Key: 精确类型
     * Value: 对应的 TypeAdapter 或 JsonSerializer/JsonDeserializer
     */
    @PublishedApi
    internal val typeAdapters: MutableMap<Type, Any> = ConcurrentHashMap()

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
        // 创建并缓存 Gson 实例
        gsons[KEY_DELEGATE] = createGsonWithHierarchyAdapters()
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

    // ============ TypeHierarchyAdapter 注册（处理抽象类/接口） ============

    /**
     * 注册类型层次适配器
     *
     * 用于处理抽象类、接口及其所有子类的序列化/反序列化
     * 例如：Uri、Parcelable 等抽象类型
     *
     * 注意：注册后会自动刷新 Gson 实例
     *
     * 使用示例：
     * ```
     * GsonUtils.registerTypeHierarchyAdapter(Uri::class.java, UriTypeAdapter())
     * GsonUtils.registerTypeHierarchyAdapter(Drawable::class.java, DrawableTypeAdapter())
     * ```
     *
     * @param baseType 基类或接口的 Class
     * @param typeAdapter 对应的 TypeAdapter
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun <T> registerTypeHierarchyAdapter(
        baseType: Class<T>,
        typeAdapter: TypeAdapter<T>,
        refreshGson: Boolean = true
    ) {
        typeHierarchyAdapters[baseType] = typeAdapter
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 注册类型层次适配器（使用 reified 泛型简化调用）
     *
     * 使用示例：
     * ```
     * GsonUtils.registerTypeHierarchyAdapter<Uri>(UriTypeAdapter())
     * ```
     *
     * @param typeAdapter 对应的 TypeAdapter
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    inline fun <reified T> registerTypeHierarchyAdapter(
        typeAdapter: TypeAdapter<T>,
        refreshGson: Boolean = true
    ) {
        registerTypeHierarchyAdapter(T::class.java, typeAdapter, refreshGson)
    }

    /**
     * 批量注册类型层次适配器
     *
     * @param adapters 类型与适配器的映射
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun registerTypeHierarchyAdapters(
        adapters: Map<Class<*>, TypeAdapter<*>>,
        refreshGson: Boolean = true
    ) {
        typeHierarchyAdapters.putAll(adapters)
        if (refreshGson) {
            refreshGsonInstance()
        }
    }
    /**
     * 注册抽象类/接口的完整支持
     *
     * 同时注册 TypeHierarchyAdapter 和 InstanceCreator，
     * 解决 Kotlin data class 中包含该类型字段时的问题
     *
     * @param baseType 基类或接口的 Class
     * @param typeAdapter 对应的 TypeAdapter
     * @param defaultValue 默认值，用于 data class 字段初始化
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun <T> registerAbstractTypeSupport(
        baseType: Class<T>,
        typeAdapter: TypeAdapter<T>,
        defaultValue: T,
        refreshGson: Boolean = true
    ) {
        typeHierarchyAdapters[baseType] = typeAdapter
        GsonFactory.registerInstanceCreator(baseType) { defaultValue }
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 注册抽象类/接口的完整支持（reified 版本）
     */
    inline fun <reified T> registerAbstractTypeSupport(
        typeAdapter: TypeAdapter<T>,
        defaultValue: T,
        refreshGson: Boolean = true
    ) {
        registerAbstractTypeSupport(T::class.java, typeAdapter, defaultValue, refreshGson)
    }

    /**
     * 移除类型层次适配器
     *
     * @param baseType 基类或接口的 Class
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     * @return 被移除的适配器，不存在返回 null
     */
    fun <T> removeTypeHierarchyAdapter(
        baseType: Class<T>,
        refreshGson: Boolean = true
    ): TypeAdapter<*>? {
        val removed = typeHierarchyAdapters.remove(baseType)
        if (removed != null && refreshGson) {
            refreshGsonInstance()
        }
        return removed
    }

    /**
     * 检查是否已注册指定类型的层次适配器
     *
     * @param baseType 基类或接口的 Class
     * @return 是否已注册
     */
    fun hasTypeHierarchyAdapter(baseType: Class<*>): Boolean {
        return typeHierarchyAdapters.containsKey(baseType)
    }

    /**
     * 获取所有已注册的类型层次适配器的类型
     *
     * @return 不可变的类型集合
     */
    fun getRegisteredHierarchyTypes(): Set<Class<*>> {
        return typeHierarchyAdapters.keys.toSet()
    }

    // ============ TypeAdapter 注册（精确类型匹配） ============

    /**
     * 注册类型适配器（精确类型匹配）
     *
     * 与 registerTypeHierarchyAdapter 不同，这个方法只匹配精确类型
     *
     * @param type 精确类型
     * @param typeAdapter 对应的 TypeAdapter 或 JsonSerializer/JsonDeserializer
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun registerTypeAdapter(
        type: Type,
        typeAdapter: Any,
        refreshGson: Boolean = true
    ) {
        typeAdapters[type] = typeAdapter
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 注册类型适配器（使用 reified 泛型简化调用）
     *
     * 使用示例：
     * ```
     * GsonUtils.registerTypeAdapter<Date>(DateTypeAdapter())
     * ```
     *
     * @param typeAdapter 对应的 TypeAdapter
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    inline fun <reified T> registerTypeAdapter(
        typeAdapter: TypeAdapter<T>,
        refreshGson: Boolean = true
    ) {
        // 直接操作 typeAdapters，避免方法重载歧义
        typeAdapters[T::class.java] = typeAdapter
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 移除类型适配器
     *
     * @param type 要移除的类型
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     * @return 被移除的适配器，不存在返回 null
     */
    fun removeTypeAdapter(
        type: Type,
        refreshGson: Boolean = true
    ): Any? {
        val removed = typeAdapters.remove(type)
        if (removed != null && refreshGson) {
            refreshGsonInstance()
        }
        return removed
    }

    /**
     * 检查是否已注册指定类型的适配器
     *
     * @param type 类型
     * @return 是否已注册
     */
    fun hasTypeAdapter(type: Type): Boolean {
        return typeAdapters.containsKey(type)
    }

    /**
     * 获取所有已注册的类型适配器的类型
     *
     * @return 不可变的类型集合
     */
    fun getRegisteredAdapterTypes(): Set<Type> {
        return typeAdapters.keys.toSet()
    }

    // ============ TypeAdapterFactory 注册 ============

    /**
     * 注册类型解析适配器工厂
     *
     * 注意：注册后会自动刷新 GsonFactory 的单例 Gson，以使新注册的适配器生效
     *
     * @param factory 要注册的 TypeAdapterFactory
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun registerTypeAdapterFactory(factory: TypeAdapterFactory, refreshGson: Boolean = true) {
        GsonFactory.registerTypeAdapterFactory(factory)
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 批量注册类型解析适配器工厂
     *
     * @param factories 要注册的 TypeAdapterFactory 列表
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun registerTypeAdapterFactories(
        vararg factories: TypeAdapterFactory,
        refreshGson: Boolean = true
    ) {
        factories.forEach { factory ->
            GsonFactory.registerTypeAdapterFactory(factory)
        }
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 批量注册类型解析适配器工厂
     *
     * @param factories 要注册的 TypeAdapterFactory 集合
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun registerTypeAdapterFactories(
        factories: Iterable<TypeAdapterFactory>,
        refreshGson: Boolean = true
    ) {
        factories.forEach { factory ->
            GsonFactory.registerTypeAdapterFactory(factory)
        }
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    // ============ InstanceCreator 注册 ============

    /**
     * 注册构造函数创建器
     *
     * 注意：注册后会自动刷新 GsonFactory 的单例 Gson，以使新注册的创建器生效
     *
     * @param type 对象类型
     * @param creator 实例创建器
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun <T> registerInstanceCreator(
        type: Type,
        creator: InstanceCreator<T>,
        refreshGson: Boolean = true
    ) {
        GsonFactory.registerInstanceCreator(type, creator)
        if (refreshGson) {
            refreshGsonInstance()
        }
    }

    /**
     * 注册构造函数创建器（使用 reified 泛型简化调用）
     *
     * 使用示例：
     * ```
     * GsonUtils.registerInstanceCreator<MyClass> { MyClass() }
     * ```
     *
     * @param creator 实例创建器
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    inline fun <reified T> registerInstanceCreator(
        noinline creator: (Type) -> T,
        refreshGson: Boolean = true
    ) {
        registerInstanceCreator(T::class.java, InstanceCreator { type -> creator(type) }, refreshGson)
    }

    // ============ 回调设置 ============

    /**
     * 设置 Json 解析出错回调对象
     *
     * @param callback 解析异常回调，传 null 清除回调
     */
    fun setParseExceptionCallback(callback: ParseExceptionCallback?) {
        GsonFactory.setParseExceptionCallback(callback)
    }

    /**
     * 获取 Json 解析出错回调对象
     *
     * @return 当前设置的解析异常回调，可能为 null
     */
    fun getParseExceptionCallback(): ParseExceptionCallback? {
        return GsonFactory.getParseExceptionCallback()
    }

    // ============ Gson 实例管理 ============

    /**
     * 创建包含所有已注册 TypeHierarchyAdapter 的 Gson 实例
     *
     * @return 配置完成的 Gson 实例
     */
    @Suppress("UNCHECKED_CAST")
    private fun createGsonWithHierarchyAdapters(): Gson {
        val builder = GsonFactory.newGsonBuilder()

        // 注册所有 TypeHierarchyAdapter
        typeHierarchyAdapters.forEach { (type, adapter) ->
            builder.registerTypeHierarchyAdapter(type, adapter as TypeAdapter<Any>)
        }

        // 注册所有精确类型 TypeAdapter
        typeAdapters.forEach { (type, adapter) ->
            builder.registerTypeAdapter(type, adapter)
        }

        return builder.create()
    }

    /**
     * 刷新 GsonFactory 的单例 Gson 实例
     *
     * 在注册新的 TypeAdapterFactory、TypeHierarchyAdapter 或 InstanceCreator 后调用，
     * 以使新注册的适配器/创建器生效
     */
    fun refreshGsonInstance() {
        val newGson = createGsonWithHierarchyAdapters()
        GsonFactory.setSingletonGson(newGson)
        gsons[KEY_DELEGATE] = newGson
    }

    /**
     * 使用 GsonFactory 创建新的 GsonBuilder（已包含 TypeHierarchyAdapter）
     *
     * 可用于自定义配置后创建 Gson 实例
     *
     * @return 配置好的 GsonBuilder
     */
    @Suppress("UNCHECKED_CAST")
    fun newGsonBuilder(): GsonBuilder {
        val builder = GsonFactory.newGsonBuilder()

        // 应用所有已注册的 TypeHierarchyAdapter
        typeHierarchyAdapters.forEach { (type, adapter) ->
            builder.registerTypeHierarchyAdapter(type, adapter as TypeAdapter<Any>)
        }

        // 应用所有精确类型 TypeAdapter
        typeAdapters.forEach { (type, adapter) ->
            builder.registerTypeAdapter(type, adapter)
        }

        return builder
    }

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
        // 重新创建 Gson 实例
        gsons[KEY_DELEGATE] = createGsonWithHierarchyAdapters()
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

    /**
     * 清空所有适配器注册
     *
     * @param refreshGson 是否刷新 Gson 实例，默认为 true
     */
    fun clearAdapters(refreshGson: Boolean = true) {
        typeHierarchyAdapters.clear()
        typeAdapters.clear()
        if (refreshGson) {
            refreshGsonInstance()
        }
    }
}
