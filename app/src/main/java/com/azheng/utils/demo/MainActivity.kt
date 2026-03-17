package com.azheng.utils.demo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.azheng.androidutils.ActivityUtils
import com.azheng.androidutils.onSingleClick
import com.azheng.utils.demo.databinding.ActivityMainBinding
import com.azheng.utils.demo.GsonActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnGson.onSingleClick {
            // 跳转到 GsonUtils 使用示例页面
            startActivity(Intent(this, GsonActivity::class.java))
        }
    }

    /**
     * 重写Back事件，双击退出
     */
    private var pressTime: Long = 0     // 初始时间，用来判断双击返回

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
