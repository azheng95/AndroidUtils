package com.azheng.androidutils

import android.view.View

/**
 * 点击工具类 - 处理防抖动点击
 */
object ClickUtils {
    
    /**
     * 为单个 View 应用防抖动点击
     * @param view 需要处理的视图
     * @param duration 防抖时间间隔(毫秒)
     * @param listener 点击监听器
     */
    @JvmStatic
    fun applySingleDebouncing(view: View, duration: Long = 500, listener: (View) -> Unit) {
        applySingleDebouncing(arrayOf(view), duration, listener)
    }
    
    /**
     * 为多个 View 应用防抖动点击
     * @param views 需要处理的视图数组
     * @param duration 防抖时间间隔(毫秒)
     * @param listener 点击监听器
     */
    @JvmStatic
    fun applySingleDebouncing(views: Array<View>, duration: Long = 500, listener: (View) -> Unit) {
        applyDebouncing(views, false, duration, listener)
    }
    
    /**
     * 为多个 View 应用全局防抖动点击
     * @param views 需要处理的视图数组
     * @param duration 防抖时间间隔(毫秒)
     * @param listener 点击监听器
     */
    @JvmStatic
    fun applyGlobalDebouncing(views: Array<View>, duration: Long = 500, listener: (View) -> Unit) {
        applyDebouncing(views, true, duration, listener)
    }
    
    /**
     * 应用防抖动点击
     * @param views 需要处理的视图数组
     * @param isGlobal 是否全局防抖
     * @param duration 防抖时间间隔(毫秒)
     * @param listener 点击监听器
     */
    private fun applyDebouncing(
        views: Array<View>?, 
        isGlobal: Boolean, 
        duration: Long, 
        listener: (View) -> Unit
    ) {
        if (views.isNullOrEmpty()) return
        
        views.forEach { view ->
            view?.setOnClickListener(object : OnDebouncingClickListener(isGlobal, duration) {
                override fun onDebouncingClick(v: View) {
                    listener.invoke(v)
                }
            })
        }
    }
    
    /**
     * 防抖动点击监听器抽象类
     */
    private abstract class OnDebouncingClickListener(
        private val isGlobal: Boolean = false,
        private val duration: Long = 500
    ) : View.OnClickListener {
        
        companion object {
            // 上次点击时间
            private var sLastClickTime: Long = 0
            // 上次点击的视图ID
            private var sLastClickViewId = 0
        }
        
        /**
         * 防抖动点击事件回调
         */
        abstract fun onDebouncingClick(v: View)
        
        override fun onClick(v: View) {
            val viewId = v.id
            val currentTime = System.currentTimeMillis()
            
            if (isGlobal) {
                // 全局防抖
                if (currentTime - sLastClickTime > duration) {
                    sLastClickTime = currentTime
                    onDebouncingClick(v)
                }
            } else {
                // 单视图防抖
                if (viewId != sLastClickViewId) {
                    // 不同视图，直接触发
                    sLastClickViewId = viewId
                    sLastClickTime = currentTime
                    onDebouncingClick(v)
                } else if (currentTime - sLastClickTime > duration) {
                    // 同一视图但超过防抖时间
                    sLastClickTime = currentTime
                    onDebouncingClick(v)
                }
            }
        }
    }
}

/**
 * 解决连续单击问题的扩展函数
 * @param action 点击事件处理函数
 * @param duration 防抖时间间隔(毫秒)
 * 只对单个视图进行防抖。如果用户点击了视图A，然后立即点击视图B，两个点击事件都会被触发。
 */
fun View.onSingleClick(duration: Long = 500, action: (view: View) -> Unit) {
    ClickUtils.applySingleDebouncing(this, duration, action)
}

/**
 * 全局防抖点击的扩展函数
 * @param action 点击事件处理函数
 * @param duration 防抖时间间隔(毫秒)
 * 对所有使用此函数的视图进行全局防抖。如果用户点击了视图A，然后在防抖时间内点击视图B，视图B的点击事件不会被触发。
 */
fun View.onGlobalClick(duration: Long = 500, action: (view: View) -> Unit) {
    ClickUtils.applyGlobalDebouncing(arrayOf(this), duration, action)
}
