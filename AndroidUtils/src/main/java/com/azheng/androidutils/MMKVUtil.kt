package com.azheng.androidutils

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * @author azheng
 * @date 2020/11/17.
 * email：wei.azheng@foxmail.com
 * description：存储数据
 */
class MMKVUtil {

    companion object {
        val mmkv: MMKV by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            MMKV.defaultMMKV()
        }

        fun initialize(context: Context) {
            MMKV.initialize(context)
        }

        /**
         * 提交到数据库
         */
        fun <T : Any> commitData(key: String, value: T?): Boolean {
            if (value == null) return false
            try {
                return when (value) {
                    is String -> mmkv.encode(key, value)
                    is Boolean -> mmkv.encode(key, value)
                    is Float -> mmkv.encode(key, value)
                    is Int -> mmkv.encode(key, value)
                    is Long -> mmkv.encode(key, value)
                    else -> false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

        /**
         * 从数据库获取数据
         */
        fun <T> get(key: String, defValue: T): T? {
            try {
                return when (defValue) {
                    is String -> mmkv.decodeString(key, defValue)
                    is Boolean -> mmkv.decodeBool(key, defValue)
                    is Float -> mmkv.decodeFloat(key, defValue)
                    is Int -> mmkv.decodeInt(key, defValue)
                    is Long -> mmkv.decodeLong(key, defValue)
                    else -> null
                } as T?
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * 设置过期时间 默认不过期
         */
        fun enableAutoKeyExpire(expireDurationInSecond: Int =MMKV.ExpireInYear) {
            // MMKV.ExpireInDay = 24 * 60 * 60
            mmkv.enableAutoKeyExpire(expireDurationInSecond)
        }


        /**
         * 删除
         */
        fun remove(key: String) {
            mmkv.removeValueForKey(key)
        }

        /**
         * 清除全部信息
         */
        fun clearAll() {
            mmkv.clearAll()
        }

    }
}
