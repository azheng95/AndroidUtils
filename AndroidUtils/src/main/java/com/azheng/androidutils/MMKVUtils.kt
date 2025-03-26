package com.azheng.androidutils

import com.tencent.mmkv.MMKV

/**
 * @author azheng
 * @date 2020/11/17.
 * email：wei.azheng@foxmail.com
 * description：存储数据
 */
object MMKVUtils {

    val mmkv: MMKV by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MMKV.defaultMMKV(MMKV.SINGLE_PROCESS_MODE, "utils_mmkv")
    }

    init {
        MMKV.initialize(Utils.getApplication())
    }

    /**
     * 存储数据（支持基础数据类型）
     * @param key 键名（建议使用命名空间规范如 "config:user_name"）
     * @param value 支持类型：String/Boolean/Float/Int/Long/Double
     * @return 存储是否成功（当类型不支持时返回 false）
     *
     * - 空值（传入 null 会直接返回 false）
     */
    fun put(
        key: String,
        value: Any?,
    ): Boolean {
        try {
            if (value == null) {
                return false
            }
            return when (value) {
                is String -> mmkv.encode(key, value)
                is Boolean -> mmkv.encode(key, value)
                is Float -> mmkv.encode(key, value)
                is Int -> mmkv.encode(key, value)
                is Long -> mmkv.encode(key, value)
                is Double -> mmkv.encode(key, value)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * 读取数据（类型安全模式）
     * @param key 键名
     * @param defaultValue 默认值（必须与存储时的数据类型一致）
     * @return 读取结果（当类型不匹配时返回 null）
     *
     * 注意：默认值类型必须与实际存储类型严格匹配，否则会导致：
     * - 返回 null
     */
    fun get(
        key: String,
        defaultValue: Any,
    ): Any? {
        try {
            return when (defaultValue) {
                is String -> mmkv.decodeString(key, defaultValue)
                is Boolean -> mmkv.decodeBool(key, defaultValue)
                is Float -> mmkv.decodeFloat(key, defaultValue)
                is Int -> mmkv.decodeInt(key, defaultValue)
                is Long -> mmkv.decodeLong(key, defaultValue)
                is Double -> mmkv.decodeDouble(key, defaultValue)
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    /**
     * 检查 MMKV 中是否包含指定键
     * @param key 需要检查的键名
     * @return Boolean 存在返回 true，否则 false
     *
     * */
    fun containsKey(key: String) = mmkv.containsKey(key)


    /**
     * 获取当前 MMKV 实例存储的所有键集合
     * @return Array<String>? 可能为空的键数组（按插入顺序无序排列）
     * 数据量超过 10,000 条时建议在子线程调用
     * */
    fun getAllKeys() = mmkv.allKeys()

    /**
     * 删除指定键值对
     * @param key 要删除的键名
     */
    fun remove(key: String) {
        mmkv.removeValueForKey(key)
    }

    /**
     * 清空所有数据（危险操作！）
     * 注意：会清除当前 MMKV 实例的所有数据（包括其他模块存储的数据）
     */
    fun clearAll() {
        mmkv.clearAll()
    }

}
