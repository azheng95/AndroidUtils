package com.azheng.utils.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.azheng.androidutils.ActivityUtils
import com.azheng.androidutils.GsonUtils
import com.azheng.androidutils.LogUtils
import com.azheng.androidutils.onSingleClick
import com.azheng.utils.demo.databinding.ActivityMainBinding
import dev.androidbroadcast.vbpd.viewBinding


class MainActivity : AppCompatActivity() {

    private val viewBinding: ActivityMainBinding by viewBinding(ActivityMainBinding::bind)
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewBinding.tvTestView.onSingleClick {
          val json =  GsonUtils.fromJson("123", Int::class.java)
            LogUtils.d(json)
        }

    }

    /**
     * 重写Back事件，双击退出
     */
    private var pressTime: Long = 0     //初始时间，用来判断双击返回

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val time = System.currentTimeMillis()
        if (time - pressTime > 2000) {
            pressTime = time
        } else {
            super.onBackPressed()
            ActivityUtils.finishAllActivity()
        }
    }
}
