package com.azheng.androidutils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.NetworkInterface
import java.util.*

/**
 * 设备工具类
 */
object DeviceUtils {

    private const val TAG = "DeviceUtils"

    /**
     * 判断设备是否 rooted
     *
     * @return `true`: 是<br></br>`false`: 否
     */
    val isDeviceRooted: Boolean
        get() {
            val su = "su"
            val locations = arrayOf(
                "/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/",
                "/system/bin/failsafe/", "/data/local/xbin/", "/data/local/bin/", "/data/local/",
                "/system/sbin/", "/usr/bin/", "/vendor/bin/"
            )
            for (location in locations) {
                if (File(location + su).exists()) {
                    return true
                }
            }
            return false
        }

    /**
     * 判断设备 ADB 是否可用
     *
     * @return `true`: 是<br></br>`false`: 否
     */
    fun isAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                Utils.getApplication().contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) > 0
        } catch (e: Exception) {
            Log.e(TAG, "isAdbEnabled: ", e)
            false
        }
    }

    /**
     * 获取设备系统版本号
     *
     * @return 设备系统版本号
     */
    val sDKVersionName: String
        get() = Build.VERSION.RELEASE

    /**
     * 获取设备系统版本码
     *
     * @return 设备系统版本码
     */
    val sDKVersionCode: Int
        get() = Build.VERSION.SDK_INT

    /**
     * 获取设备 AndroidID
     *
     * @return AndroidID
     */
    @SuppressLint("HardwareIds")
    fun getAndroidID(): String {
        return try {
            val id = Settings.Secure.getString(
                Utils.getApplication().contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: ""
            // 排除 Android 2.2 系统 bug 生成的通用ID
            if ("9774d56d682e549c".equals(id)) return ""
            id
        } catch (e: Exception) {
            Log.e(TAG, "getAndroidID: ", e)
            ""
        }
    }

    /**
     * 获取设备 MAC 地址
     *
     * @return MAC 地址
     */
    @SuppressLint("HardwareIds", "MissingPermission")
    fun getMacAddress(): String {
        try {
            // Android 6.0 及以上版本不再通过 WifiManager 获取 MAC 地址
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkInterfaces = NetworkInterface.getNetworkInterfaces() ?: return ""
                for (networkInterface in networkInterfaces) {
                    if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                        val macBytes = networkInterface.hardwareAddress ?: return ""
                        val sb = StringBuilder()
                        for (b in macBytes) {
                            sb.append(String.format("%02X:", b))
                        }
                        if (sb.isNotEmpty()) {
                            sb.deleteCharAt(sb.length - 1)
                        }
                        val address = sb.toString()
                        // 排除随机生成的 MAC 地址
                        if ("02:00:00:00:00:00".equals(address, ignoreCase = true)) return ""
                        return address
                    }
                }
            } else {
                val wifiManager = Utils.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                if (wifiInfo != null) {
                    val macAddress = wifiInfo.macAddress
                    // 排除随机生成的 MAC 地址
                    if (!TextUtils.isEmpty(macAddress) && !"02:00:00:00:00:00".equals(
                            macAddress,
                            ignoreCase = true
                        )
                    ) {
                        return macAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMacAddress: ", e)
        }
        return ""
    }


    /**
     * 获取设备厂商
     *
     * @return 设备厂商
     */
    val manufacturer: String
        get() = Build.MANUFACTURER

    /**
     * 获取设备型号
     *
     * @return 设备型号
     */
    val model: String
        get() = Build.MODEL

    /**
     * 获取设备 ABIs
     *
     * @return 设备 ABIs
     */
    val aBIs: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS
        } else {
            if (!TextUtils.isEmpty(Build.CPU_ABI2)) {
                arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
            } else {
                arrayOf(Build.CPU_ABI)
            }
        }

    /**
     * 判断是否是平板
     *
     * @return `true`: 是<br></br>`false`: 否
     */
    fun isTablet(context: Context): Boolean {
        return try {
            (context.resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >=
                    android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
        } catch (e: Exception) {
            Log.e(TAG, "isTablet: ", e)
            false
        }
    }

    /**
     * 判断是否是模拟器
     *
     * @return `true`: 是<br></br>`false`: 否
     */
    val isEmulator: Boolean
        get() {
            val checkProperty = (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.lowercase(Locale.getDefault()).contains("vbox")
                    || Build.FINGERPRINT.lowercase(Locale.getDefault()).contains("test-keys")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                    || "google_sdk" == Build.PRODUCT)
            if (checkProperty) return true

            var operatorName = ""
            try {
                val tm = Utils.getApplication()
                    .getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
                operatorName = tm.networkOperatorName
            } catch (e: Exception) {
                Log.e(TAG, "isEmulator: ", e)
            }
            return operatorName.lowercase(Locale.getDefault()) == "android"
        }

    /**
     * 判断开发者选项是否打开
     *
     * @return `true`: 是<br></br>`false`: 否
     */
    fun isDevelopmentSettingsEnabled(context: Context): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) > 0
        } catch (e: Exception) {
            try {
                // 尝试旧版本的设置
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.DEVELOPMENT_SETTINGS_ENABLED, 0
                ) > 0
            } catch (e1: Exception) {
                Log.e(TAG, "isDevelopmentSettingsEnabled: ", e1)
                false
            }
        }
    }

    /**
     * 获取唯一设备 ID
     *
     * @return 唯一设备 ID
     */
    @SuppressLint("HardwareIds")
    fun getUniqueDeviceId(): String {
        val androidId = Settings.Secure.getString(
            Utils.getApplication().contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: ""

        // 只使用不会随系统更新改变的硬件信息
        val deviceInfo = buildString {
            append(androidId)
            append(Build.BOARD)
            append(Build.BRAND)
            append(Build.DEVICE)
            append(Build.HARDWARE)
            append(Build.MANUFACTURER)
            append(Build.MODEL)
            append(Build.PRODUCT)
        }
        // 使用 UUID v5 (SHA-1) 或直接用 UUID v3
        return UUID.nameUUIDFromBytes(deviceInfo.toByteArray()).toString()
    }

}

