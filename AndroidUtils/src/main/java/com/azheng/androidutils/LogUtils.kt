package com.azheng.androidutils

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.collection.SimpleArrayMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource
import kotlin.math.min

/**
 *         // 配置 LogUtils
 *         LogUtils.config
 *             .setLogSwitch(BuildConfig.DEBUG)     // 总开关（Release时关闭）
 *             .setConsoleSwitch(true)              // 控制台输出开关
 *             .setLog2FileSwitch(true)             // 写入文件开关
 *             .setGlobalTag("MyApp")               // 全局Tag
 *             .setBorderSwitch(true)               // 显示边框
 *             .setLogHeadSwitch(true)              // 显示头部信息（线程、类名、行号）
 *             .setFilePrefix("app_log")            // 日志文件前缀
 *             .setFileExtension(".txt")            // 日志文件后缀
 *             .setSaveDays(7)                      // 日志保存天数
 *             .setConsoleFilter(LogUtils.V)        // 控制台过滤级别
 *             .setFileFilter(LogUtils.I)           // 文件过滤级别
 *             .setStackDeep(1)                     // 堆栈深度
 *             .setBufferSize(10)                   // 缓冲区大小
 *             .setFlushInterval(3000L)             // 刷新间隔（毫秒）
 */
class LogUtils private constructor() {

    @IntDef(V, D, I, W, E, A)
    @Retention(AnnotationRetention.SOURCE)
    annotation class TYPE

    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }

    class Config internal constructor() {
        var defaultDir: String? = null
            private set
        private var mDir: String? = null
        var filePrefix: String = "util"
            private set
        var fileExtension: String = ".txt"
            private set
        var isLogSwitch: Boolean = true
            private set
        var isLog2ConsoleSwitch: Boolean = true
            private set
        private var mGlobalTag = ""
        var mTagIsSpace: Boolean = true
        var isLogHeadSwitch: Boolean = true
            private set
        var isLog2FileSwitch: Boolean = false
            private set
        var isLogBorderSwitch: Boolean = true
            private set
        var isSingleTagSwitch: Boolean = true
            private set
        var mConsoleFilter: Int = V
        var mFileFilter: Int = V
        var stackDeep: Int = 1
            private set
        var stackOffset: Int = 0
            private set
        var saveDays: Int = -1
            private set

        // 缓冲区配置
        var bufferSize: Int = 10
            private set
        var flushIntervalMs: Long = 3000L
            private set

        var mFileWriter: IFileWriter? = null
        var mOnConsoleOutputListener: OnConsoleOutputListener? = null
        var mOnFileOutputListener: OnFileOutputListener? = null
        internal val mFileHead = FileHead("Log")

        init {
            if (isSDCardEnableByEnvironment
                && Utils.getApplication().getExternalFilesDir(null) != null
            ) {
                defaultDir = Utils.getApplication().getExternalFilesDir(null)
                    .toString() + FILE_SEP + "log" + FILE_SEP
            } else {
                defaultDir =
                    Utils.getApplication().filesDir.toString() + FILE_SEP + "log" + FILE_SEP
            }

            // 自动注册生命周期监听
            registerLifecycleIfNeeded()
        }

        fun setLogSwitch(logSwitch: Boolean): Config {
            isLogSwitch = logSwitch
            return this
        }

        fun setConsoleSwitch(consoleSwitch: Boolean): Config {
            isLog2ConsoleSwitch = consoleSwitch
            return this
        }

        fun setGlobalTag(tag: String): Config {
            if (isSpace(tag)) {
                mGlobalTag = ""
                mTagIsSpace = true
            } else {
                mGlobalTag = tag
                mTagIsSpace = false
            }
            return this
        }

        fun setLogHeadSwitch(logHeadSwitch: Boolean): Config {
            isLogHeadSwitch = logHeadSwitch
            return this
        }

        fun setLog2FileSwitch(log2FileSwitch: Boolean): Config {
            isLog2FileSwitch = log2FileSwitch
            return this
        }

        fun setDir(dir: String): Config {
            mDir = if (isSpace(dir)) {
                null
            } else {
                if (dir.endsWith(FILE_SEP)) dir else dir + FILE_SEP
            }
            return this
        }

        fun setDir(dir: File?): Config {
            mDir = if (dir == null) null else (dir.absolutePath + FILE_SEP)
            return this
        }

        fun setFilePrefix(filePrefix: String): Config {
            if (isSpace(filePrefix)) {
                this.filePrefix = "util"
            } else {
                this.filePrefix = filePrefix
            }
            return this
        }

        fun setFileExtension(fileExtension: String): Config {
            if (isSpace(fileExtension)) {
                this.fileExtension = ".txt"
            } else {
                this.fileExtension = if (fileExtension.startsWith(".")) {
                    fileExtension
                } else {
                    ".$fileExtension"
                }
            }
            return this
        }

        fun setBorderSwitch(borderSwitch: Boolean): Config {
            isLogBorderSwitch = borderSwitch
            return this
        }

        fun setSingleTagSwitch(singleTagSwitch: Boolean): Config {
            isSingleTagSwitch = singleTagSwitch
            return this
        }

        fun setConsoleFilter(@TYPE consoleFilter: Int): Config {
            mConsoleFilter = consoleFilter
            return this
        }

        fun setFileFilter(@TYPE fileFilter: Int): Config {
            mFileFilter = fileFilter
            return this
        }

        fun setStackDeep(@IntRange(from = 1) stackDeep: Int): Config {
            this.stackDeep = stackDeep
            return this
        }

        fun setStackOffset(@IntRange(from = 0) stackOffset: Int): Config {
            this.stackOffset = stackOffset
            return this
        }

        fun setSaveDays(@IntRange(from = 1) saveDays: Int): Config {
            this.saveDays = saveDays
            return this
        }

        fun setBufferSize(@IntRange(from = 1) bufferSize: Int): Config {
            this.bufferSize = bufferSize
            return this
        }

        fun setFlushInterval(intervalMs: Long): Config {
            this.flushIntervalMs = intervalMs
            return this
        }

        fun <T> addFormatter(iFormatter: IFormatter<T>?): Config {
            if (iFormatter != null) {
                I_FORMATTER_MAP.put(getTypeClassFromParadigm(iFormatter), iFormatter)
            }
            return this
        }

        fun setFileWriter(fileWriter: IFileWriter?): Config {
            mFileWriter = fileWriter
            return this
        }

        fun setOnConsoleOutputListener(listener: OnConsoleOutputListener?): Config {
            mOnConsoleOutputListener = listener
            return this
        }

        fun setOnFileOutputListener(listener: OnFileOutputListener?): Config {
            mOnFileOutputListener = listener
            return this
        }

        fun addFileExtraHead(fileExtraHead: Map<String, String?>?): Config {
            mFileHead.append(fileExtraHead)
            return this
        }

        fun addFileExtraHead(key: String, value: String?): Config {
            mFileHead.append(key, value)
            return this
        }

        val dir: String?
            get() = mDir ?: defaultDir

        val globalTag: String
            get() = if (isSpace(mGlobalTag)) "" else mGlobalTag

        val consoleFilter: Char
            get() = T[mConsoleFilter - V]

        val fileFilter: Char
            get() = T[mFileFilter - V]

        fun haveSetOnConsoleOutputListener(): Boolean = mOnConsoleOutputListener != null

        fun haveSetOnFileOutputListener(): Boolean = mOnFileOutputListener != null

        override fun toString(): String {
            return buildString {
                append(LINE_SEP).append("logSwitch: ").append(isLogSwitch)
                append(LINE_SEP).append("consoleSwitch: ").append(isLog2ConsoleSwitch)
                append(LINE_SEP).append("tag: ").append(if (globalTag == "") "null" else globalTag)
                append(LINE_SEP).append("headSwitch: ").append(isLogHeadSwitch)
                append(LINE_SEP).append("fileSwitch: ").append(isLog2FileSwitch)
                append(LINE_SEP).append("dir: ").append(dir)
                append(LINE_SEP).append("filePrefix: ").append(filePrefix)
                append(LINE_SEP).append("borderSwitch: ").append(isLogBorderSwitch)
                append(LINE_SEP).append("singleTagSwitch: ").append(isSingleTagSwitch)
                append(LINE_SEP).append("consoleFilter: ").append(consoleFilter)
                append(LINE_SEP).append("fileFilter: ").append(fileFilter)
                append(LINE_SEP).append("stackDeep: ").append(stackDeep)
                append(LINE_SEP).append("stackOffset: ").append(stackOffset)
                append(LINE_SEP).append("saveDays: ").append(saveDays)
                append(LINE_SEP).append("bufferSize: ").append(bufferSize)
                append(LINE_SEP).append("flushIntervalMs: ").append(flushIntervalMs)
                append(LINE_SEP).append("formatter: ").append(I_FORMATTER_MAP)
                append(LINE_SEP).append("fileWriter: ").append(mFileWriter)
                append(LINE_SEP).append("onConsoleOutputListener: ").append(mOnConsoleOutputListener)
                append(LINE_SEP).append("onFileOutputListener: ").append(mOnFileOutputListener)
                append(LINE_SEP).append("fileExtraHeader: ").append(mFileHead.appended)
            }
        }
    }

    abstract class IFormatter<T> {
        abstract fun format(t: Any): String
    }

    interface IFileWriter {
        fun write(file: String?, content: String?)
    }

    interface OnConsoleOutputListener {
        fun onConsoleOutput(@TYPE type: Int, tag: String?, content: String?)
    }

    interface OnFileOutputListener {
        fun onFileOutput(filePath: String?, content: String?)
    }

    private data class TagHead(
        val tag: String,
        val consoleHead: Array<String?>?,
        val fileHead: String
    )

    private data class FileLogEntry(
        val type: Int,
        val tag: String,
        val content: String,
        val timestamp: Date
    )

    private object LogFormatter {
        private val transformerFactory by lazy { TransformerFactory.newInstance() }

        fun object2String(`object`: Any, type: Int = -1): String {
            return when {
                `object`.javaClass.isArray -> array2String(`object`)
                `object` is Throwable -> getFullStackTrace(`object`)
                `object` is Bundle -> bundle2String(`object`)
                `object` is Intent -> intent2String(`object`)
                type == JSON -> object2Json(`object`)
                type == XML -> formatXml(`object`.toString())
                else -> `object`.toString()
            }
        }

        fun bundle2String(bundle: Bundle): String {
            val iterator = bundle.keySet().iterator()
            if (!iterator.hasNext()) {
                return "Bundle {}"
            }
            return buildString(128) {
                append("Bundle { ")
                while (true) {
                    val key = iterator.next()
                    val value = bundle[key]
                    append(key).append('=')
                    if (value is Bundle) {
                        append(if (value === bundle) "(this Bundle)" else bundle2String(value))
                    } else {
                        append(formatObject(value))
                    }
                    if (!iterator.hasNext()) {
                        append(" }")
                        break
                    }
                    append(", ")
                }
            }
        }

        fun intent2String(intent: Intent): String {
            return buildString(128) {
                append("Intent { ")
                var first = true

                intent.action?.let {
                    append("act=").append(it)
                    first = false
                }

                intent.categories?.let { categories ->
                    if (!first) append(' ')
                    first = false
                    append("cat=[")
                    append(categories.joinToString(","))
                    append("]")
                }

                intent.data?.let {
                    if (!first) append(' ')
                    first = false
                    append("dat=").append(it)
                }

                intent.type?.let {
                    if (!first) append(' ')
                    first = false
                    append("typ=").append(it)
                }

                if (intent.flags != 0) {
                    if (!first) append(' ')
                    first = false
                    append("flg=0x").append(Integer.toHexString(intent.flags))
                }

                intent.`package`?.let {
                    if (!first) append(' ')
                    first = false
                    append("pkg=").append(it)
                }

                intent.component?.let {
                    if (!first) append(' ')
                    first = false
                    append("cmp=").append(it.flattenToShortString())
                }

                intent.sourceBounds?.let {
                    if (!first) append(' ')
                    first = false
                    append("bnds=").append(it.toShortString())
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    intent.clipData?.let { clipData ->
                        if (!first) append(' ')
                        first = false
                        clipData2String(clipData, this)
                    }
                }

                intent.extras?.let {
                    if (!first) append(' ')
                    first = false
                    append("extras={").append(bundle2String(it)).append('}')
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    intent.selector?.let { selector ->
                        if (!first) append(' ')
                        append("sel={")
                        append(if (selector === intent) "(this Intent)" else intent2String(selector))
                        append("}")
                    }
                }
                append(" }")
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        private fun clipData2String(clipData: ClipData, sb: StringBuilder) {
            val item = clipData.getItemAt(0)
            if (item == null) {
                sb.append("ClipData.Item {}")
                return
            }
            sb.append("ClipData.Item { ")
            when {
                item.htmlText != null -> sb.append("H:").append(item.htmlText)
                item.text != null -> sb.append("T:").append(item.text)
                item.uri != null -> sb.append("U:").append(item.uri)
                item.intent != null -> sb.append("I:").append(intent2String(item.intent))
                else -> sb.append("NULL")
            }
            sb.append("}")
        }

        fun object2Json(`object`: Any): String {
            if (`object` is CharSequence) {
                return formatJson(`object`.toString())
            }
            return try {
                GsonUtils.gson4LogUtils.toJson(`object`)
            } catch (t: Throwable) {
                `object`.toString()
            }
        }

        fun formatJson(json: String): String {
            try {
                for (c in json) {
                    when {
                        c == '{' -> return JSONObject(json).toString(2)
                        c == '[' -> return JSONArray(json).toString(2)
                        !Character.isWhitespace(c) -> return json
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return json
        }

        fun formatXml(xml: String): String {
            return try {
                val xmlInput = StreamSource(StringReader(xml))
                val xmlOutput = StreamResult(StringWriter())
                val transformer = transformerFactory.newTransformer()
                transformer.setOutputProperty(OutputKeys.INDENT, "yes")
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                transformer.transform(xmlInput, xmlOutput)
                xmlOutput.writer.toString().replaceFirst(">", ">$LINE_SEP")
            } catch (e: Exception) {
                e.printStackTrace()
                xml
            }
        }

        fun array2String(`object`: Any): String {
            return when (`object`) {
                is Array<*> -> `object`.contentDeepToString()
                is BooleanArray -> `object`.contentToString()
                is ByteArray -> `object`.contentToString()
                is CharArray -> `object`.contentToString()
                is DoubleArray -> `object`.contentToString()
                is FloatArray -> `object`.contentToString()
                is IntArray -> `object`.contentToString()
                is LongArray -> `object`.contentToString()
                is ShortArray -> `object`.contentToString()
                else -> throw IllegalArgumentException("Array has incompatible type: ${`object`.javaClass}")
            }
        }
    }

    internal class FileHead(private val mName: String) {
        private val mFirst = LinkedHashMap<String, String?>()
        private val mLast = LinkedHashMap<String, String?>()

        fun addFirst(key: String, value: String?) {
            append2Host(mFirst, key, value)
        }

        fun append(extra: Map<String, String?>?) {
            if (extra.isNullOrEmpty()) return
            extra.forEach { (key, value) -> append2Host(mLast, key, value) }
        }

        fun append(key: String, value: String?) {
            append2Host(mLast, key, value)
        }

        private fun append2Host(host: MutableMap<String, String?>, key: String, value: String?) {
            if (key.isEmpty() || value.isNullOrEmpty()) return
            val delta = 19 - key.length
            val paddedKey = if (delta > 0) {
                key + " ".repeat(delta)
            } else key
            host[paddedKey] = value
        }

        val appended: String
            get() = buildString {
                mLast.forEach { (key, value) ->
                    append(key).append(": ").append(value).append("\n")
                }
            }

        override fun toString(): String {
            return buildString {
                val border = "************* $mName Head ****************\n"
                append(border)
                mFirst.forEach { (key, value) ->
                    append(key).append(": ").append(value).append("\n")
                }
                append("Device Manufacturer: ").append(Build.MANUFACTURER).append("\n")
                append("Device Model       : ").append(Build.MODEL).append("\n")
                append("Android Version    : ").append(Build.VERSION.RELEASE).append("\n")
                append("Android SDK        : ").append(Build.VERSION.SDK_INT).append("\n")
                append("App VersionName    : ").append(AppUtils.getAppVersionName()).append("\n")
                append("App VersionCode    : ").append(AppUtils.getAppVersionCode()).append("\n")
                append(appended)
                append(border).append("\n")
            }
        }
    }

    companion object {
        const val V: Int = Log.VERBOSE
        const val D: Int = Log.DEBUG
        const val I: Int = Log.INFO
        const val W: Int = Log.WARN
        const val E: Int = Log.ERROR
        const val A: Int = Log.ASSERT

        private val T = charArrayOf('V', 'D', 'I', 'W', 'E', 'A')

        private const val FILE = 0x10
        private const val JSON = 0x20
        private const val XML = 0x30

        private val FILE_SEP: String = System.getProperty("file.separator") ?: "/"
        private val LINE_SEP: String = System.getProperty("line.separator") ?: "\n"
        private const val TOP_CORNER = "┌"
        private const val MIDDLE_CORNER = "├"
        private const val LEFT_BORDER = "│ "
        private const val BOTTOM_CORNER = "└"
        private const val SIDE_DIVIDER =
            "────────────────────────────────────────────────────────"
        private const val MIDDLE_DIVIDER =
            "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"
        private const val TOP_BORDER = TOP_CORNER + SIDE_DIVIDER + SIDE_DIVIDER
        private const val MIDDLE_BORDER = MIDDLE_CORNER + MIDDLE_DIVIDER + MIDDLE_DIVIDER
        private const val BOTTOM_BORDER = BOTTOM_CORNER + SIDE_DIVIDER + SIDE_DIVIDER
        private const val MAX_LEN = 1100
        private const val NOTHING = "log nothing"
        private const val NULL = "null"
        private const val ARGS = "args"
        private const val PLACEHOLDER = " "

        val config: Config by lazy { Config() }

        // ==================== 生命周期监听相关 ====================

        private val isLifecycleRegistered = AtomicBoolean(false)
        private val activeActivityCount = AtomicInteger(0)

        private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
        private var flushCoroutineScope: CoroutineScope? = null
        private val delayFlushJob = AtomicReference<Job?>(null)

        // 延迟刷新时间（毫秒）
        private const val DELAY_FLUSH_MS = 1000L

        // ==================== 协程和Channel相关（支持重建） ====================

        private val threadLocalSdf = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy_MM_dd HH:mm:ss.SSS ", Locale.getDefault())
            }
        }

        private val I_FORMATTER_MAP = SimpleArrayMap<Class<*>?, IFormatter<*>>()

        private const val MAX_CHANNEL_CAPACITY = 1000

        // 使用 AtomicReference 包装，支持重建
        private val logScopeRef = AtomicReference<CoroutineScope?>(null)
        private val fileLogChannelRef = AtomicReference<Channel<FileLogEntry>?>(null)

        // 标记文件写入协程是否已启动
        private val isFileWriterStarted = AtomicBoolean(false)

        private val writeBuffer = StringBuilder()
        private val bufferMutex = Mutex()
        private var lastFlushTime = System.currentTimeMillis()

        /**
         * 获取或创建日志协程作用域
         */
        private fun getOrCreateLogScope(): CoroutineScope {
            var scope = logScopeRef.get()
            if (scope == null || !scope.isActive) {
                scope = CoroutineScope(
                    SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
                        Log.e("LogUtils", "Coroutine exception: ${throwable.message}")
                    }
                )
                logScopeRef.set(scope)
            }
            return scope
        }

        /**
         * 获取或创建日志 Channel
         */
        private fun getOrCreateFileLogChannel(): Channel<FileLogEntry> {
            var channel = fileLogChannelRef.get()
            if (channel == null || channel.isClosedForSend) {
                channel = Channel(MAX_CHANNEL_CAPACITY)
                fileLogChannelRef.set(channel)
            }
            return channel
        }

        init {
            // 初始化时启动文件写入协程
            ensureFileWriterStarted()
        }

        // ==================== 初始化检查 ====================

        /**
         * 确保已初始化（在每次日志操作前调用）
         */
        private fun ensureInitialized() {
            // 确保文件写入协程已启动
            ensureFileWriterStarted()

            // 确保生命周期已注册
            if (!isLifecycleRegistered.get()) {
                registerLifecycleIfNeeded()
            }
        }

        /**
         * 确保文件写入协程已启动
         */
        private fun ensureFileWriterStarted() {
            if (isFileWriterStarted.compareAndSet(false, true)) {
                startFileWriterCoroutine()
            } else {
                // 检查协程是否仍然活跃，如果不活跃则重启
                val scope = logScopeRef.get()
                if (scope == null || !scope.isActive) {
                    isFileWriterStarted.set(false)
                    if (isFileWriterStarted.compareAndSet(false, true)) {
                        startFileWriterCoroutine()
                    }
                }
            }
        }

        // ==================== 生命周期监听方法 ====================

        /**
         * 注册生命周期监听（只注册一次，不会自动注销）
         */
        private fun registerLifecycleIfNeeded() {
            // 使用 CAS 确保只注册一次
            if (!isLifecycleRegistered.compareAndSet(false, true)) {
                return
            }

            try {
                val app = Utils.getApplication()

                // 创建或重建协程作用域
                flushCoroutineScope?.cancel()
                flushCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

                lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
                    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                        activeActivityCount.incrementAndGet()
                        // 取消延迟刷新（从后台切回前台）
                        cancelDelayedFlush()
                    }

                    override fun onActivityStarted(activity: Activity) {}
                    override fun onActivityResumed(activity: Activity) {}
                    override fun onActivityPaused(activity: Activity) {}
                    override fun onActivityStopped(activity: Activity) {}
                    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                    override fun onActivityDestroyed(activity: Activity) {
                        val count = activeActivityCount.decrementAndGet()
                        // 当所有 Activity 销毁时，延迟刷新日志（但不注销监听）
                        if (count <= 0) {
                            activeActivityCount.set(0)
                            scheduleDelayedFlush()
                        }
                    }
                }

                app.registerActivityLifecycleCallbacks(lifecycleCallbacks)

            } catch (e: Exception) {
                isLifecycleRegistered.set(false)
                Log.e("LogUtils", "Failed to register lifecycle: ${e.message}")
            }
        }

        /**
         * 取消延迟刷新
         */
        private fun cancelDelayedFlush() {
            delayFlushJob.getAndSet(null)?.cancel()
        }

        /**
         * 调度延迟刷新（只刷新，不注销）
         */
        private fun scheduleDelayedFlush() {
            val job = flushCoroutineScope?.launch {
                delay(DELAY_FLUSH_MS)
                // 再次检查是否仍无活跃 Activity
                if (activeActivityCount.get() <= 0) {
                    // 只刷新日志，不注销监听
                    getOrCreateLogScope().launch {
                        flushBufferImmediate()
                    }
                }
            }
            delayFlushJob.getAndSet(job)?.cancel()
        }

        // ==================== 文件写入协程 ====================

        private fun startFileWriterCoroutine() {
            val scope = getOrCreateLogScope()
            val channel = getOrCreateFileLogChannel()

            // 日志处理协程
            scope.launch {
                try {
                    for (entry in channel) {
                        processFileLogEntry(entry)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.e("LogUtils", "File writer coroutine error: ${e.message}")
                    }
                }
            }

            // 定时刷新协程
            scope.launch {
                try {
                    while (isActive) {
                        delay(config.flushIntervalMs)
                        flushBufferIfNeeded()
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.e("LogUtils", "Flush coroutine error: ${e.message}")
                    }
                }
            }
        }

        private suspend fun processFileLogEntry(entry: FileLogEntry) {
            val format = threadLocalSdf.get()!!.format(entry.timestamp)
            val content = buildString {
                append(format.substring(11))
                append(T[entry.type - V])
                append("/")
                append(entry.tag)
                append(entry.content)
                append(LINE_SEP)
            }

            bufferMutex.withLock {
                writeBuffer.append(content)

                val shouldFlush = writeBuffer.length >= config.bufferSize * 200 ||
                        (System.currentTimeMillis() - lastFlushTime) >= config.flushIntervalMs

                if (shouldFlush) {
                    flushBufferInternal(entry.timestamp)
                }
            }
        }

        private suspend fun flushBufferIfNeeded() {
            bufferMutex.withLock {
                if (writeBuffer.isNotEmpty()) {
                    flushBufferInternal(Date())
                }
            }
        }

        private suspend fun flushBufferImmediate() {
            bufferMutex.withLock {
                if (writeBuffer.isNotEmpty()) {
                    flushBufferInternal(Date())
                }
            }
        }

        private fun flushBufferInternal(date: Date) {
            if (writeBuffer.isEmpty()) return

            val content = writeBuffer.toString()
            writeBuffer.clear()
            lastFlushTime = System.currentTimeMillis()

            val filePath = getCurrentLogFilePath(date)
            val format = threadLocalSdf.get()!!.format(date)
            val dateStr = format.substring(0, 10)

            if (!createOrExistsFile(filePath, dateStr)) {
                Log.e("LogUtils", "create $filePath failed!")
                return
            }

            input2File(filePath, content)
        }

        /**
         * 异步刷新缓冲区
         */
        fun flush() {
            getOrCreateLogScope().launch {
                flushBufferImmediate()
            }
        }

        /**
         * 同步刷新缓冲区（阻塞调用）
         */
        fun flushSync() {
            runBlocking {
                flushBufferImmediate()
            }
        }

        /**
         * 释放资源
         * 注意：释放后可以重新使用，会自动重新初始化
         */
        fun release() {
            // 1. 先同步刷新缓冲区
            flushSync()

            // 2. 取消延迟刷新任务
            cancelDelayedFlush()

            // 3. 取消刷新协程作用域
            flushCoroutineScope?.cancel()
            flushCoroutineScope = null

            // 4. 注销生命周期监听
            try {
                lifecycleCallbacks?.let {
                    Utils.getApplication().unregisterActivityLifecycleCallbacks(it)
                }
            } catch (e: Exception) {
                Log.e("LogUtils", "Failed to unregister: ${e.message}")
            }

            lifecycleCallbacks = null
            isLifecycleRegistered.set(false)
            activeActivityCount.set(0)

            // 5. 取消日志协程（但允许重建）
            logScopeRef.getAndSet(null)?.cancel()

            // 6. 关闭 Channel（但允许重建）
            fileLogChannelRef.getAndSet(null)?.close()

            // 7. 重置文件写入协程启动标志
            isFileWriterStarted.set(false)
        }

        // ==================== 日志打印方法 ====================

        fun v(vararg contents: Any?) {
            log(V, config.globalTag, *contents)
        }

        fun vTag(tag: String, vararg contents: Any?) {
            log(V, tag, *contents)
        }

        fun d(vararg contents: Any?) {
            log(D, config.globalTag, *contents)
        }

        fun dTag(tag: String, vararg contents: Any?) {
            log(D, tag, *contents)
        }

        fun i(vararg contents: Any?) {
            log(I, config.globalTag, *contents)
        }

        fun iTag(tag: String, vararg contents: Any?) {
            log(I, tag, *contents)
        }

        fun w(vararg contents: Any?) {
            log(W, config.globalTag, *contents)
        }

        fun wTag(tag: String, vararg contents: Any?) {
            log(W, tag, *contents)
        }

        fun e(vararg contents: Any?) {
            log(E, config.globalTag, *contents)
        }

        fun eTag(tag: String, vararg contents: Any?) {
            log(E, tag, *contents)
        }

        fun a(vararg contents: Any?) {
            log(A, config.globalTag, *contents)
        }

        fun aTag(tag: String, vararg contents: Any?) {
            log(A, tag, *contents)
        }

        fun file(content: Any?) {
            log(FILE or D, config.globalTag, content)
        }

        fun file(@TYPE type: Int, content: Any?) {
            log(FILE or type, config.globalTag, content)
        }

        fun file(tag: String, content: Any?) {
            log(FILE or D, tag, content)
        }

        fun file(@TYPE type: Int, tag: String, content: Any?) {
            log(FILE or type, tag, content)
        }

        fun json(content: Any?) {
            log(JSON or D, config.globalTag, content)
        }

        fun json(@TYPE type: Int, content: Any?) {
            log(JSON or type, config.globalTag, content)
        }

        fun json(tag: String, content: Any?) {
            log(JSON or D, tag, content)
        }

        fun json(@TYPE type: Int, tag: String, content: Any?) {
            log(JSON or type, tag, content)
        }

        fun xml(content: String?) {
            log(XML or D, config.globalTag, content)
        }

        fun xml(@TYPE type: Int, content: String?) {
            log(XML or type, config.globalTag, content)
        }

        fun xml(tag: String, content: String?) {
            log(XML or D, tag, content)
        }

        fun xml(@TYPE type: Int, tag: String, content: String?) {
            log(XML or type, tag, content)
        }

        fun log(type: Int, tag: String, vararg contents: Any?) {
            if (!config.isLogSwitch) return

            // 确保已初始化
            ensureInitialized()

            val typeLow = type and 0x0f
            val typeHigh = type and 0xf0

            if (!config.isLog2ConsoleSwitch && !config.isLog2FileSwitch && typeHigh != FILE) {
                return
            }

            if (typeLow < config.mConsoleFilter && typeLow < config.mFileFilter) {
                return
            }

            val tagHead = processTagAndHead(tag)
            val channel = getOrCreateFileLogChannel()

            // 控制台输出保持同步
            if (config.isLog2ConsoleSwitch && typeHigh != FILE && typeLow >= config.mConsoleFilter) {
                val body = processBody(typeHigh, *contents)
                print2Console(typeLow, tagHead.tag, tagHead.consoleHead, body)

                // 文件写入异步处理
                if ((config.isLog2FileSwitch || typeHigh == FILE) && typeLow >= config.mFileFilter) {
                    val timestamp = Date()
                    val fileContent = tagHead.fileHead + body
                    channel.trySend(FileLogEntry(typeLow, tagHead.tag, fileContent, timestamp))
                }
            } else if ((config.isLog2FileSwitch || typeHigh == FILE) && typeLow >= config.mFileFilter) {
                val timestamp = Date()
                getOrCreateLogScope().launch(Dispatchers.Default) {
                    val body = processBody(typeHigh, *contents)
                    val fileContent = tagHead.fileHead + body
                    channel.send(FileLogEntry(typeLow, tagHead.tag, fileContent, timestamp))
                }
            }
        }

        val currentLogFilePath: String
            get() = getCurrentLogFilePath(Date())

        val logFiles: List<File>
            get() {
                val dir = config.dir ?: return emptyList()
                val logDir = File(dir)
                if (!logDir.exists()) return emptyList()
                return logDir.listFiles { _, name -> isMatchLogFileName(name) }?.toList()
                    ?: emptyList()
            }

        // ==================== 辅助方法 ====================

        private fun processTagAndHead(tag: String): TagHead {
            var processedTag = tag
            if (!config.mTagIsSpace && !config.isLogHeadSwitch) {
                processedTag = config.globalTag
            } else {
                val stackTrace = Throwable().stackTrace
                val stackIndex = 3 + config.stackOffset

                if (stackIndex >= stackTrace.size) {
                    val targetElement = stackTrace[3]
                    val fileName = getFileName(targetElement)
                    if (config.mTagIsSpace && isSpace(processedTag)) {
                        val index = fileName.indexOf('.')
                        processedTag = if (index == -1) fileName else fileName.substring(0, index)
                    }
                    return TagHead(processedTag, null, ": ")
                }

                var targetElement = stackTrace[stackIndex]
                val fileName = getFileName(targetElement)
                if (config.mTagIsSpace && isSpace(processedTag)) {
                    val index = fileName.indexOf('.')
                    processedTag = if (index == -1) fileName else fileName.substring(0, index)
                }

                if (config.isLogHeadSwitch) {
                    val tName = Thread.currentThread().name
                    val head = String.format(
                        "%s, %s.%s(%s:%d)",
                        tName,
                        targetElement.className,
                        targetElement.methodName,
                        fileName,
                        targetElement.lineNumber
                    )
                    val fileHead = " [$head]: "

                    if (config.stackDeep <= 1) {
                        return TagHead(processedTag, arrayOf(head), fileHead)
                    } else {
                        val consoleHead = arrayOfNulls<String>(
                            min(config.stackDeep, stackTrace.size - stackIndex)
                        )
                        consoleHead[0] = head
                        val spaceLen = tName.length + 2
                        val space = " ".repeat(spaceLen)

                        for (i in 1 until consoleHead.size) {
                            targetElement = stackTrace[i + stackIndex]
                            consoleHead[i] = String.format(
                                "%s%s.%s(%s:%d)",
                                space,
                                targetElement.className,
                                targetElement.methodName,
                                getFileName(targetElement),
                                targetElement.lineNumber
                            )
                        }
                        return TagHead(processedTag, consoleHead, fileHead)
                    }
                }
            }
            return TagHead(processedTag, null, ": ")
        }

        private fun getFileName(targetElement: StackTraceElement): String {
            targetElement.fileName?.let { return it }

            var className = targetElement.className
            val classNameInfo = className.split(".").toTypedArray()
            if (classNameInfo.isNotEmpty()) {
                className = classNameInfo.last()
            }
            val index = className.indexOf('$')
            if (index != -1) {
                className = className.substring(0, index)
            }
            return "$className.java"
        }

        private fun processBody(type: Int, vararg contents: Any?): String {
            if (contents.isEmpty()) return NOTHING

            return if (contents.size == 1) {
                val result = formatObject(type, contents[0])
                if (result.isEmpty()) NOTHING else result
            } else {
                buildString {
                    contents.forEachIndexed { index, content ->
                        append(ARGS)
                            .append("[").append(index).append("]")
                            .append(" = ")
                            .append(formatObject(content))
                            .append(LINE_SEP)
                    }
                }.ifEmpty { NOTHING }
            }
        }

        private fun formatObject(type: Int, obj: Any?): String {
            if (obj == null) return NULL
            return when (type) {
                JSON -> LogFormatter.object2String(obj, JSON)
                XML -> LogFormatter.object2String(obj, XML)
                else -> formatObject(obj)
            }
        }

        private fun formatObject(any: Any?): String {
            if (any == null) return NULL
            if (!I_FORMATTER_MAP.isEmpty) {
                val iFormatter = I_FORMATTER_MAP[getClassFromObject(any)]
                if (iFormatter != null) {
                    return iFormatter.format(any)
                }
            }
            return LogFormatter.object2String(any)
        }

        private fun print2Console(
            type: Int,
            tag: String,
            head: Array<String?>?,
            msg: String
        ) {
            if (config.isSingleTagSwitch) {
                printSingleTagMsg(type, tag, processSingleTagMsg(head, msg))
            } else {
                printBorder(type, tag, true)
                printHead(type, tag, head)
                printMsg(type, tag, msg)
                printBorder(type, tag, false)
            }
        }

        private fun printBorder(type: Int, tag: String, isTop: Boolean) {
            if (config.isLogBorderSwitch) {
                print2Console(type, tag, if (isTop) TOP_BORDER else BOTTOM_BORDER)
            }
        }

        private fun printHead(type: Int, tag: String, head: Array<String?>?) {
            head?.forEach { aHead ->
                print2Console(
                    type, tag,
                    if (config.isLogBorderSwitch) LEFT_BORDER + aHead else aHead!!
                )
            }
            if (head != null && config.isLogBorderSwitch) {
                print2Console(type, tag, MIDDLE_BORDER)
            }
        }

        private fun printMsg(type: Int, tag: String, msg: String) {
            val len = msg.length
            val countOfSub = len / MAX_LEN
            if (countOfSub > 0) {
                var index = 0
                repeat(countOfSub) {
                    printSubMsg(type, tag, msg.substring(index, index + MAX_LEN))
                    index += MAX_LEN
                }
                if (index != len) {
                    printSubMsg(type, tag, msg.substring(index, len))
                }
            } else {
                printSubMsg(type, tag, msg)
            }
        }

        private fun printSubMsg(type: Int, tag: String, msg: String) {
            if (!config.isLogBorderSwitch) {
                print2Console(type, tag, msg)
                return
            }
            msg.split(LINE_SEP).forEach { line ->
                print2Console(type, tag, LEFT_BORDER + line)
            }
        }

        private fun processSingleTagMsg(head: Array<String?>?, msg: String): String {
            return buildString {
                if (config.isLogBorderSwitch) {
                    append(PLACEHOLDER).append(LINE_SEP)
                    append(TOP_BORDER).append(LINE_SEP)
                    head?.forEach { aHead ->
                        append(LEFT_BORDER).append(aHead).append(LINE_SEP)
                    }
                    if (head != null) {
                        append(MIDDLE_BORDER).append(LINE_SEP)
                    }
                    msg.split(LINE_SEP).forEach { line ->
                        append(LEFT_BORDER).append(line).append(LINE_SEP)
                    }
                    append(BOTTOM_BORDER)
                } else {
                    if (head != null) {
                        append(PLACEHOLDER).append(LINE_SEP)
                        head.forEach { aHead ->
                            append(aHead).append(LINE_SEP)
                        }
                    }
                    append(msg)
                }
            }
        }

        private fun printSingleTagMsg(type: Int, tag: String, msg: String) {
            val len = msg.length
            val countOfSub = if (config.isLogBorderSwitch) {
                (len - BOTTOM_BORDER.length) / MAX_LEN
            } else {
                len / MAX_LEN
            }

            if (countOfSub > 0) {
                if (config.isLogBorderSwitch) {
                    print2Console(type, tag, msg.substring(0, MAX_LEN) + LINE_SEP + BOTTOM_BORDER)
                    var index = MAX_LEN
                    for (i in 1 until countOfSub) {
                        print2Console(
                            type, tag,
                            "$PLACEHOLDER$LINE_SEP$TOP_BORDER$LINE_SEP$LEFT_BORDER${
                                msg.substring(index, index + MAX_LEN)
                            }$LINE_SEP$BOTTOM_BORDER"
                        )
                        index += MAX_LEN
                    }
                    if (index != len - BOTTOM_BORDER.length) {
                        print2Console(
                            type, tag,
                            "$PLACEHOLDER$LINE_SEP$TOP_BORDER$LINE_SEP$LEFT_BORDER${
                                msg.substring(index, len)
                            }"
                        )
                    }
                } else {
                    print2Console(type, tag, msg.substring(0, MAX_LEN))
                    var index = MAX_LEN
                    for (i in 1 until countOfSub) {
                        print2Console(
                            type, tag,
                            "$PLACEHOLDER$LINE_SEP${msg.substring(index, index + MAX_LEN)}"
                        )
                        index += MAX_LEN
                    }
                    if (index != len) {
                        print2Console(type, tag, "$PLACEHOLDER$LINE_SEP${msg.substring(index, len)}")
                    }
                }
            } else {
                print2Console(type, tag, msg)
            }
        }

        private fun print2Console(type: Int, tag: String, msg: String) {
            Log.println(type, tag, msg)
            config.mOnConsoleOutputListener?.onConsoleOutput(type, tag, msg)
        }

        private fun getCurrentLogFilePath(d: Date): String {
            val format = threadLocalSdf.get()!!.format(d)
            val date = format.substring(0, 10)
            return "${config.dir}${config.filePrefix}_${date}${config.fileExtension}"
        }

        private fun createOrExistsFile(filePath: String, date: String): Boolean {
            val file = File(filePath)
            if (file.exists()) return file.isFile

            if (!createOrExistsDir(file.parentFile)) return false

            return try {
                deleteDueLogs(filePath, date)
                val isCreate = file.createNewFile()
                if (isCreate) {
                    printDeviceInfo(filePath, date)
                }
                isCreate
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        private fun deleteDueLogs(filePath: String, date: String) {
            if (config.saveDays <= 0) return

            val file = File(filePath)
            val parentFile = file.parentFile ?: return
            val files = parentFile.listFiles { _, name -> isMatchLogFileName(name) }

            if (files.isNullOrEmpty()) return

            val sdf = SimpleDateFormat("yyyy_MM_dd", Locale.getDefault())
            try {
                val dueMillis = sdf.parse(date)!!.time - config.saveDays * 86400000L
                files.forEach { aFile ->
                    val logDay = findDate(aFile.name)
                    if (logDay.isNotEmpty() && sdf.parse(logDay)!!.time <= dueMillis) {
                        getOrCreateLogScope().launch {
                            if (!aFile.delete()) {
                                Log.e("LogUtils", "delete $aFile failed!")
                            }
                        }
                    }
                }
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }

        private fun isMatchLogFileName(name: String): Boolean {
            return name.matches("^${config.filePrefix}_[0-9]{4}_[0-9]{2}_[0-9]{2}.*$".toRegex())
        }

        private fun findDate(str: String): String {
            val pattern = Pattern.compile("[0-9]{4}_[0-9]{2}_[0-9]{2}")
            val matcher = pattern.matcher(str)
            return if (matcher.find()) matcher.group() else ""
        }

        private fun printDeviceInfo(filePath: String, date: String) {
            config.mFileHead.addFirst("Date of Log", date)
            input2File(filePath, config.mFileHead.toString())
        }

        private fun input2File(filePath: String, input: String) {
            if (config.mFileWriter == null) {
                writeFileFromString(File(filePath), input, true)
            } else {
                config.mFileWriter!!.write(filePath, input)
            }
            config.mOnFileOutputListener?.onFileOutput(filePath, input)
        }

        private fun <T> getTypeClassFromParadigm(formatter: IFormatter<T>): Class<*>? {
            val genericInterfaces = formatter.javaClass.genericInterfaces
            var type: Type? = if (genericInterfaces.size == 1) {
                genericInterfaces[0]
            } else {
                formatter.javaClass.genericSuperclass
            }

            type = (type as ParameterizedType).actualTypeArguments[0]
            while (type is ParameterizedType) {
                type = type.rawType
            }

            var className = type.toString()
            className = when {
                className.startsWith("class ") -> className.substring(6)
                className.startsWith("interface ") -> className.substring(10)
                else -> className
            }

            return try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }
        }

        private fun getClassFromObject(obj: Any): Class<*> {
            val objClass = obj.javaClass
            if (objClass.isAnonymousClass || objClass.isSynthetic) {
                val genericInterfaces = objClass.genericInterfaces
                var className: String

                var type: Type? = if (genericInterfaces.size == 1) {
                    genericInterfaces[0]
                } else {
                    objClass.genericSuperclass
                }

                while (type is ParameterizedType) {
                    type = type.rawType
                }

                className = type.toString()
                className = when {
                    className.startsWith("class ") -> className.substring(6)
                    className.startsWith("interface ") -> className.substring(10)
                    else -> className
                }

                return try {
                    Class.forName(className)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                    objClass
                }
            }
            return objClass
        }

        private fun isSpace(s: String?): Boolean {
            return s.isNullOrBlank()
        }

        private fun getFullStackTrace(throwable: Throwable?): String {
            var current = throwable
            val throwableList = mutableListOf<Throwable>()
            while (current != null && current !in throwableList) {
                throwableList.add(current)
                current = current.cause
            }

            val size = throwableList.size
            val frames = mutableListOf<String>()
            var nextTrace = getStackFrameList(throwableList[size - 1])

            for (i in size downTo 1) {
                val trace = nextTrace
                if (i != 1) {
                    nextTrace = getStackFrameList(throwableList[i - 2])
                    removeCommonFrames(trace, nextTrace)
                }
                frames.add(
                    if (i == size) throwableList[i - 1].toString()
                    else " Caused by: ${throwableList[i - 1]}"
                )
                frames.addAll(trace)
            }

            return frames.joinToString(LINE_SEP)
        }

        private fun getStackFrameList(throwable: Throwable): MutableList<String> {
            val sw = StringWriter()
            val pw = PrintWriter(sw, true)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val list = mutableListOf<String>()
            var traceStarted = false

            stackTrace.split(LINE_SEP).forEach { token ->
                val at = token.indexOf("at")
                if (at != -1 && token.substring(0, at).isBlank()) {
                    traceStarted = true
                    list.add(token)
                } else if (traceStarted) {
                    return list
                }
            }
            return list
        }

        private fun removeCommonFrames(
            causeFrames: MutableList<String>,
            wrapperFrames: List<String>
        ) {
            var causeFrameIndex = causeFrames.size - 1
            var wrapperFrameIndex = wrapperFrames.size - 1

            while (causeFrameIndex >= 0 && wrapperFrameIndex >= 0) {
                if (causeFrames[causeFrameIndex] == wrapperFrames[wrapperFrameIndex]) {
                    causeFrames.removeAt(causeFrameIndex)
                }
                causeFrameIndex--
                wrapperFrameIndex--
            }
        }

        private fun createOrExistsDir(file: File?): Boolean {
            return file != null && (if (file.exists()) file.isDirectory else file.mkdirs())
        }

        private val isSDCardEnableByEnvironment: Boolean
            get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()

        private fun writeFileFromString(file: File?, content: String?, append: Boolean): Boolean {
            if (file == null || content == null) return false

            if (!createOrExistsFile(file)) {
                Log.e("FileIOUtils", "create file <$file> failed.")
                return false
            }

            return try {
                BufferedWriter(FileWriter(file, append)).use { bw ->
                    bw.write(content)
                }
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        private fun createOrExistsFile(file: File?): Boolean {
            if (file == null) return false
            if (file.exists()) return file.isFile
            if (!createOrExistsDir(file.parentFile)) return false

            return try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }
    }
}
