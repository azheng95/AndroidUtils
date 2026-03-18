package com.azheng.utils.demo

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.azheng.androidutils.GsonUtils
import com.azheng.utils.demo.bean.Company
import com.azheng.utils.demo.bean.User
import com.azheng.utils.demo.databinding.ActivityGsonBinding
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // 自定义适配器示例
        binding.btnCustomAdapter.setOnClickListener {
            demonstrateCustomAdapters()
        }

        // Gson 实例管理示例
        binding.btnGsonManagement.setOnClickListener {
            demonstrateGsonManagement()
        }

        // Set 和高级类型示例
        binding.btnAdvancedTypes.setOnClickListener {
            demonstrateAdvancedTypes()
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

        // 使用指定类型序列化
        val anyData: Any = userList
        val typedJson = GsonUtils.toJson(anyData, GsonUtils.getListType(User::class.java))
        appendResult("\n使用指定 Type 序列化:")
        appendResult(typedJson)
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

        // 使用 Type 解析
        val type = User::class.java
        val user4: User = GsonUtils.fromJson(json, type)
        appendResult("使用 Type 解析: $user4")
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

        // 使用自定义默认值
        val customDefault = listOf(User(0, "默认用户", null, 0))
        val resultWithDefault = GsonUtils.fromJsonToList<User>("invalid", customDefault)
        appendResult("自定义默认值: $resultWithDefault")
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

        // 嵌套 Map
        val nestedMapJson = """
            {
                "level1": {
                    "level2": {"value": 100}
                }
            }
        """.trimIndent()
        val nestedMap = GsonUtils.fromJsonToMap<String, Map<String, Any>>(nestedMapJson)
        appendResult("\n嵌套 Map: $nestedMap")
    }

    /**
     * 示例5: 安全解析（异常处理）
     */
    private fun demonstrateSafeParse() {
        appendResult("===== 安全解析示例 =====")
        appendResult("💡 注意: GsonFactory 有强大的容错能力")

        // 1. GsonFactory 容错示例：缺少引号的 key 也能解析
        val lenientJson = """{"id":1,"name":"张三",age:25}"""
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

        // 多层嵌套类型
        appendResult("\n===== 多层嵌套类型 =====")
        val deepNestedJson = """
            {
                "data": [
                    {"users": [{"id":1,"name":"用户1","age":20}]},
                    {"users": [{"id":2,"name":"用户2","age":25}]}
                ]
            }
        """.trimIndent()

        // 构建 Map<String, List<Map<String, List<User>>>> 类型
        val innerListType = GsonUtils.getListType(User::class.java)
        val innerMapType = GsonUtils.getMapType(String::class.java, innerListType)
        val outerListType = GsonUtils.getListType(innerMapType)
        val finalType = GsonUtils.getMapType(String::class.java, outerListType)

        val deepResult: Map<String, List<Map<String, List<User>>>> =
            GsonUtils.fromJson(deepNestedJson, finalType)
        appendResult("深层嵌套解析: data 列表大小 = ${deepResult["data"]?.size}")
    }

    /**
     * 示例8: 自定义适配器
     */
    private fun demonstrateCustomAdapters() {
        appendResult("===== 自定义适配器示例 =====")

        // 1. 注册 TypeAdapter（精确类型匹配）
        appendResult("\n【1. TypeAdapter 注册】")

        // Date 适配器
        val dateAdapter = object : TypeAdapter<Date>() {
            private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            override fun write(out: JsonWriter, value: Date?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.value(format.format(value))
                }
            }

            override fun read(reader: JsonReader): Date? {
                return try {
                    format.parse(reader.nextString())
                } catch (e: Exception) {
                    null
                }
            }
        }

        GsonUtils.registerTypeAdapter<Date>(dateAdapter)
        appendResult("  ✅ 已注册 Date TypeAdapter")

        // 测试 Date 序列化
        data class Event(val name: String, val date: Date)

        val event = Event("会议", Date())
        val eventJson = GsonUtils.toJson(event)
        appendResult("  Event 序列化: $eventJson")

        // 反序列化
        val parsedEvent = GsonUtils.fromJsonToObject<Event>(eventJson)
        appendResult("  Event 反序列化: $parsedEvent")

        // 2. 注册 TypeHierarchyAdapter（处理抽象类/接口）
        appendResult("\n【2. TypeHierarchyAdapter 注册】")

        // Uri 适配器示例
        val uriAdapter = object : TypeAdapter<Uri>() {
            override fun write(out: JsonWriter, value: Uri?) {
                if (value == null) {
                    out.nullValue()
                } else {
                    out.value(value.toString())
                }
            }

            override fun read(reader: JsonReader): Uri? {
                return try {
                    Uri.parse(reader.nextString())
                } catch (e: Exception) {
                    null
                }
            }
        }

        GsonUtils.registerTypeHierarchyAdapter<Uri>(uriAdapter)
        appendResult("  ✅ 已注册 Uri TypeHierarchyAdapter")

        // 测试 Uri 序列化
        data class Link(val title: String, val uri: Uri?)

        val link = Link("百度", Uri.parse("https://www.baidu.com"))
        val linkJson = GsonUtils.toJson(link)
        appendResult("  Link 序列化: $linkJson")

        val parsedLink = GsonUtils.fromJsonToObject<Link>(linkJson)
        appendResult("  Link 反序列化: $parsedLink")

        // 3. 查看已注册的适配器
        appendResult("\n【3. 已注册的适配器】")
        appendResult("  TypeAdapter 类型: ${GsonUtils.getRegisteredAdapterTypes()}")
        appendResult("  TypeHierarchyAdapter 类型: ${GsonUtils.getRegisteredHierarchyTypes()}")

        // 4. 检查适配器
        appendResult("\n【4. 检查适配器】")
        appendResult("  是否有 Date 适配器: ${GsonUtils.hasTypeAdapter(Date::class.java)}")
        appendResult("  是否有 Uri 层次适配器: ${GsonUtils.hasTypeHierarchyAdapter(Uri::class.java)}")
        appendResult("  是否有 String 适配器: ${GsonUtils.hasTypeAdapter(String::class.java)}")

        // 5. TypeAdapterFactory 示例
        appendResult("\n【5. TypeAdapterFactory 注册】")

        // 创建一个简单的日志工厂
        val loggingFactory = object : TypeAdapterFactory {
            override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                // 这里返回 null 表示不处理，让 Gson 使用默认适配器
                // 实际使用中可以在这里添加日志或包装逻辑
                return null
            }
        }

        GsonUtils.registerTypeAdapterFactory(loggingFactory)
        appendResult("  ✅ 已注册 LoggingFactory")

        // 6. InstanceCreator 示例（修复：使用正确的语法）
        appendResult("\n【6. InstanceCreator 注册】")

        // 为 User 注册默认实例创建器 - 使用命名参数明确指定
        GsonUtils.registerInstanceCreator<User>(
            creator = { _ -> User(0, "默认用户", null, 0) }
        )
        appendResult("  ✅ 已注册 User InstanceCreator")

        // 测试空 JSON 对象解析
        val emptyUser = GsonUtils.fromJsonToObject<User>("{}")
        appendResult("  空 JSON 解析结果: $emptyUser")

        // 7. 抽象类完整支持
        appendResult("\n【7. 抽象类完整支持】")
        GsonUtils.registerAbstractTypeSupport<Uri>(
            typeAdapter = uriAdapter,
            defaultValue = Uri.EMPTY
        )
        appendResult("  ✅ 已注册 Uri 完整支持（包含 InstanceCreator）")

        // 清理（可选）
        appendResult("\n【清理】")
        // GsonUtils.removeTypeAdapter(Date::class.java)
        // GsonUtils.removeTypeHierarchyAdapter(Uri::class.java)
        appendResult("  💡 适配器保持注册状态，后续可继续使用")
    }

    /**
     * 示例9: Gson 实例管理
     */
    private fun demonstrateGsonManagement() {
        appendResult("===== Gson 实例管理示例 =====")

        // 1. 获取默认 Gson
        appendResult("\n【1. 默认 Gson 实例】")
        val defaultGson = GsonUtils.gson
        appendResult("  默认 Gson: ${defaultGson.javaClass.simpleName}")

        // 2. 获取日志专用 Gson
        val logGson = GsonUtils.gson4LogUtils
        appendResult("  日志 Gson: ${logGson.javaClass.simpleName}")

        // 3. 创建自定义 Gson
        appendResult("\n【2. 自定义 Gson 实例】")

        // 使用 newGsonBuilder 创建（包含已注册的适配器）
        val customGson1 = GsonUtils.newGsonBuilder()
            .setDateFormat("yyyy年MM月dd日")
            .create()
        GsonUtils.setGson("custom_date", customGson1)
        appendResult("  ✅ 创建并缓存 'custom_date' Gson")

        // 创建另一个自定义实例
        val customGson2 = GsonUtils.newGsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
        GsonUtils.setGson("expose_only", customGson2)
        appendResult("  ✅ 创建并缓存 'expose_only' Gson")

        // 4. 获取已缓存的 Gson
        appendResult("\n【3. 获取缓存的 Gson】")
        val cached = GsonUtils.getGson("custom_date")
        appendResult("  获取 'custom_date': ${if (cached != null) "成功" else "失败"}")
        appendResult("  是否存在 'custom_date': ${GsonUtils.containsGson("custom_date")}")
        appendResult("  是否存在 'not_exists': ${GsonUtils.containsGson("not_exists")}")

        // 5. 查看所有已注册的 key
        appendResult("\n【4. 所有已注册的 Gson】")
        val keys = GsonUtils.getRegisteredKeys()
        keys.forEach { appendResult("  - $it") }

        // 6. 使用特定 Gson 进行序列化
        appendResult("\n【5. 使用特定 Gson】")
        val dateGson = GsonUtils.getGson("custom_date")
        if (dateGson != null) {
            data class DateTest(val date: Date)

            val test = DateTest(Date())
            val json = dateGson.toJson(test)
            appendResult("  使用 custom_date Gson: $json")
        }

        // 7. 设置代理 Gson
        appendResult("\n【6. 设置代理 Gson】")
        val proxyGson = GsonUtils.newGsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create()
        GsonUtils.setGsonDelegate(proxyGson)
        appendResult("  ✅ 已设置新的代理 Gson")

        // 8. 刷新 Gson 实例
        appendResult("\n【7. 刷新 Gson 实例】")
        GsonUtils.refreshGsonInstance()
        appendResult("  ✅ 已刷新主 Gson 实例")

        // 9. 移除缓存的 Gson
        appendResult("\n【8. 清理】")
        val removed = GsonUtils.removeGson("custom_date")
        appendResult("  移除 'custom_date': ${if (removed != null) "成功" else "不存在"}")

        GsonUtils.removeGson("expose_only")
        appendResult("  移除 'expose_only': 成功")

        appendResult("  当前注册的 Gson: ${GsonUtils.getRegisteredKeys()}")
    }

    /**
     * 示例10: Set 和高级类型
     */
    private fun demonstrateAdvancedTypes() {
        appendResult("===== 高级类型示例 =====")

        // 1. Set 反序列化
        appendResult("\n【1. Set 反序列化】")
        val setJson = """[1, 2, 3, 2, 1, 4, 5]"""

        val intSet = GsonUtils.fromJsonToSet<Int>(setJson)
        appendResult("  JSON: $setJson")
        appendResult("  Set<Int>: $intSet")
        appendResult("  大小: ${intSet.size} (去重后)")

        // MutableSet
        val mutableSet = GsonUtils.fromJsonToMutableSet<Int>(setJson)
        mutableSet.add(6)
        appendResult("  MutableSet 添加 6 后: $mutableSet")

        // User Set
        val userSetJson = """
            [
                {"id":1,"name":"张三","age":25},
                {"id":2,"name":"李四","age":30},
                {"id":1,"name":"张三","age":25}
            ]
        """.trimIndent()
        val userSet = GsonUtils.fromJsonToSet<User>(userSetJson)
        appendResult("  User Set 大小: ${userSet.size}")

        // 2. Array Type
        appendResult("\n【2. Array 类型】")
        val arrayJson = """[1, 2, 3, 4, 5]"""
        val arrayType = GsonUtils.getArrayType(Int::class.javaObjectType)
        val intArray: Array<Int> = GsonUtils.fromJson(arrayJson, arrayType)
        appendResult("  Array<Int>: ${intArray.contentToString()}")

        // 3. Type 工具方法
        appendResult("\n【3. Type 构建工具】")

        // 构建各种类型
        val listType = GsonUtils.getListType(User::class.java)
        appendResult("  List<User> Type: $listType")

        val setType = GsonUtils.getSetType(String::class.java)
        appendResult("  Set<String> Type: $setType")

        val mapType = GsonUtils.getMapType(String::class.java, User::class.java)
        appendResult("  Map<String, User> Type: $mapType")

        // 复杂嵌套类型
        val nestedType = GsonUtils.getType(
            Map::class.java,
            String::class.java,
            GsonUtils.getListType(User::class.java)
        )
        appendResult("  Map<String, List<User>> Type: $nestedType")

        // 4. 使用构建的类型进行解析
        appendResult("\n【4. 使用构建的类型解析】")
        val nestedJson = """
            {
                "team1": [{"id":1,"name":"成员1","age":25}],
                "team2": [{"id":2,"name":"成员2","age":30}]
            }
        """.trimIndent()

        val teamData: Map<String, List<User>> = GsonUtils.fromJson(nestedJson, nestedType)
        teamData.forEach { (team, members) ->
            appendResult("  $team: ${members.map { it.name }}")
        }

        // 5. Type 缓存
        appendResult("\n【5. Type 缓存】")
        val type1 = GsonUtils.getListType(User::class.java)
        val type2 = GsonUtils.getListType(User::class.java)
        appendResult("  相同类型是否复用: ${type1 === type2}")

        // 清空缓存
        GsonUtils.clearTypeCache()
        val type3 = GsonUtils.getListType(User::class.java)
        appendResult("  清空缓存后: ${type1 === type3}")

        // 6. 解析回调
        appendResult("\n【6. 解析异常回调】")
        val originalHandler = GsonUtils.parseExceptionHandler
        var exceptionCaught = false

        GsonUtils.parseExceptionHandler = { e, _ ->
            exceptionCaught = true
            appendResult("  捕获异常: ${e.javaClass.simpleName}")
        }

        GsonUtils.fromJsonToObject<User>("invalid_json_data")
        appendResult("  是否捕获异常: $exceptionCaught")

        // 恢复原始处理器
        GsonUtils.parseExceptionHandler = originalHandler
    }

    /**
     * 追加结果到显示区域
     */
    private fun appendResult(text: String) {
        val current = binding.tvResult.text.toString()
        binding.tvResult.text = if (current.isEmpty()) text else "$current\n$text"

        // 自动滚动到底部
        binding.tvResult.post {
            val scrollView = binding.tvResult.parent as? android.widget.ScrollView
            scrollView?.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
        }

        Log.d(TAG, text)
    }
}
