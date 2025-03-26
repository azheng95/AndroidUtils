package com.azheng.utils.demo

import android.app.Application
import com.azheng.androidutils.Utils

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化Utils
        Utils.init(this)
    }
}
