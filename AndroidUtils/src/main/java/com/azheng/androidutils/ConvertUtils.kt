package com.azheng.androidutils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import java.io.ByteArrayOutputStream

object ConvertUtils {

    /* ==================== 单位转换 ==================== */

    fun dp2px(dp: Float): Float {
        val metrics = Utils.getApplication().resources.displayMetrics
        return (dp * metrics.density + 0.5f)
    }

    fun px2dp(px: Float): Float {
        val metrics = Utils.getApplication().resources.displayMetrics
        return (px / metrics.density + 0.5f)
    }

    fun sp2px(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, sp, Utils.getApplication().resources.displayMetrics
        )
    }

    fun px2sp(px: Float): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            TypedValue.deriveDimension(
                TypedValue.COMPLEX_UNIT_PX,
                px,
                Utils.getApplication().resources.displayMetrics
            ).toInt()
        } else {
            val metrics = Utils.getApplication().resources.displayMetrics
            (px / metrics.scaledDensity + 0.5f).toInt()
        }
    }

    /* ==================== Drawable ↔ Bitmap ==================== */

    fun drawable2Bitmap(drawable: Drawable, config: Bitmap.Config? = null): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val bitmapConfig = config ?: if (drawable.opacity == PixelFormat.OPAQUE) {
            Bitmap.Config.RGB_565
        } else {
            Bitmap.Config.ARGB_8888
        }
        return createBitmap(width, height, bitmapConfig).also { bitmap ->
            Canvas(bitmap).apply {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(this)
            }
        }
    }

    fun bitmap2Drawable(bitmap: Bitmap): Drawable {
        return bitmap.toDrawable(Utils.getApplication().resources)
    }

    /* ==================== Drawable ↔ Bytes ==================== */

    fun drawable2Bytes(
        drawable: Drawable,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): ByteArray {
        return bitmap2Bytes(drawable2Bitmap(drawable), format)
    }

    fun bytes2Drawable(bytes: ByteArray): Drawable {
        return bitmap2Drawable(bytes2Bitmap(bytes))
    }

    /* ==================== View → Bitmap ==================== */

    fun view2Bitmap(view: View): Bitmap {
        if (view.measuredWidth <= 0 || view.measuredHeight <= 0) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(
                view.parent?.let { (it as View).width }
                    ?: Utils.getApplication().resources.displayMetrics.widthPixels,
                View.MeasureSpec.AT_MOST
            )
            val heightSpec = View.MeasureSpec.makeMeasureSpec(
                view.parent?.let { (it as View).height }
                    ?: Utils.getApplication().resources.displayMetrics.heightPixels,
                View.MeasureSpec.AT_MOST
            )
            view.measure(widthSpec, heightSpec)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        }
        return createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888).also {
            Canvas(it).apply { view.draw(this) }
        }
    }

    /* ==================== Bitmap ↔ Bytes ==================== */

    fun bitmap2Bytes(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): ByteArray {
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(format, quality, stream)
            stream.toByteArray()
        }
    }

    fun bytes2Bitmap(bytes: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

/* ==================== 扩展函数 - 单位转换 ==================== */

// Float 扩展
fun Float.dp2px(): Float = ConvertUtils.dp2px(this)
fun Float.px2dp(): Float = ConvertUtils.px2dp(this)
fun Float.sp2px(): Float = ConvertUtils.sp2px(this)
fun Float.px2sp(): Int = ConvertUtils.px2sp(this)

// Int 扩展
fun Int.dp2px(): Float = ConvertUtils.dp2px(this.toFloat())
fun Int.px2dp(): Float = ConvertUtils.px2dp(this.toFloat())
fun Int.sp2px(): Float = ConvertUtils.sp2px(this.toFloat())
fun Int.px2sp(): Int = ConvertUtils.px2sp(this.toFloat())

// Double 扩展
fun Double.dp2px(): Float = ConvertUtils.dp2px(this.toFloat())
fun Double.px2dp(): Float = ConvertUtils.px2dp(this.toFloat())
fun Double.sp2px(): Float = ConvertUtils.sp2px(this.toFloat())
fun Double.px2sp(): Int = ConvertUtils.px2sp(this.toFloat())

// 返回 Int 类型的便捷方法
fun Float.dp2pxInt(): Int = this.dp2px().toInt()
fun Float.px2dpInt(): Int = this.px2dp().toInt()
fun Float.sp2pxInt(): Int = this.sp2px().toInt()
fun Int.dp2pxInt(): Int = this.dp2px().toInt()
fun Int.px2dpInt(): Int = this.px2dp().toInt()
fun Int.sp2pxInt(): Int = this.sp2px().toInt()

/* ==================== 扩展函数 - 图像转换 ==================== */

// Drawable 扩展
fun Drawable.toBitmap(config: Bitmap.Config? = null): Bitmap =
    ConvertUtils.drawable2Bitmap(this, config)

fun Drawable.toBytes(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray =
    ConvertUtils.drawable2Bytes(this, format)

// Bitmap 扩展
fun Bitmap.toDrawable(): Drawable =
    ConvertUtils.bitmap2Drawable(this)

fun Bitmap.toBytes(
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
    quality: Int = 100
): ByteArray = ConvertUtils.bitmap2Bytes(this, format, quality)

// ByteArray 扩展
fun ByteArray.toBitmap(): Bitmap =
    ConvertUtils.bytes2Bitmap(this)

fun ByteArray.toDrawable(): Drawable =
    ConvertUtils.bytes2Drawable(this)

// View 扩展
fun View.toBitmap(): Bitmap =
    ConvertUtils.view2Bitmap(this)
