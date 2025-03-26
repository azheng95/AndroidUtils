package com.azheng.androidutils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * 时间工具类
 */
object TimeUtils {
    private const val defaultFormat = "yyyy-MM-dd HH:mm:ss"
    private val chineseWeek = arrayOf("日", "一", "二", "三", "四", "五", "六")
    private val usWeek =
        arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val zodiacArray = arrayOf(
        "水瓶座",
        "双鱼座",
        "白羊座",
        "金牛座",
        "双子座",
        "巨蟹座",
        "狮子座",
        "处女座",
        "天秤座",
        "天蝎座",
        "射手座",
        "摩羯座"
    )
    private val zodiacDays = intArrayOf(20, 19, 21, 20, 21, 22, 23, 23, 23, 24, 23, 22)

    private val chineseZodiac =
        arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")

    // 优化点1：使用双重校验锁实现线程安全缓存
    @Volatile
    private var formatCache: MutableMap<String, ThreadLocal<SimpleDateFormat>>? = null

    // 优化点2：复用Calendar对象减少内存分配
    private val calendar: Calendar by lazy { Calendar.getInstance() }

    // 获取安全的日期格式（带缓存机制）
    private fun getSafeDateFormat(pattern: String): SimpleDateFormat {
        if (formatCache == null) {
            synchronized(this) {
                if (formatCache == null) {
                    formatCache = HashMap()
                }
            }
        }
        return formatCache!!.getOrPut(pattern) {
            ThreadLocal.withInitial {
                SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }
            }
        }.get()
    }

    // region 核心转换方法
    /**
     * 时间戳转字符串
     * @param millis 毫秒时间戳
     * @param pattern 格式模板（默认：yyyy-MM-dd HH:mm:ss）
     */
    fun millis2String(millis: Long, pattern: String = defaultFormat): String {
        return getSafeDateFormat(pattern).format(Date(millis))
    }

    /**
     * 字符串转时间戳（带空安全处理）
     * @param timeString 时间字符串
     * @param pattern 格式模板
     * @return 解析失败返回0
     */
    fun string2Millis(timeString: String, pattern: String = defaultFormat): Long {
        return try {
            getSafeDateFormat(pattern).parse(timeString)?.time ?: 0
        } catch (e: Exception) {
            0
        }
    }

    // 字符串转Date
    fun string2Date(timeString: String, pattern: String = defaultFormat): Date? {
        return getSafeDateFormat(pattern).parse(timeString)
    }

    // Date转字符串
    fun date2String(date: Date, pattern: String = defaultFormat): String {
        return getSafeDateFormat(pattern).format(date)
    }


    // 优化点4：时间差计算增加时间单位自动转换
    fun getTimeSpan(startMillis: Long, endMillis: Long, unit: TimeUnit): Long {
        val duration = endMillis - startMillis
        return when (unit) {
            TimeUnit.MILLISECONDS -> duration
            else -> unit.convert(duration, TimeUnit.MILLISECONDS)
        }
    }

    // 获取合适型时间差（自动选择单位） 增强型时间差显示（支持周、月、年）
    fun getFitTimeSpanYear(startMillis: Long, endMillis: Long): String {
        val span = abs(endMillis - startMillis)
        return when {
            span < 60_000L -> "${span / 1000}秒"
            span < 3_600_000L -> "${span / 60_000}分钟"
            span < 86_400_000L -> "${span / 3_600_000}小时"
            span < 604_800_000L -> "${span / 86_400_000}天"
            span < 2_592_000_000L -> "${span / 604_800_000}周"  // 约30天
            span < 31_536_000_000L -> "${span / 2_592_000_000}月"  // 约365天
            else -> "${span / 31_536_000_000}年"
        }
    }

    // 获取合适型时间差（自动选择单位）
    fun getFitTimeSpan(startMillis: Long, endMillis: Long): String {
        val span = abs(endMillis - startMillis)
        return when {
            span < TimeUnit.MINUTES.toMillis(1) -> "${TimeUnit.MILLISECONDS.toSeconds(span)}秒"
            span < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(span)}分钟"
            span < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(span)}小时"
            else -> "${TimeUnit.MILLISECONDS.toDays(span)}天"
        }
    }

    // 当前Date对象
    fun getNowDate(): Date = Date(getNowMills())

    // 获取与当前时间的差
    fun getTimeSpanByNow(millis: Long, unit: TimeUnit): Long =
        getTimeSpan(millis, getNowMills(), unit)

    // 当前时间获取
    fun getNowMills(): Long {
        return System.currentTimeMillis()
    }

    // 当前时间字符串
    fun getNowString(pattern: String = defaultFormat): String =
        Date(getNowMills()).toStringFormat(pattern)

    // 判断是否是今天
    fun isToday(millis: Long): Boolean {
        synchronized(calendar) {
            calendar.timeInMillis = getNowMills()
            val todayYear = calendar[Calendar.YEAR]
            val todayDay = calendar[Calendar.DAY_OF_YEAR]

            calendar.timeInMillis = millis
            return todayYear == calendar[Calendar.YEAR] &&
                    todayDay == calendar[Calendar.DAY_OF_YEAR]
        }
    }


    // region 扩展函数优化
    /** Date扩展：转字符串 */
    fun Date.toStringFormat(pattern: String = defaultFormat): String =
        getSafeDateFormat(pattern).format(this)

    /** String扩展：转Date */
    fun String.toDate(pattern: String = defaultFormat): Date? =
        getSafeDateFormat(pattern).parse(this)

    // 原始方法（未优化）
    fun date2Millis(date: Date): Long = date.time

    // 优化方法（新增扩展函数）
    fun Date.toMillis(): Long = this.time

    // 原始方法（未优化）
    fun millis2Date(millis: Long): Date = Date(millis)

    // 优化方法（新增扩展函数）
    fun Long.toDate(): Date = Date(this)


    // 判断闰年
    fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0


    // 中式星期
    fun getChineseWeek(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return "星期${chineseWeek[cal.get(Calendar.DAY_OF_WEEK) - 1]}"
    }

    // 美式星期
    fun getUSWeek(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return usWeek[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }

    // 判断上午
    fun isAm(millis: Long): Boolean {
        return Calendar.getInstance().apply { timeInMillis = millis }
            .get(Calendar.AM_PM) == Calendar.AM
    }

    // 判断下午
    fun isPm(millis: Long): Boolean = !isAm(millis)

    // 获取生肖
    fun getChineseZodiac(millis: Long): String {
        val year = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)
        return chineseZodiac[(year - 4) % 12]
    }

    // 星座算法（精确到分钟）
    fun getZodiac(millis: Long): String {
        synchronized(calendar) {
            calendar.timeInMillis = millis
            val month = calendar[Calendar.MONTH]
            val day = calendar[Calendar.DAY_OF_MONTH]
            val hour = calendar[Calendar.HOUR_OF_DAY]
            val minute = calendar[Calendar.MINUTE]

            // 精确到分钟的时间点计算
            val preciseTime = day * 1440 + hour * 60 + minute
            val threshold = zodiacDays[month] * 1440

            return if (preciseTime >= threshold) {
                zodiacArray[month]
            } else {
                // 特殊处理 1 月的前半段（摩羯座）
                if (month == 0) zodiacArray[11] else zodiacArray[month - 1]
            }
        }
    }


}
