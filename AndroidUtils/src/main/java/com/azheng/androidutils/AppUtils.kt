package com.azheng.androidutils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import androidx.annotation.Nullable
import java.io.File
import java.security.MessageDigest
import java.util.*

/**
 * App 相关工具类
 */
object AppUtils {


    /**
     * 获取 App 图标
     *
     * @param packageName 包名
     * @return App 图标
     */
    fun getAppIcon(
        packageName: String = Utils.getApplication().packageName
    ): Drawable? {
        return try {
            Utils.getApplication().packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取 App 包名
     * @return App 包名
     */
    fun getAppPackageName(): String {
        return Utils.getApplication().packageName
    }

    /**
     * 获取 App 名称
     *
     * @param packageName 包名
     * @return App 名称
     */
    fun getAppName(packageName: String = Utils.getApplication().packageName): String {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)

            packageInfo.applicationInfo?.loadLabel(packageManager)?.toString() ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取 App 路径
     *
     * @param packageName 包名
     * @return App 路径
     */
    fun getAppPath(packageName: String = getAppPackageName()): String {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.applicationInfo?.sourceDir ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取 App 版本号
     *
     * @param packageName 包名
     * @return App 版本号
     */
    fun getAppVersionName(): String {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = packageManager.getPackageInfo(getAppPackageName(), 0)
            packageInfo.versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取 App 版本码
     *
     * @param packageName 包名
     * @return App 版本码
     */
    fun getAppVersionCode(): Long {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = packageManager.getPackageInfo(getAppPackageName(), 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * 获取 App 支持最低系统版本号
     *
     * @param packageName 包名
     * @return App 支持最低系统版本号
     */
    fun getAppMinSdkVersion(packageName: String = getAppPackageName()): Int {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                packageInfo.applicationInfo?.minSdkVersion ?: -1
            } else {
                -1 // API 24 以下无法直接获取
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * 获取 App 目标系统版本号
     *
     * @param packageName 包名
     * @return App 目标系统版本号
     */
    fun getAppTargetSdkVersion(packageName: String = getAppPackageName()): Int {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.applicationInfo?.targetSdkVersion ?: -1
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            -1
        }
    }

    /**
     * 获取应用签名的 SHA1 值
     *
     * @param packageName 包名
     * @return 应用签名的 SHA1 值
     */
    fun getAppSignaturesSHA1(packageName: String = getAppPackageName()): String {
        return getSignatureHash(packageName, "SHA1")
    }

    /**
     * 获取应用签名的 SHA256 值
     *
     * @param packageName 包名
     * @return 应用签名的 SHA256 值
     */
    fun getAppSignaturesSHA256(packageName: String = getAppPackageName()): String {
        return getSignatureHash(packageName, "SHA-256")
    }

    /**
     * 获取应用签名的 MD5 值
     *
     * @param packageName 包名
     * @return 应用签名的 MD5 值
     */
    fun getAppSignaturesMD5(packageName: String = getAppPackageName()): String {
        return getSignatureHash(packageName, "MD5")
    }

    /**
     * 获取签名哈希值
     *
     *
     * @param packageName 包名
     * @param algorithm   算法
     * @return 签名哈希值
     */
    private fun getSignatureHash(
        packageName: String = getAppPackageName(),
        algorithm: String
    ): String {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures?.isEmpty() == true) return ""

            val signature = signatures?.get(0) ?: return ""
            val md = MessageDigest.getInstance(algorithm)
            val bytes = md.digest(signature.toByteArray())
            val sb = StringBuilder()
            for (i in bytes.indices) {
                sb.append(String.format("%02X:", bytes[i]))
            }
            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 打开 App
     *
     *
     * @param packageName 包名
     * @return 是否成功
     */
    fun launchApp(packageName: String = getAppPackageName()): Boolean {
        return try {
            val packageManager = Utils.getApplication().packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                Utils.getApplication().startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 重启 App
     *
     */
    @SuppressLint("MissingPermission")
    fun relaunchApp() {
        val intent =
            Utils.getApplication().packageManager.getLaunchIntentForPackage(getAppPackageName())
                ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        Utils.getApplication().startActivity(intent)
        // 杀掉当前进程
        val am =
            Utils.getApplication().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.appTasks.forEach { it.finishAndRemoveTask() }
        Process.killProcess(Process.myPid())
    }
}
