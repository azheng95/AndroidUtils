package com.azheng.androidutils

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.BlurMaskFilter.Blur
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LineHeightSpan
import android.text.style.MaskFilterSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.text.style.UpdateAppearance
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.core.content.ContextCompat
import java.io.Serializable
import java.lang.ref.WeakReference
import androidx.core.graphics.drawable.toDrawable

class SpansUtils() {
    @IntDef(*[ALIGN_BOTTOM, ALIGN_BASELINE, ALIGN_CENTER, ALIGN_TOP])
    @Retention(
        AnnotationRetention.SOURCE
    )
    annotation class Align

    private var mTextView: TextView? = null
    private var mText: CharSequence?
    private var flag = 0
    private var foregroundColor = 0
    private var backgroundColor = 0
    private var lineHeight = 0
    private var alignLine = 0
    private var quoteColor = 0
    private var stripeWidth = 0
    private var quoteGapWidth = 0
    private var first = 0
    private var rest = 0
    private var bulletColor = 0
    private var bulletRadius = 0
    private var bulletGapWidth = 0
    private var fontSize = 0
    private var proportion = 0f
    private var xProportion = 0f
    private var isStrikethrough = false
    private var isUnderline = false
    private var isSuperscript = false
    private var isSubscript = false
    private var isBold = false
    private var isItalic = false
    private var isBoldItalic = false
    private var fontFamily: String? = null
    private var typeface: Typeface? = null
    private var alignment: Layout.Alignment? = null
    private var verticalAlign = 0
    private var clickSpan: ClickableSpan? = null
    private var url: String? = null
    private var blurRadius = 0f
    private var style: Blur? = null
    private var shader: Shader? = null
    private var shadowRadius = 0f
    private var shadowDx = 0f
    private var shadowDy = 0f
    private var shadowColor = 0
    private var spans: Array<Any>? = null

    private var imageBitmap: Bitmap? = null
    private var imageDrawable: Drawable? = null
    private var imageUri: Uri? = null
    private var imageResourceId = 0
    private var alignImage = 0

    private var spaceSize = 0
    private var spaceColor = 0

    private val mBuilder: SerializableSpannableStringBuilder
    private var isCreated = false

    private var mType: Int
    private val mTypeCharSequence = 0
    private val mTypeImage = 1
    private val mTypeSpace = 2

    private constructor(textView: TextView) : this() {
        mTextView = textView
    }

    init {
        mBuilder = SerializableSpannableStringBuilder()
        mText = ""
        mType = -1
        setDefault()
    }

    private fun setDefault() {
        flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        foregroundColor = COLOR_DEFAULT
        backgroundColor = COLOR_DEFAULT
        lineHeight = -1
        quoteColor = COLOR_DEFAULT
        first = -1
        bulletColor = COLOR_DEFAULT
        fontSize = -1
        proportion = -1f
        xProportion = -1f
        isStrikethrough = false
        isUnderline = false
        isSuperscript = false
        isSubscript = false
        isBold = false
        isItalic = false
        isBoldItalic = false
        fontFamily = null
        typeface = null
        alignment = null
        verticalAlign = -1
        clickSpan = null
        url = null
        blurRadius = -1f
        shader = null
        shadowRadius = -1f
        spans = null

        imageBitmap = null
        imageDrawable = null
        imageUri = null
        imageResourceId = -1

        spaceSize = -1
    }

    /**
     * Set the span of flag.
     *
     * @param flag The flag.
     *
     *  * [Spanned.SPAN_INCLUSIVE_EXCLUSIVE]
     *  * [Spanned.SPAN_INCLUSIVE_INCLUSIVE]
     *  * [Spanned.SPAN_EXCLUSIVE_EXCLUSIVE]
     *  * [Spanned.SPAN_EXCLUSIVE_INCLUSIVE]
     *
     * @return the single [SpansUtils] instance
     */
    fun setFlag(flag: Int): SpansUtils {
        this.flag = flag
        return this
    }

    /**
     * Set the span of foreground's color.
     *
     * @param color The color of foreground
     * @return the single [SpansUtils] instance
     */
    fun setForegroundColor(@ColorInt color: Int): SpansUtils {
        this.foregroundColor = color
        return this
    }

    /**
     * Set the span of background's color.
     *
     * @param color The color of background
     * @return the single [SpansUtils] instance
     */
    fun setBackgroundColor(@ColorInt color: Int): SpansUtils {
        this.backgroundColor = color
        return this
    }

    /**
     * Set the span of line height.
     *
     * @param lineHeight The line height, in pixel.
     * @return the single [SpansUtils] instance
     */
    fun setLineHeight(@IntRange(from = 0) lineHeight: Int): SpansUtils {
        return setLineHeight(lineHeight, ALIGN_CENTER)
    }

    /**
     * Set the span of line height.
     *
     * @param lineHeight The line height, in pixel.
     * @param align      The alignment.
     *
     *  * [Align.ALIGN_TOP]
     *  * [Align.ALIGN_CENTER]
     *  * [Align.ALIGN_BOTTOM]
     *
     * @return the single [SpansUtils] instance
     */
    fun setLineHeight(
        @IntRange(from = 0) lineHeight: Int,
        @Align align: Int
    ): SpansUtils {
        this.lineHeight = lineHeight
        this.alignLine = align
        return this
    }

    /**
     * Set the span of quote's color.
     *
     * @param color The color of quote
     * @return the single [SpansUtils] instance
     */
    fun setQuoteColor(@ColorInt color: Int): SpansUtils {
        return setQuoteColor(color, 2, 2)
    }

    /**
     * Set the span of quote's color.
     *
     * @param color       The color of quote.
     * @param stripeWidth The width of stripe, in pixel.
     * @param gapWidth    The width of gap, in pixel.
     * @return the single [SpansUtils] instance
     */
    fun setQuoteColor(
        @ColorInt color: Int,
        @IntRange(from = 1) stripeWidth: Int,
        @IntRange(from = 0) gapWidth: Int
    ): SpansUtils {
        this.quoteColor = color
        this.stripeWidth = stripeWidth
        this.quoteGapWidth = gapWidth
        return this
    }

    /**
     * Set the span of leading margin.
     *
     * @param first The indent for the first line of the paragraph.
     * @param rest  The indent for the remaining lines of the paragraph.
     * @return the single [SpansUtils] instance
     */
    fun setLeadingMargin(
        @IntRange(from = 0) first: Int,
        @IntRange(from = 0) rest: Int
    ): SpansUtils {
        this.first = first
        this.rest = rest
        return this
    }

    /**
     * Set the span of bullet.
     *
     * @param gapWidth The width of gap, in pixel.
     * @return the single [SpansUtils] instance
     */
    fun setBullet(@IntRange(from = 0) gapWidth: Int): SpansUtils {
        return setBullet(0, 3, gapWidth)
    }

    /**
     * Set the span of bullet.
     *
     * @param color    The color of bullet.
     * @param radius   The radius of bullet, in pixel.
     * @param gapWidth The width of gap, in pixel.
     * @return the single [SpansUtils] instance
     */
    fun setBullet(
        @ColorInt color: Int,
        @IntRange(from = 0) radius: Int,
        @IntRange(from = 0) gapWidth: Int
    ): SpansUtils {
        this.bulletColor = color
        this.bulletRadius = radius
        this.bulletGapWidth = gapWidth
        return this
    }

    /**
     * Set the span of font's size.
     *
     * @param size The size of font.
     * @return the single [SpansUtils] instance
     */
    fun setFontSize(@IntRange(from = 0) size: Int): SpansUtils {
        return setFontSize(size, false)
    }

    /**
     * Set the span of size of font.
     *
     * @param size The size of font.
     * @param isSp True to use sp, false to use pixel.
     * @return the single [SpansUtils] instance
     */
    fun setFontSize(@IntRange(from = 0) size: Int, isSp: Boolean): SpansUtils {
        if (isSp) {
            val fontScale = Resources.getSystem().displayMetrics.scaledDensity
            this.fontSize = (size * fontScale + 0.5f).toInt()
        } else {
            this.fontSize = size
        }
        return this
    }

    /**
     * Set the span of proportion of font.
     *
     * @param proportion The proportion of font.
     * @return the single [SpansUtils] instance
     */
    fun setFontProportion(proportion: Float): SpansUtils {
        this.proportion = proportion
        return this
    }

    /**
     * Set the span of transverse proportion of font.
     *
     * @param proportion The transverse proportion of font.
     * @return the single [SpansUtils] instance
     */
    fun setFontXProportion(proportion: Float): SpansUtils {
        this.xProportion = proportion
        return this
    }

    /**
     * Set the span of strikethrough.
     *
     * @return the single [SpansUtils] instance
     */
    fun setStrikethrough(): SpansUtils {
        this.isStrikethrough = true
        return this
    }

    /**
     * Set the span of underline.
     *
     * @return the single [SpansUtils] instance
     */
    fun setUnderline(): SpansUtils {
        this.isUnderline = true
        return this
    }

    /**
     * Set the span of superscript.
     *
     * @return the single [SpansUtils] instance
     */
    fun setSuperscript(): SpansUtils {
        this.isSuperscript = true
        return this
    }

    /**
     * Set the span of subscript.
     *
     * @return the single [SpansUtils] instance
     */
    fun setSubscript(): SpansUtils {
        this.isSubscript = true
        return this
    }

    /**
     * Set the span of bold.
     *
     * @return the single [SpansUtils] instance
     */
    fun setBold(): SpansUtils {
        isBold = true
        return this
    }

    /**
     * Set the span of italic.
     *
     * @return the single [SpansUtils] instance
     */
    fun setItalic(): SpansUtils {
        isItalic = true
        return this
    }

    /**
     * Set the span of bold italic.
     *
     * @return the single [SpansUtils] instance
     */
    fun setBoldItalic(): SpansUtils {
        isBoldItalic = true
        return this
    }

    /**
     * Set the span of font family.
     *
     * @param fontFamily The font family.
     *
     *  * monospace
     *  * serif
     *  * sans-serif
     *
     * @return the single [SpansUtils] instance
     */
    fun setFontFamily(fontFamily: String): SpansUtils {
        this.fontFamily = fontFamily
        return this
    }

    /**
     * Set the span of typeface.
     *
     * @param typeface The typeface.
     * @return the single [SpansUtils] instance
     */
    fun setTypeface(typeface: Typeface): SpansUtils {
        this.typeface = typeface
        return this
    }

    /**
     * Set the span of horizontal alignment.
     *
     * @param alignment The alignment.
     *
     *  * [Alignment.ALIGN_NORMAL]
     *  * [Alignment.ALIGN_OPPOSITE]
     *  * [Alignment.ALIGN_CENTER]
     *
     * @return the single [SpansUtils] instance
     */
    fun setHorizontalAlign(alignment: Layout.Alignment): SpansUtils {
        this.alignment = alignment
        return this
    }

    /**
     * Set the span of vertical alignment.
     *
     * @param align The alignment.
     *
     *  * [Align.ALIGN_TOP]
     *  * [Align.ALIGN_CENTER]
     *  * [Align.ALIGN_BASELINE]
     *  * [Align.ALIGN_BOTTOM]
     *
     * @return the single [SpansUtils] instance
     */
    fun setVerticalAlign(@Align align: Int): SpansUtils {
        this.verticalAlign = align
        return this
    }

    /**
     * Set the span of click.
     *
     * Must set `view.setMovementMethod(LinkMovementMethod.getInstance())`
     *
     * @param clickSpan The span of click.
     * @return the single [SpansUtils] instance
     */
    fun setClickSpan(clickSpan: ClickableSpan): SpansUtils {
        setMovementMethodIfNeed()
        this.clickSpan = clickSpan
        return this
    }

    /**
     * Set the span of click.
     *
     * Must set `view.setMovementMethod(LinkMovementMethod.getInstance())`
     *
     * @param color         The color of click span.
     * @param underlineText True to support underline, false otherwise.
     * @param listener      The listener of click span.
     * @return the single [SpansUtils] instance
     */
    fun setClickSpan(
        @ColorInt color: Int,
        underlineText: Boolean,
        listener: View.OnClickListener?
    ): SpansUtils {
        setMovementMethodIfNeed()
        this.clickSpan = object : ClickableSpan() {
            override fun updateDrawState(paint: TextPaint) {
                paint.color = color
                paint.isUnderlineText = underlineText
            }

            override fun onClick(widget: View) {
                listener?.onClick(widget)
            }
        }
        return this
    }

    /**
     * Set the span of url.
     *
     * Must set `view.setMovementMethod(LinkMovementMethod.getInstance())`
     *
     * @param url The url.
     * @return the single [SpansUtils] instance
     */
    fun setUrl(url: String): SpansUtils {
        setMovementMethodIfNeed()
        this.url = url
        return this
    }

    private fun setMovementMethodIfNeed() {
        if (mTextView != null && mTextView!!.movementMethod == null) {
            mTextView!!.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    /**
     * Set the span of blur.
     *
     * @param radius The radius of blur.
     * @param style  The style.
     *
     *  * [Blur.NORMAL]
     *  * [Blur.SOLID]
     *  * [Blur.OUTER]
     *  * [Blur.INNER]
     *
     * @return the single [SpansUtils] instance
     */
    fun setBlur(
        @FloatRange(from = 0.0, fromInclusive = false) radius: Float,
        style: Blur?
    ): SpansUtils {
        this.blurRadius = radius
        this.style = style
        return this
    }

    /**
     * Set the span of shader.
     *
     * @param shader The shader.
     * @return the single [SpansUtils] instance
     */
    fun setShader(shader: Shader): SpansUtils {
        this.shader = shader
        return this
    }

    /**
     * Set the span of shadow.
     *
     * @param radius      The radius of shadow.
     * @param dx          X-axis offset, in pixel.
     * @param dy          Y-axis offset, in pixel.
     * @param shadowColor The color of shadow.
     * @return the single [SpansUtils] instance
     */
    fun setShadow(
        @FloatRange(from = 0.0, fromInclusive = false) radius: Float,
        dx: Float,
        dy: Float,
        shadowColor: Int
    ): SpansUtils {
        this.shadowRadius = radius
        this.shadowDx = dx
        this.shadowDy = dy
        this.shadowColor = shadowColor
        return this
    }


    /**
     * Set the spans.
     *
     * @param spans The spans.
     * @return the single [SpansUtils] instance
     */
    fun setSpans(vararg spans: Any): SpansUtils {
        if (spans.size > 0) {
            this.spans = arrayOf(spans)
        }
        return this
    }

    /**
     * Append the text text.
     *
     * @param text The text.
     * @return the single [SpansUtils] instance
     */
    fun append(text: CharSequence): SpansUtils {
        apply(mTypeCharSequence)
        mText = text
        return this
    }

    /**
     * Append one line.
     *
     * @return the single [SpansUtils] instance
     */
    fun appendLine(): SpansUtils {
        apply(mTypeCharSequence)
        mText = LINE_SEPARATOR
        return this
    }

    /**
     * Append text and one line.
     *
     * @return the single [SpansUtils] instance
     */
    fun appendLine(text: CharSequence): SpansUtils {
        apply(mTypeCharSequence)
        mText = text.toString() + LINE_SEPARATOR
        return this
    }

    /**
     * Append one image.
     *
     * @param bitmap The bitmap.
     * @param align  The alignment.
     *
     *  * [Align.ALIGN_TOP]
     *  * [Align.ALIGN_CENTER]
     *  * [Align.ALIGN_BASELINE]
     *  * [Align.ALIGN_BOTTOM]
     *
     * @return the single [SpansUtils] instance
     */
    /**
     * Append one image.
     *
     * @param bitmap The bitmap of image.
     * @return the single [SpansUtils] instance
     */
    @JvmOverloads
    fun appendImage(bitmap: Bitmap, @Align align: Int = ALIGN_BOTTOM): SpansUtils {
        apply(mTypeImage)
        this.imageBitmap = bitmap
        this.alignImage = align
        return this
    }

    /**
     * Append one image.
     *
     * @param drawable The drawable of image.
     * @param align    The alignment.
     *
     *  * [Align.ALIGN_TOP]
     *  * [Align.ALIGN_CENTER]
     *  * [Align.ALIGN_BASELINE]
     *  * [Align.ALIGN_BOTTOM]
     *
     * @return the single [SpansUtils] instance
     */
    /**
     * Append one image.
     *
     * @param drawable The drawable of image.
     * @return the single [SpansUtils] instance
     */
    @JvmOverloads
    fun appendImage(drawable: Drawable, @Align align: Int = ALIGN_BOTTOM): SpansUtils {
        apply(mTypeImage)
        this.imageDrawable = drawable
        this.alignImage = align
        return this
    }

    /**
     * Append one image.
     *
     * @param uri   The uri of image.
     * @param align The alignment.
     *
     *  * [Align.ALIGN_TOP]
     *  * [Align.ALIGN_CENTER]
     *  * [Align.ALIGN_BASELINE]
     *  * [Align.ALIGN_BOTTOM]
     *
     * @return the single [SpansUtils] instance
     */
    /**
     * Append one image.
     *
     * @param uri The uri of image.
     * @return the single [SpansUtils] instance
     */
    @JvmOverloads
    fun appendImage(uri: Uri, @Align align: Int = ALIGN_BOTTOM): SpansUtils {
        apply(mTypeImage)
        this.imageUri = uri
        this.alignImage = align
        return this
    }

    /**
     * Append one image.
     *
     * @param resourceId The resource id of image.
     * @param align      The alignment.
     *
     *  * [Align.ALIGN_TOP]
     *  * [Align.ALIGN_CENTER]
     *  * [Align.ALIGN_BASELINE]
     *  * [Align.ALIGN_BOTTOM]
     *
     * @return the single [SpansUtils] instance
     */
    /**
     * Append one image.
     *
     * @param resourceId The resource id of image.
     * @return the single [SpansUtils] instance
     */
    @JvmOverloads
    fun appendImage(@DrawableRes resourceId: Int, @Align align: Int = ALIGN_BOTTOM): SpansUtils {
        apply(mTypeImage)
        this.imageResourceId = resourceId
        this.alignImage = align
        return this
    }

    /**
     * Append space.
     *
     * @param size  The size of space.
     * @param color The color of space.
     * @return the single [SpansUtils] instance
     */
    /**
     * Append space.
     *
     * @param size The size of space.
     * @return the single [SpansUtils] instance
     */
    @JvmOverloads
    fun appendSpace(
        @IntRange(from = 0) size: Int,
        @ColorInt color: Int = Color.TRANSPARENT
    ): SpansUtils {
        apply(mTypeSpace)
        spaceSize = size
        spaceColor = color
        return this
    }

    private fun apply(type: Int) {
        applyLast()
        mType = type
    }

    fun get(): SpannableStringBuilder {
        return mBuilder
    }

    /**
     * Create the span string.
     *
     * @return the span string
     */
    fun create(): SpannableStringBuilder {
        applyLast()
        if (mTextView != null) {
            mTextView!!.text = mBuilder
        }
        isCreated = true
        return mBuilder
    }

    private fun applyLast() {
        if (isCreated) {
            return
        }
        if (mType == mTypeCharSequence) {
            updateCharCharSequence()
        } else if (mType == mTypeImage) {
            updateImage()
        } else if (mType == mTypeSpace) {
            updateSpace()
        }
        setDefault()
    }

    private fun updateCharCharSequence() {
        if (mText!!.length == 0) {
            return
        }
        var start = mBuilder.length
        if (start == 0 && lineHeight != -1) { // bug of LineHeightSpan when first line
            mBuilder.append(Character.toString(2.toChar()))
                .append("\n")
                .setSpan(AbsoluteSizeSpan(0), 0, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = 2
        }
        mBuilder.append(mText)
        val end = mBuilder.length
        if (verticalAlign != -1) {
            mBuilder.setSpan(VerticalAlignSpan(verticalAlign), start, end, flag)
        }
        if (foregroundColor != COLOR_DEFAULT) {
            mBuilder.setSpan(ForegroundColorSpan(foregroundColor), start, end, flag)
        }
        if (backgroundColor != COLOR_DEFAULT) {
            mBuilder.setSpan(BackgroundColorSpan(backgroundColor), start, end, flag)
        }
        if (first != -1) {
            mBuilder.setSpan(LeadingMarginSpan.Standard(first, rest), start, end, flag)
        }
        if (quoteColor != COLOR_DEFAULT) {
            mBuilder.setSpan(
                CustomQuoteSpan(quoteColor, stripeWidth, quoteGapWidth),
                start,
                end,
                flag
            )
        }
        if (bulletColor != COLOR_DEFAULT) {
            mBuilder.setSpan(
                CustomBulletSpan(bulletColor, bulletRadius, bulletGapWidth),
                start,
                end,
                flag
            )
        }
        if (fontSize != -1) {
            mBuilder.setSpan(AbsoluteSizeSpan(fontSize, false), start, end, flag)
        }
        if (proportion != -1f) {
            mBuilder.setSpan(RelativeSizeSpan(proportion), start, end, flag)
        }
        if (xProportion != -1f) {
            mBuilder.setSpan(ScaleXSpan(xProportion), start, end, flag)
        }
        if (lineHeight != -1) {
            mBuilder.setSpan(CustomLineHeightSpan(lineHeight, alignLine), start, end, flag)
        }
        if (isStrikethrough) {
            mBuilder.setSpan(StrikethroughSpan(), start, end, flag)
        }
        if (isUnderline) {
            mBuilder.setSpan(UnderlineSpan(), start, end, flag)
        }
        if (isSuperscript) {
            mBuilder.setSpan(SuperscriptSpan(), start, end, flag)
        }
        if (isSubscript) {
            mBuilder.setSpan(SubscriptSpan(), start, end, flag)
        }
        if (isBold) {
            mBuilder.setSpan(StyleSpan(Typeface.BOLD), start, end, flag)
        }
        if (isItalic) {
            mBuilder.setSpan(StyleSpan(Typeface.ITALIC), start, end, flag)
        }
        if (isBoldItalic) {
            mBuilder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, flag)
        }
        if (fontFamily != null) {
            mBuilder.setSpan(TypefaceSpan(fontFamily), start, end, flag)
        }
        if (typeface != null) {
            mBuilder.setSpan(CustomTypefaceSpan(typeface!!), start, end, flag)
        }
        if (alignment != null) {
            mBuilder.setSpan(AlignmentSpan.Standard(alignment!!), start, end, flag)
        }
        if (clickSpan != null) {
            mBuilder.setSpan(clickSpan, start, end, flag)
        }
        if (url != null) {
            mBuilder.setSpan(URLSpan(url), start, end, flag)
        }
        if (blurRadius != -1f) {
            mBuilder.setSpan(
                MaskFilterSpan(BlurMaskFilter(blurRadius, style)),
                start,
                end,
                flag
            )
        }
        if (shader != null) {
            mBuilder.setSpan(ShaderSpan(shader!!), start, end, flag)
        }
        if (shadowRadius != -1f) {
            mBuilder.setSpan(
                ShadowSpan(shadowRadius, shadowDx, shadowDy, shadowColor),
                start,
                end,
                flag
            )
        }
        if (spans != null) {
            for (span in spans!!) {
                mBuilder.setSpan(span, start, end, flag)
            }
        }
    }

    private fun updateImage() {
        val start = mBuilder.length
        mText = "<img>"
        updateCharCharSequence()
        val end = mBuilder.length
        if (imageBitmap != null) {
            mBuilder.setSpan(CustomImageSpan(imageBitmap!!, alignImage), start, end, flag)
        } else if (imageDrawable != null) {
            mBuilder.setSpan(CustomImageSpan(imageDrawable!!, alignImage), start, end, flag)
        } else if (imageUri != null) {
            mBuilder.setSpan(CustomImageSpan(imageUri!!, alignImage), start, end, flag)
        } else if (imageResourceId != -1) {
            mBuilder.setSpan(CustomImageSpan(imageResourceId, alignImage), start, end, flag)
        }
    }

    private fun updateSpace() {
        val start = mBuilder.length
        mText = "< >"
        updateCharCharSequence()
        val end = mBuilder.length
        mBuilder.setSpan(SpaceSpan(spaceSize, spaceColor), start, end, flag)
    }

    internal class VerticalAlignSpan(val mVerticalAlignment: Int) : ReplacementSpan() {
        override fun getSize(
            paint: Paint,
            text: CharSequence,
            start: Int,
            end: Int,
            fm: FontMetricsInt?
        ): Int {
            var text = text
            text = text.subSequence(start, end)
            return paint.measureText(text.toString()).toInt()
        }

        override fun draw(
            canvas: Canvas,
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            top: Int,
            y: Int,
            bottom: Int,
            paint: Paint
        ) {
            var text = text
            text = text.subSequence(start, end)
            val fm = paint.fontMetricsInt

            //            int need = height - (v + fm.descent - fm.ascent - spanstartv);
//            if (need > 0) {
//                if (mVerticalAlignment == ALIGN_TOP) {
//                    fm.descent += need;
//                } else if (mVerticalAlignment == ALIGN_CENTER) {
//                    fm.descent += need / 2;
//                    fm.ascent -= need / 2;
//                } else {
//                    fm.ascent -= need;
//                }
//            }
//            need = height - (v + fm.bottom - fm.top - spanstartv);
//            if (need > 0) {
//                if (mVerticalAlignment == ALIGN_TOP) {
//                    fm.bottom += need;
//                } else if (mVerticalAlignment == ALIGN_CENTER) {
//                    fm.bottom += need / 2;
//                    fm.top -= need / 2;
//                } else {
//                    fm.top -= need;
//                }
//            }
            canvas.drawText(
                text.toString(),
                x,
                (y - ((y + fm.descent + y + fm.ascent) / 2 - (bottom + top) / 2)).toFloat(),
                paint
            )
        }

        companion object {
            const val ALIGN_CENTER: Int = 2
            const val ALIGN_TOP: Int = 3
        }
    }

    internal class CustomLineHeightSpan(private val height: Int, val mVerticalAlignment: Int) :
        LineHeightSpan {
        override fun chooseHeight(
            text: CharSequence, start: Int, end: Int,
            spanstartv: Int, v: Int, fm: FontMetricsInt
        ) {
//            LogUtils.e(fm, sfm);
            if (sfm == null) {
                sfm = FontMetricsInt()
                sfm!!.top = fm.top
                sfm!!.ascent = fm.ascent
                sfm!!.descent = fm.descent
                sfm!!.bottom = fm.bottom
                sfm!!.leading = fm.leading
            } else {
                fm.top = sfm!!.top
                fm.ascent = sfm!!.ascent
                fm.descent = sfm!!.descent
                fm.bottom = sfm!!.bottom
                fm.leading = sfm!!.leading
            }
            var need = height - (v + fm.descent - fm.ascent - spanstartv)
            if (need > 0) {
                if (mVerticalAlignment == ALIGN_TOP) {
                    fm.descent += need
                } else if (mVerticalAlignment == ALIGN_CENTER) {
                    fm.descent += need / 2
                    fm.ascent -= need / 2
                } else {
                    fm.ascent -= need
                }
            }
            need = height - (v + fm.bottom - fm.top - spanstartv)
            if (need > 0) {
                if (mVerticalAlignment == ALIGN_TOP) {
                    fm.bottom += need
                } else if (mVerticalAlignment == ALIGN_CENTER) {
                    fm.bottom += need / 2
                    fm.top -= need / 2
                } else {
                    fm.top -= need
                }
            }
            if (end == (text as Spanned).getSpanEnd(this)) {
                sfm = null
            }
            //            LogUtils.e(fm, sfm);
        }

        companion object {
            const val ALIGN_CENTER: Int = 2
            const val ALIGN_TOP: Int = 3

            var sfm: FontMetricsInt? = null
        }
    }

    internal class SpaceSpan(private val width: Int, color: Int = Color.TRANSPARENT) :
        ReplacementSpan() {
        private val paint = Paint()

        init {
            paint.color = color
            paint.style = Paint.Style.FILL
        }

        override fun getSize(
            paint: Paint, text: CharSequence,
            @IntRange(from = 0) start: Int,
            @IntRange(from = 0) end: Int,
            fm: FontMetricsInt?
        ): Int {
            return width
        }

        override fun draw(
            canvas: Canvas, text: CharSequence,
            @IntRange(from = 0) start: Int,
            @IntRange(from = 0) end: Int,
            x: Float, top: Int, y: Int, bottom: Int,
            paint: Paint
        ) {
            canvas.drawRect(x, top.toFloat(), x + width, bottom.toFloat(), this.paint)
        }
    }

    internal class CustomQuoteSpan(
        private val color: Int,
        private val stripeWidth: Int,
        private val gapWidth: Int
    ) :
        LeadingMarginSpan {
        override fun getLeadingMargin(first: Boolean): Int {
            return stripeWidth + gapWidth
        }

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, layout: Layout
        ) {
            val style = p.style
            val color = p.color

            p.style = Paint.Style.FILL
            p.color = this.color

            c.drawRect(
                x.toFloat(),
                top.toFloat(),
                (x + dir * stripeWidth).toFloat(),
                bottom.toFloat(),
                p
            )

            p.style = style
            p.color = color
        }
    }

    internal class CustomBulletSpan(
        private val color: Int,
        private val radius: Int,
        private val gapWidth: Int
    ) :
        LeadingMarginSpan {
        private var sBulletPath: Path? = null

        override fun getLeadingMargin(first: Boolean): Int {
            return 2 * radius + gapWidth
        }

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, l: Layout
        ) {
            if ((text as Spanned).getSpanStart(this) == start) {
                val style = p.style
                var oldColor = 0
                oldColor = p.color
                p.color = color
                p.style = Paint.Style.FILL
                if (c.isHardwareAccelerated) {
                    if (sBulletPath == null) {
                        sBulletPath = Path()
                        // Bullet is slightly better to avoid aliasing artifacts on mdpi devices.
                        sBulletPath!!.addCircle(0.0f, 0.0f, radius.toFloat(), Path.Direction.CW)
                    }
                    c.save()
                    c.translate((x + dir * radius).toFloat(), (top + bottom) / 2.0f)
                    c.drawPath(sBulletPath!!, p)
                    c.restore()
                } else {
                    c.drawCircle(
                        (x + dir * radius).toFloat(),
                        (top + bottom) / 2.0f,
                        radius.toFloat(),
                        p
                    )
                }
                p.color = oldColor
                p.style = style
            }
        }
    }

    @SuppressLint("ParcelCreator")
    internal class CustomTypefaceSpan(private val newType: Typeface) : TypefaceSpan("") {
        override fun updateDrawState(textPaint: TextPaint) {
            apply(textPaint, newType)
        }

        override fun updateMeasureState(paint: TextPaint) {
            apply(paint, newType)
        }

        private fun apply(paint: Paint, tf: Typeface) {
            val oldStyle: Int
            val old = paint.typeface
            oldStyle = old?.style ?: 0

            val fake = oldStyle and tf.style.inv()
            if ((fake and Typeface.BOLD) != 0) {
                paint.isFakeBoldText = true
            }

            if ((fake and Typeface.ITALIC) != 0) {
                paint.textSkewX = -0.25f
            }

            paint.shader

            paint.setTypeface(tf)
        }
    }

    internal class CustomImageSpan : CustomDynamicDrawableSpan {
        private var mDrawable: Drawable? = null
        private var mContentUri: Uri? = null
        private var mResourceId = 0

        constructor(b: Bitmap, verticalAlignment: Int) : super(verticalAlignment) {
            mDrawable = b.toDrawable(Utils.getApplication().resources)
            (mDrawable as BitmapDrawable).setBounds(
                0, 0, (mDrawable as BitmapDrawable).getIntrinsicWidth(), (mDrawable as BitmapDrawable).getIntrinsicHeight()
            )
        }

        constructor(d: Drawable, verticalAlignment: Int) : super(verticalAlignment) {
            mDrawable = d
            mDrawable!!.setBounds(
                0, 0, mDrawable!!.intrinsicWidth, mDrawable!!.intrinsicHeight
            )
        }

        constructor(uri: Uri, verticalAlignment: Int) : super(verticalAlignment) {
            mContentUri = uri
        }

        constructor(
            @DrawableRes resourceId: Int,
            verticalAlignment: Int
        ) : super(verticalAlignment) {
            mResourceId = resourceId
        }

        override val drawable: Drawable?
            get() {
                var drawable: Drawable? = null
                if (mDrawable != null) {
                    drawable = mDrawable
                } else if (mContentUri != null) {
                    val bitmap: Bitmap
                    try {
                        val `is` =
                            Utils.  getApplication()
                                .contentResolver.openInputStream(mContentUri!!)
                        bitmap = BitmapFactory.decodeStream(`is`)
                        drawable = BitmapDrawable(
                            Utils.  getApplication().resources,
                            bitmap
                        )
                        drawable.setBounds(
                            0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight()
                        )
                        `is`?.close()
                    } catch (e: Exception) {
                        Log.e("sms", "Failed to loaded content $mContentUri", e)
                    }
                } else {
                    try {
                        drawable = ContextCompat.getDrawable(
                            Utils.getApplication(),
                            mResourceId
                        )
                        drawable!!.setBounds(
                            0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return drawable
            }
    }

    internal abstract class CustomDynamicDrawableSpan : ReplacementSpan {
        val mVerticalAlignment: Int

        private constructor() {
            mVerticalAlignment = ALIGN_BOTTOM
        }

        protected constructor(verticalAlignment: Int) {
            mVerticalAlignment = verticalAlignment
        }

        abstract val drawable: Drawable?

        override fun getSize(
            paint: Paint, text: CharSequence,
            start: Int, end: Int, fm: FontMetricsInt?
        ): Int {
            val d = cachedDrawable
            val rect = d!!.bounds
            if (fm != null) {
//                LogUtils.d("fm.top: " + fm.top,
//                        "fm.ascent: " + fm.ascent,
//                        "fm.descent: " + fm.descent,
//                        "fm.bottom: " + fm.bottom,
//                        "lineHeight: " + (fm.bottom - fm.top));
                val lineHeight = fm.bottom - fm.top
                if (lineHeight < rect.height()) {
                    if (mVerticalAlignment == ALIGN_TOP) {
                        fm.top = fm.top
                        fm.bottom = rect.height() + fm.top
                    } else if (mVerticalAlignment == ALIGN_CENTER) {
                        fm.top = -rect.height() / 2 - lineHeight / 4
                        fm.bottom = rect.height() / 2 - lineHeight / 4
                    } else {
                        fm.top = -rect.height() + fm.bottom
                        fm.bottom = fm.bottom
                    }
                    fm.ascent = fm.top
                    fm.descent = fm.bottom
                }
            }
            return rect.right
        }

        override fun draw(
            canvas: Canvas, text: CharSequence,
            start: Int, end: Int, x: Float,
            top: Int, y: Int, bottom: Int, paint: Paint
        ) {
            val d = cachedDrawable
            val rect = d!!.bounds
            canvas.save()
            val transY: Float
            val lineHeight = bottom - top
            //            LogUtils.d("rectHeight: " + rect.height(),
//                    "lineHeight: " + (bottom - top));
            if (rect.height() < lineHeight) {
                transY = if (mVerticalAlignment == ALIGN_TOP) {
                    top.toFloat()
                } else if (mVerticalAlignment == ALIGN_CENTER) {
                    ((bottom + top - rect.height()) / 2).toFloat()
                } else if (mVerticalAlignment == ALIGN_BASELINE) {
                    (y - rect.height()).toFloat()
                } else {
                    (bottom - rect.height()).toFloat()
                }
                canvas.translate(x, transY)
            } else {
                canvas.translate(x, top.toFloat())
            }
            d.draw(canvas)
            canvas.restore()
        }

        private val cachedDrawable: Drawable?
            get() {
                val wr = mDrawableRef
                var d: Drawable? = null
                if (wr != null) {
                    d = wr.get()
                }
                if (d == null) {
                    d = drawable
                    mDrawableRef = WeakReference(d)
                }
                return d
            }

        private var mDrawableRef: WeakReference<Drawable?>? = null

        companion object {
            const val ALIGN_BOTTOM: Int = 0

            const val ALIGN_BASELINE: Int = 1

            const val ALIGN_CENTER: Int = 2

            const val ALIGN_TOP: Int = 3
        }
    }

    internal class ShaderSpan(private val mShader: Shader) : CharacterStyle(),
        UpdateAppearance {
        override fun updateDrawState(tp: TextPaint) {
            tp.setShader(mShader)
        }
    }

    internal class ShadowSpan(
        private val radius: Float,
        private val dx: Float,
        private val dy: Float,
        private val shadowColor: Int
    ) : CharacterStyle(),
        UpdateAppearance {
        override fun updateDrawState(tp: TextPaint) {
            tp.setShadowLayer(radius, dx, dy, shadowColor)
        }
    }

    private class SerializableSpannableStringBuilder : SpannableStringBuilder(), Serializable {
     companion object{
         private const val serialVersionUID = 4909567650765875771L
     }
    }

    companion object {
        private const val COLOR_DEFAULT = -0x1000001

        const val ALIGN_BOTTOM: Int = 0
        const val ALIGN_BASELINE: Int = 1
        const val ALIGN_CENTER: Int = 2
        const val ALIGN_TOP: Int = 3

        private val LINE_SEPARATOR: String? = System.lineSeparator()

        fun with(textView: TextView): SpansUtils {
            return SpansUtils(textView)
        }
    }
}