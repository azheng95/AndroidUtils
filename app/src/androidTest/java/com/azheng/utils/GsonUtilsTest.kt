package com.azheng.utils

import com.azheng.androidutils.GsonUtils

/**
 * @date 2025/3/26.
 * description：
 */
class GsonUtilsTest {
    fun test() {
        val json =
            "{\"name\":\"name\",\"age\":18}"
        val userBean = GsonUtils.fromJsonToObject<UserBean>(json)
    }
}