package com.xuexiang.xupdate.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.xuexiang.flutter_xupdate.R
import kotlin.math.max
import kotlin.math.min

/**
 * 数字进度条
 *
 * @author xuexiang
 * @since 2018/7/2 上午11:23
 */
class NumberProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * 当前进度
     */
    var progress: Int = 0
        set(value) {
            if (value <= maxProgress && value >= 0) {
                field = value
                postInvalidate()
            }
        }

    var maxProgress: Int = 100
        set(value) {
            if (value > 0) {
                field = value
                postInvalidate()
            }
        }

    /**
     * 已到达区域的颜色
     */
    var reachedBarColor: Int = Color.rgb(66, 145, 241)
        set(value) {
            field = value
            mReachedBarPaint.color = reachedBarColor
            postInvalidate()
        }

    /**
     * 未到达区域的颜色
     */
    var unreachedBarColor: Int = Color.rgb(204, 204, 204)
        set(value) {
            field = value
            mUnreachedBarPaint.color = unreachedBarColor
            postInvalidate()
        }

    /**
     * 进度文字颜色
     */
    var progressTextColor: Int = Color.rgb(66, 145, 241)
        set(value) {
            field = value
            mTextPaint.color = progressTextColor
            postInvalidate()
        }

    /**
     * 进度文字大小
     */
    var progressTextSize: Float = sp2px(10f)
        set(value) {
            field = value
            mTextPaint.textSize = progressTextSize
            postInvalidate()
        }

    /**
     * 已到达区域的高度
     */
    var reachedBarHeight: Float = dp2px(1.5f)

    /**
     * 未到达区域的高度
     */
    var unreachedBarHeight: Float = dp2px(1.0f)

    /**
     * 数字的间隔字符
     */
    var suffix: String = "%"
        set(value) {
            field = value ?: ""
        }

    /**
     * 前缀
     */
    var prefix: String = ""
        set(value) {
            field = value ?: ""
        }

    /**
     * 进度文字开始位置
     */
    private var mDrawTextStart = 0f

    /**
     * 进度文字结束位置
     */
    private var mDrawTextEnd = 0f

    /**
     * 绘制的文字
     */
    private var mCurrentDrawText: String = ""

    /**
     * 已到达区域的画笔
     */
    private val mReachedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 未到达区域的画笔
     */
    private val mUnreachedBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 进度文字的画笔
     */
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * 未到达区域的绘制矩形
     */
    private val mUnreachedRectF = RectF(0f, 0f, 0f, 0f)

    /**
     * 已到达区域的绘制矩形
     */
    private val mReachedRectF = RectF(0f, 0f, 0f, 0f)

    /**
     * 进度文字偏移量
     */
    private var mOffset: Float = dp2px(3.0f)

    /**
     * 是否绘制未到达区域
     */
    private var mDrawUnreachedBar = true

    private var mDrawReachedBar = true

    private var mIfDrawText = true

    /**
     * 进度条监听
     */
    private var mListener: OnProgressBarListener? = null

    init {
        val defaultReachedBarHeight = dp2px(1.5f)
        val defaultUnreachedBarHeight = dp2px(1.0f)
        val defaultTextSize = sp2px(10f)
        val defaultProgressTextOffset = dp2px(3.0f)

        //load styled attributes.
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.XNumberProgressBar, defStyleAttr, 0)

        reachedBarColor = attributes.getColor(R.styleable.XNumberProgressBar_xnpb_reached_color, Color.rgb(66, 145, 241))
        unreachedBarColor = attributes.getColor(R.styleable.XNumberProgressBar_xnpb_unreached_color, Color.rgb(204, 204, 204))
        progressTextColor = attributes.getColor(R.styleable.XNumberProgressBar_xnpb_text_color, Color.rgb(66, 145, 241))
        progressTextSize = attributes.getDimension(R.styleable.XNumberProgressBar_xnpb_text_size, defaultTextSize)
        reachedBarHeight = attributes.getDimension(R.styleable.XNumberProgressBar_xnpb_reached_bar_height, defaultReachedBarHeight)
        unreachedBarHeight = attributes.getDimension(R.styleable.XNumberProgressBar_xnpb_unreached_bar_height, defaultUnreachedBarHeight)
        mOffset = attributes.getDimension(R.styleable.XNumberProgressBar_xnpb_text_offset, defaultProgressTextOffset)

        val textVisible = attributes.getInt(R.styleable.XNumberProgressBar_xnpb_text_visibility, PROGRESS_TEXT_VISIBLE)
        if (textVisible != PROGRESS_TEXT_VISIBLE) {
            mIfDrawText = false
        }

        progress = attributes.getInt(R.styleable.XNumberProgressBar_xnpb_current, 0)
        maxProgress = attributes.getInt(R.styleable.XNumberProgressBar_xnpb_max, 100)

        attributes.recycle()
        initializePainters()
    }

    override fun getSuggestedMinimumWidth(): Int {
        return progressTextSize.toInt()
    }

    override fun getSuggestedMinimumHeight(): Int {
        return max(progressTextSize.toInt(), max(reachedBarHeight.toInt(), unreachedBarHeight.toInt()))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measure(widthMeasureSpec, true), measure(heightMeasureSpec, false))
    }

    private fun measure(measureSpec: Int, isWidth: Boolean): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        val padding = if (isWidth) paddingLeft + paddingRight else paddingTop + paddingBottom
        var result: Int
        if (mode == MeasureSpec.EXACTLY) {
            result = size
        } else {
            result = (if (isWidth) suggestedMinimumWidth else suggestedMinimumHeight) + padding
            if (mode == MeasureSpec.AT_MOST) {
                result = if (isWidth) max(result, size) else min(result, size)
            }
        }
        return result
    }

    override fun onDraw(canvas: Canvas) {
        if (mIfDrawText) {
            calculateDrawRectF()
        } else {
            calculateDrawRectFWithoutProgressText()
        }

        if (mDrawReachedBar) {
            canvas.drawRect(mReachedRectF, mReachedBarPaint)
        }

        if (mDrawUnreachedBar) {
            canvas.drawRect(mUnreachedRectF, mUnreachedBarPaint)
        }

        if (mIfDrawText) {
            canvas.drawText(mCurrentDrawText, mDrawTextStart, mDrawTextEnd, mTextPaint)
        }
    }

    private fun initializePainters() {
        mReachedBarPaint.color = reachedBarColor
        mUnreachedBarPaint.color = unreachedBarColor
        mTextPaint.color = progressTextColor
        mTextPaint.textSize = progressTextSize
    }

    private fun calculateDrawRectFWithoutProgressText() {
        mReachedRectF.left = paddingLeft.toFloat()
        mReachedRectF.top = height / 2.0f - reachedBarHeight / 2.0f
        mReachedRectF.right =
            (width - paddingLeft - paddingRight) / (maxProgress * 1.0f) * progress + paddingLeft
        mReachedRectF.bottom = height / 2.0f + reachedBarHeight / 2.0f

        mUnreachedRectF.left = mReachedRectF.right
        mUnreachedRectF.right = (width - paddingRight).toFloat()
        mUnreachedRectF.top = height / 2.0f + -unreachedBarHeight / 2.0f
        mUnreachedRectF.bottom = height / 2.0f + unreachedBarHeight / 2.0f
    }

    private fun calculateDrawRectF() {
        mCurrentDrawText = String.format("%d", progress * 100 / maxProgress)
        mCurrentDrawText = prefix + mCurrentDrawText + suffix

        /*
         The width of the text that to be drawn.
        */
        val drawTextWidth = mTextPaint.measureText(mCurrentDrawText)

        if (progress == 0) {
            mDrawReachedBar = false
            mDrawTextStart = paddingLeft.toFloat()
        } else {
            mDrawReachedBar = true
            mReachedRectF.left = paddingLeft.toFloat()
            mReachedRectF.top = height / 2.0f - reachedBarHeight / 2.0f
            mReachedRectF.right =
                (width - paddingLeft - paddingRight) / (maxProgress * 1.0f) * progress - mOffset + paddingLeft
            mReachedRectF.bottom = height / 2.0f + reachedBarHeight / 2.0f
            mDrawTextStart = mReachedRectF.right + mOffset
        }

        mDrawTextEnd = (height / 2.0f - (mTextPaint.descent() + mTextPaint.ascent()) / 2.0f).toInt().toFloat()

        if (mDrawTextStart + drawTextWidth >= width - paddingRight) {
            mDrawTextStart = (width - paddingRight - drawTextWidth)
            mReachedRectF.right = mDrawTextStart - mOffset
        }

        val unreachedBarStart = mDrawTextStart + drawTextWidth + mOffset
        if (unreachedBarStart >= width - paddingRight) {
            mDrawUnreachedBar = false
        } else {
            mDrawUnreachedBar = true
            mUnreachedRectF.left = unreachedBarStart
            mUnreachedRectF.right = (width - paddingRight).toFloat()
            mUnreachedRectF.top = height / 2.0f + -unreachedBarHeight / 2.0f
            mUnreachedRectF.bottom = height / 2.0f + unreachedBarHeight / 2.0f
        }
    }

    fun setProgressTextVisibility(visibility: ProgressTextVisibility) {
        mIfDrawText = visibility == ProgressTextVisibility.VISIBLE
        postInvalidate()
    }

    fun getProgressTextVisibility(): Boolean {
        return mIfDrawText
    }

    fun incrementProgressBy(by: Int) {
        if (by > 0) {
            progress += by
        }
        mListener?.onProgressChange(progress, maxProgress)
    }

    fun setOnProgressBarListener(listener: OnProgressBarListener?) {
        mListener = listener
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putInt(INSTANCE_TEXT_COLOR, progressTextColor)
        bundle.putFloat(INSTANCE_TEXT_SIZE, progressTextSize)
        bundle.putFloat(INSTANCE_REACHED_BAR_HEIGHT, reachedBarHeight)
        bundle.putFloat(INSTANCE_UNREACHED_BAR_HEIGHT, unreachedBarHeight)
        bundle.putInt(INSTANCE_REACHED_BAR_COLOR, reachedBarColor)
        bundle.putInt(INSTANCE_UNREACHED_BAR_COLOR, unreachedBarColor)
        bundle.putInt(INSTANCE_MAX, maxProgress)
        bundle.putInt(INSTANCE_PROGRESS, progress)
        bundle.putString(INSTANCE_SUFFIX, suffix)
        bundle.putString(INSTANCE_PREFIX, prefix)
        bundle.putBoolean(INSTANCE_TEXT_VISIBILITY, getProgressTextVisibility())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            progressTextColor = state.getInt(INSTANCE_TEXT_COLOR)
            progressTextSize = state.getFloat(INSTANCE_TEXT_SIZE)
            reachedBarHeight = state.getFloat(INSTANCE_REACHED_BAR_HEIGHT)
            unreachedBarHeight = state.getFloat(INSTANCE_UNREACHED_BAR_HEIGHT)
            reachedBarColor = state.getInt(INSTANCE_REACHED_BAR_COLOR)
            unreachedBarColor = state.getInt(INSTANCE_UNREACHED_BAR_COLOR)
            initializePainters()
            maxProgress = state.getInt(INSTANCE_MAX)
            progress = state.getInt(INSTANCE_PROGRESS)
            prefix = state.getString(INSTANCE_PREFIX) ?: ""
            suffix = state.getString(INSTANCE_SUFFIX) ?: ""
            setProgressTextVisibility(
                if (state.getBoolean(INSTANCE_TEXT_VISIBILITY)) ProgressTextVisibility.VISIBLE
                else ProgressTextVisibility.INVISIBLE
            )
            super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    fun dp2px(dp: Float): Float {
        val scale = resources.displayMetrics.density
        return dp * scale + 0.5f
    }

    fun sp2px(sp: Float): Float {
        val scale = resources.displayMetrics.scaledDensity
        return sp * scale
    }

    enum class ProgressTextVisibility {
        VISIBLE, INVISIBLE
    }

    fun interface OnProgressBarListener {
        /**
         * 进度变化
         *
         * @param current
         * @param max
         */
        fun onProgressChange(current: Int, max: Int)
    }

    companion object {
        /**
         * For save and restore instance of progressbar.
         */
        private const val INSTANCE_STATE = "saved_instance"
        private const val INSTANCE_TEXT_COLOR = "text_color"
        private const val INSTANCE_TEXT_SIZE = "text_size"
        private const val INSTANCE_REACHED_BAR_HEIGHT = "reached_bar_height"
        private const val INSTANCE_REACHED_BAR_COLOR = "reached_bar_color"
        private const val INSTANCE_UNREACHED_BAR_HEIGHT = "unreached_bar_height"
        private const val INSTANCE_UNREACHED_BAR_COLOR = "unreached_bar_color"
        private const val INSTANCE_MAX = "max"
        private const val INSTANCE_PROGRESS = "progress"
        private const val INSTANCE_SUFFIX = "suffix"
        private const val INSTANCE_PREFIX = "prefix"
        private const val INSTANCE_TEXT_VISIBILITY = "text_visibility"
        private const val PROGRESS_TEXT_VISIBLE = 0
    }
}
