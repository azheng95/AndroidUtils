package com.azheng.utils.demo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.azheng.androidutils.GsonUtils
import com.azheng.utils.demo.bean.Company
import com.azheng.utils.demo.bean.User
import com.azheng.utils.demo.databinding.ActivityGsonBinding
import com.google.gson.reflect.TypeToken

/**
 * GsonUtils 使用示例
 *
 * 展示 GsonUtils 的各种序列化/反序列化功能
 */
class GsonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGsonBinding
    private val TAG = "GsonActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGsonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        setupExceptionHandler()
    }

    private fun setupButtons() {
        // 对象序列化示例
        binding.btnSerialize.setOnClickListener {
            demonstrateSerialize()
        }

        // 对象反序列化示例
        binding.btnDeserializeObject.setOnClickListener {
            demonstrateDeserializeObject()
        }

        // List 反序列化示例
        binding.btnDeserializeList.setOnClickListener {
            demonstrateDeserializeList()
        }

        // Map 反序列化示例
        binding.btnDeserializeMap.setOnClickListener {
            demonstrateDeserializeMap()
        }

        // 安全解析示例（处理异常）
        binding.btnSafeParse.setOnClickListener {
            demonstrateSafeParse()
        }

        // 复杂嵌套对象示例
        binding.btnNestedObject.setOnClickListener {
            demonstrateNestedObject()
        }

        // TypeToken 使用示例
        binding.btnTypeToken.setOnClickListener {
            demonstrateTypeToken()
        }

        // 清空日志
        binding.btnClear.setOnClickListener {
            binding.tvResult.text = ""
        }
    }

    /**
     * 设置异常处理回调
     */
    private fun setupExceptionHandler() {
        GsonUtils.parseExceptionHandler = { throwable, json ->
            Log.e(TAG, "JSON 解析失败: $json", throwable)
            appendResult("❌ 解析异常: ${throwable.message?.take(80)}...")
        }
    }

    /**
     * 示例1: 对象序列化
     */
    private fun demonstrateSerialize() {
        appendResult("===== 对象序列化示例 =====")

        // 单个对象
        val user = User(1, "张三", "zhangsan@example.com", 25)
        val userJson = GsonUtils.toJson(user)
        appendResult("User 对象 -> JSON:")
        appendResult(userJson)

        // 格式化输出（用于日志）
        val prettyJson = GsonUtils.toPrettyJson(user)
        appendResult("\n格式化 JSON:")
        appendResult(prettyJson)

        // List 序列化
        val userList = listOf(
            User(1, "张三", "zhangsan@example.com", 25),
            User(2, "李四", "lisi@example.com", 30)
        )
        val listJson = GsonUtils.toJson(userList)
        appendResult("\nList<User> -> JSON:")
        appendResult(listJson)

        // Map 序列化
        val userMap = mapOf(
            "admin" to User(1, "管理员", null, 35),
            "guest" to User(2, "访客", null, 20)
        )
        val mapJson = GsonUtils.toJson(userMap)
        appendResult("\nMap<String, User> -> JSON:")
        appendResult(mapJson)
    }

    /**
     * 示例2: 对象反序列化
     */
    private fun demonstrateDeserializeObject() {
        appendResult("===== 对象反序列化示例 =====")

        val json = """{"id":1,"name":"张三","email":"zhangsan@example.com","age":25}"""

        // 方式1: 安全解析（推荐）
        val user1 = GsonUtils.fromJsonToObject<User>(json)
        appendResult("安全解析结果: $user1")

        // 方式2: 带默认值的安全解析
        val user2 = GsonUtils.fromJsonToObjectOrDefault(json, User())
        appendResult("带默认值解析: $user2")

        // 方式3: 基础解析（可能抛异常）
        val user3 = GsonUtils.fromJson(json, User::class.java)
        appendResult("基础解析结果: $user3")

        // null 输入测试
        val nullResult = GsonUtils.fromJsonToObject<User>(null)
        appendResult("null 输入结果: $nullResult")

        // 空字符串测试
        val emptyResult = GsonUtils.fromJsonToObject<User>("")
        appendResult("空字符串结果: $emptyResult")
    }

    /**
     * 示例3: List 反序列化
     */
    private fun demonstrateDeserializeList() {
        appendResult("===== List 反序列化示例 =====")

        val json = """
            [
                {"id":1,"name":"张三","email":"zhangsan@example.com","age":25},
                {"id":2,"name":"李四","email":"lisi@example.com","age":30},
                {"id":3,"name":"王五","email":null,"age":28}
            ]
        """.trimIndent()

        // 解析为不可变 List
        val userList = GsonUtils.fromJsonToList<User>(json)
        appendResult("List<User> 大小: ${userList.size}")
        userList.forEach { appendResult("  - $it") }

        // 解析为可变 List
        val mutableList = GsonUtils.fromJsonToMutableList<User>(json)
        mutableList.add(User(4, "新用户", null, 22))
        appendResult("\nMutableList 添加元素后大小: ${mutableList.size}")

        // 解析失败返回默认值（使用完全无效的 JSON）
        val defaultList = GsonUtils.fromJsonToList<User>("这不是JSON")
        appendResult("解析失败返回默认空列表: $defaultList")
    }

    /**
     * 示例4: Map 反序列化
     */
    private fun demonstrateDeserializeMap() {
        appendResult("===== Map 反序列化示例 =====")

        // String -> User 的 Map
        val userMapJson = """
            {
                "admin": {"id":1,"name":"管理员","age":35},
                "editor": {"id":2,"name":"编辑","age":28},
                "guest": {"id":3,"name":"访客","age":20}
            }
        """.trimIndent()

        val userMap = GsonUtils.fromJsonToMap<String, User>(userMapJson)
        appendResult("Map<String, User> 大小: ${userMap.size}")
        userMap.forEach { (key, value) ->
            appendResult("  $key -> $value")
        }

        // String -> Any 的 Map（动态数据）
        val dynamicJson = """{"name":"test","count":100,"enabled":true}"""
        val dynamicMap = GsonUtils.fromJsonToMap<String, Any>(dynamicJson)
        appendResult("\n动态 Map:")
        dynamicMap.forEach { (key, value) ->
            appendResult("  $key (${value::class.simpleName}) = $value")
        }

        // MutableMap 操作
        val mutableMap = GsonUtils.fromJsonToMutableMap<String, User>(userMapJson)
        mutableMap["newUser"] = User(4, "新用户", null, 25)
        appendResult("\nMutableMap 添加元素后大小: ${mutableMap.size}")
    }

    /**
     * 示例5: 安全解析（异常处理）
     *
     * 注意：GsonFactory 具有强大的容错能力，可以解析很多不规范的 JSON
     * 本示例使用完全无效的数据来演示异常处理
     */
    private fun demonstrateSafeParse() {
        appendResult("===== 安全解析示例 =====")
        appendResult("💡 注意: GsonFactory 有强大的容错能力")

        // 1. GsonFactory 容错示例：缺少引号的 key 也能解析
        val lenientJson = """{"id":1,"name":"张三",age:25}"""  // age 缺少引号
        appendResult("\n【容错测试】缺少引号的 JSON:")
        appendResult("  输入: $lenientJson")
        val lenientResult = GsonUtils.fromJsonToObject<User>(lenientJson)
        appendResult("  结果: $lenientResult")
        appendResult("  ✅ GsonFactory 容错解析成功！")

        // 2. 完全无效的 JSON（会触发异常）
        val invalidJson1 = "这根本不是JSON字符串"
        appendResult("\n【异常测试1】完全无效的字符串:")
        appendResult("  输入: $invalidJson1")
        val result1 = GsonUtils.fromJsonToObject<User>(invalidJson1)
        appendResult("  结果: $result1")

        // 3. 带默认值的异常处理
        val invalidJson2 = "{{{{invalid}}}}"
        appendResult("\n【异常测试2】带默认值:")
        appendResult("  输入: $invalidJson2")
        val result2 = GsonUtils.fromJsonToObjectOrDefault(
            invalidJson2,
            User(0, "默认用户", null, 0)
        )
        appendResult("  结果: $result2")

        // 4. 类型不匹配（数组解析为对象）
        val wrongTypeJson = """[1,2,3]"""
        appendResult("\n【类型测试】数组解析为对象:")
        appendResult("  输入: $wrongTypeJson")
        val result3 = GsonUtils.fromJsonToObject<User>(wrongTypeJson)
        appendResult("  结果: $result3")

        // 5. 空 JSON 对象
        val emptyObjectJson = "{}"
        appendResult("\n【边界测试】空 JSON 对象:")
        appendResult("  输入: $emptyObjectJson")
        val result4 = GsonUtils.fromJsonToObject<User>(emptyObjectJson)
        appendResult("  结果: $result4")

        // 6. List 安全解析
        val invalidListJson = "not_a_json_array_at_all"
        appendResult("\n【List测试】无效 JSON 解析为 List:")
        appendResult("  输入: $invalidListJson")
        val listResult = GsonUtils.fromJsonToList<User>(invalidListJson)
        appendResult("  结果: $listResult (返回默认空列表)")
    }

    /**
     * 示例6: 复杂嵌套对象
     */
    private fun demonstrateNestedObject() {
        appendResult("===== 复杂嵌套对象示例 =====")

        // 创建复杂对象
        val company = Company(
            name = "ABC 科技公司",
            employees = listOf(
                User(1, "张三", "zhangsan@abc.com", 25),
                User(2, "李四", "lisi@abc.com", 30)
            ),
            departments = mapOf(
                "技术部" to listOf(User(1, "张三", null, 25)),
                "市场部" to listOf(User(2, "李四", null, 30))
            )
        )

        // 序列化
        val json = GsonUtils.toJson(company)
        appendResult("Company 序列化:")
        appendResult(GsonUtils.toPrettyJson(company))

        // 反序列化
        val parsed = GsonUtils.fromJsonToObject<Company>(json)
        appendResult("\n反序列化结果:")
        appendResult("  公司名: ${parsed?.name}")
        appendResult("  员工数: ${parsed?.employees?.size}")
        appendResult("  部门数: ${parsed?.departments?.size}")

        parsed?.departments?.forEach { (dept, users) ->
            appendResult("  $dept: ${users.map { it.name }}")
        }
    }

    /**
     * 示例7: 使用 TypeToken 处理复杂泛型
     */
    private fun demonstrateTypeToken() {
        appendResult("===== TypeToken 使用示例 =====")

        // List<Map<String, User>> 这种复杂类型
        val complexJson = """
            [
                {"admin": {"id":1,"name":"管理员1","age":35}},
                {"admin": {"id":2,"name":"管理员2","age":40}}
            ]
        """.trimIndent()

        // 使用 TypeToken
        val typeToken = object : TypeToken<List<Map<String, User>>>() {}
        val result = GsonUtils.fromJsonSafe(complexJson, typeToken, emptyList())

        appendResult("List<Map<String, User>> 解析结果:")
        result?.forEachIndexed { index, map ->
            appendResult("  [$index]:")
            map.forEach { (key, user) ->
                appendResult("    $key -> $user")
            }
        }

        // 使用 getType 方法构建复杂类型
        val listMapType = GsonUtils.getType(
            List::class.java,
            GsonUtils.getMapType(String::class.java, User::class.java)
        )
        val result2: List<Map<String, User>> = GsonUtils.fromJson(complexJson, listMapType)
        appendResult("\n使用 getType 构建类型解析结果大小: ${result2.size}")
    }

    /**
     * 追加结果到显示区域
     */
    private fun appendResult(text: String) {
        val current = binding.tvResult.text.toString()
        binding.tvResult.text = if (current.isEmpty()) text else "$current\n$text"
        Log.d(TAG, text)
    }
}
