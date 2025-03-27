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

        /* 单位转换 */
        fun dp2px( dp: Float): Int {
            val metrics = Utils.getApplication().resources.displayMetrics
            return (dp * metrics.density + 0.5f).toInt()
        }

        fun px2dp( px: Float): Int {
            val metrics = Utils.getApplication().resources.displayMetrics
            return (px / metrics.density + 0.5f).toInt()
        }

        // 使用系统推荐方法
        fun sp2px(sp: Float): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                Utils.getApplication().resources.displayMetrics
            ).toInt()
        }

        fun px2sp(px: Float): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                TypedValue.deriveDimension(
                    TypedValue.COMPLEX_UNIT_PX,
                    px,
                    Utils.getApplication().resources.displayMetrics
                ).toInt()
            } else {
                // 旧版本兼容方案（非线性缩放可能存在误差）
                val metrics = Utils.getApplication().resources.displayMetrics
                (px / metrics.scaledDensity + 0.5f).toInt()
            }
        }

        /* drawable ↔ Bitmap 转换 */
        fun drawable2Bitmap(drawable: Drawable, config: Bitmap.Config? = null): Bitmap {
            return if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                // 处理无效尺寸（关键改进）
                val width = drawable.intrinsicWidth.coerceAtLeast(1)
                val height = drawable.intrinsicHeight.coerceAtLeast(1)
                var mconfig = config
                if (mconfig == null) {
                    mconfig = if (drawable.opacity == PixelFormat.OPAQUE) {
                        Bitmap.Config.RGB_565  // 无透明通道时节省内存
                    } else {
                        Bitmap.Config.ARGB_8888
                    }
                }
                // 保持高质量色彩（优于原Java方案）
                val bitmap = createBitmap(width, height, mconfig)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
                bitmap
            }
        }


        fun bitmap2Drawable( bitmap: Bitmap): Drawable {
            return bitmap.toDrawable(Utils.getApplication().resources)
        }

        /* drawable ↔ Bytes 转换 */
        fun drawable2Bytes(
            drawable: Drawable,
            format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
        ): ByteArray {
            val bitmap = drawable2Bitmap(drawable)
            return bitmap2Bytes(bitmap, format)
        }

        fun bytes2Drawable( bytes: ByteArray): Drawable {
            return bitmap2Drawable(bytes2Bitmap(bytes))
        }

        /* View → Bitmap 转换 */
        fun view2Bitmap(view: View): Bitmap {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)

            val bitmap = createBitmap(view.width, view.height)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            return bitmap
        }

        /* 内部工具方法 */
        private fun bitmap2Bytes(bitmap: Bitmap, format: Bitmap.CompressFormat): ByteArray {
            val stream = ByteArrayOutputStream()
            bitmap.compress(format, 100, stream)
            return stream.toByteArray()
        }

        private fun bytes2Bitmap(bytes: ByteArray): Bitmap {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
