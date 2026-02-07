package com.azheng.androidutils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.hjq.gson.factory.GsonFactory
import java.io.Reader
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap
object GsonUtils {

    private const val KEY_DELEGATE = "delegateGson"
    private const val KEY_LOG_UTILS = "logUtilsGson"

    private val GSONS: MutableMap<String, Gson> = ConcurrentHashMap()

    private val defaultGson: Gson by lazy {
        GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create()
    }

    private val logGson: Gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
    }

    init {
        GsonFactory.getSingletonGson()?.let { GSONS[KEY_DELEGATE] = it }
    }

    val gson: Gson
        get() = GSONS[KEY_DELEGATE] ?: defaultGson

    val gson4LogUtils: Gson
        get() = GSONS[KEY_LOG_UTILS] ?: logGson

    fun setGsonDelegate(delegate: Gson?) {
        delegate?.let { GSONS[KEY_DELEGATE] = it }
    }

    fun setGson(key: String, gson: Gson?) {
        if (key.isNotEmpty() && gson != null) {
            GSONS[key] = gson
        }
    }

    fun getGson(key: String): Gson? = GSONS[key]

    // ============ 序列化 ============

    fun toJson(any: Any?): String = gson.toJson(any)

    fun toJson(src: Any?, typeOfSrc: Type): String = gson.toJson(src, typeOfSrc)

    // ============ 反序列化（基础版） ============

    fun <T> fromJson(json: String?, type: Class<T>): T = gson.fromJson(json, type)

    fun <T> fromJson(json: String?, type: Type): T = gson.fromJson(json, type)

    fun <T> fromJson(reader: Reader, type: Class<T>): T = gson.fromJson(reader, type)

    fun <T> fromJson(reader: Reader, type: Type): T = gson.fromJson(reader, type)

    // ============ 安全解析辅助方法 ============

    /**
     * 通用的安全解析方法
     * @PublishedApi 允许 public inline 函数访问此 internal 函数
     */
    @PublishedApi
    internal inline fun <T> safeFromJson(
        json: String?,
        defaultValue: T,
        crossinline parse: () -> T
    ): T {
        if (json.isNullOrEmpty()) return defaultValue
        return runCatching { parse() }
            .onFailure { it.printStackTrace() }
            .getOrDefault(defaultValue)
    }

    // ============ 反序列化（安全版） ============

    inline fun <reified T> fromJsonToObject(
        json: String?,
        defaultValue: T? = null
    ): T? = safeFromJson(json, defaultValue) {
        fromJson(json, T::class.java)
    }

    inline fun <reified T> fromJsonToList(
        json: String?,
        defaultValue: List<T> = emptyList()
    ): List<T> = safeFromJson(json, defaultValue) {
        fromJson<List<T>>(json, getListType(T::class.java))
    }

    inline fun <reified T> fromJsonToMutableList(
        json: String?,
        defaultValue: MutableList<T> = mutableListOf()
    ): MutableList<T> = safeFromJson(json, defaultValue) {
        fromJson<MutableList<T>>(json, getListType(T::class.java))
    }

    inline fun <reified T> fromJsonToSet(
        json: String?,
        defaultValue: Set<T> = emptySet()
    ): Set<T> = safeFromJson(json, defaultValue) {
        fromJson<Set<T>>(json, getSetType(T::class.java))
    }

    inline fun <reified T> fromJsonToMutableSet(
        json: String?,
        defaultValue: MutableSet<T> = mutableSetOf()
    ): MutableSet<T> = safeFromJson(json, defaultValue) {
        fromJson<MutableSet<T>>(json, getSetType(T::class.java))
    }

    inline fun <reified K, reified V> fromJsonToMap(
        json: String?,
        defaultValue: Map<K, V> = emptyMap()
    ): Map<K, V> = safeFromJson(json, defaultValue) {
        fromJson<Map<K, V>>(json, getMapType(K::class.java, V::class.java))
    }

    inline fun <reified K, reified V> fromJsonToMutableMap(
        json: String?,
        defaultValue: MutableMap<K, V> = mutableMapOf()
    ): MutableMap<K, V> = safeFromJson(json, defaultValue) {
        fromJson<MutableMap<K, V>>(json, getMapType(K::class.java, V::class.java))
    }

    // ============ Type 工具 ============

    fun getListType(type: Type): Type =
        TypeToken.getParameterized(List::class.java, type).type

    fun getSetType(type: Type): Type =
        TypeToken.getParameterized(Set::class.java, type).type

    fun getMapType(keyType: Type, valueType: Type): Type =
        TypeToken.getParameterized(Map::class.java, keyType, valueType).type

    fun getArrayType(type: Type): Type =
        TypeToken.getArray(type).type

    fun getType(rawType: Type, vararg typeArguments: Type): Type =
        TypeToken.getParameterized(rawType, *typeArguments).type
}
