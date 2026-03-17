package com.azheng.utils.demo.bean

/**
 * 用户实体类（用于 GsonUtils 示例）
 */
data class User(
    val id: Int = 0,
    val name: String = "",
    val email: String? = null,
    val age: Int = 0
)

/**
 * 嵌套对象示例
 */
data class Company(
    val name: String = "",
    val employees: List<User> = emptyList(),
    val departments: Map<String, List<User>> = emptyMap()
)
