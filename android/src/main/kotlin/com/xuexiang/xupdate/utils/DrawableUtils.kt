package com.xuexiang.xupdate.utils

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.View
import android.widget.TextView

/**
 * Drawable工具类
 *
 * @author xuexiang
 * @since 2018/7/2 下午3:12
 */
object DrawableUtils {

    /**
     * 得到实心的drawable, 一般作为选中，点中的效果
     *
     * @param cornerRadius 圆角半径
     * @param solidColor   实心颜色
     * @return 得到实心效果
     */
    @JvmStatic
    fun getSolidRectDrawable(cornerRadius: Int, solidColor: Int): GradientDrawable {
        val gradientDrawable = GradientDrawable()
        // 设置矩形的圆角半径
        gradientDrawable.cornerRadius = cornerRadius.toFloat()
        // 设置绘画图片色值
        gradientDrawable.setColor(solidColor)
        // 绘画的是矩形
        gradientDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT)
        return gradientDrawable
    }

    /**
     * 得到空心的效果，一般作为默认的效果
     *
     * @param cornerRadius 圆角半径
     * @param solidColor   实心颜色
     * @param strokeColor  边框颜色
     * @param strokeWidth  边框宽度
     * @return 得到空心效果
     */
    @JvmStatic
    fun getStrokeRectDrawable(cornerRadius: Int, solidColor: Int, strokeColor: Int, strokeWidth: Int): GradientDrawable {
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setStroke(strokeWidth, strokeColor)
        gradientDrawable.setColor(solidColor)
        gradientDrawable.cornerRadius = cornerRadius.toFloat()
        gradientDrawable.setGradientType(GradientDrawable.RADIAL_GRADIENT)
        return gradientDrawable
    }

    /**
     * 背景选择器
     *
     * @param pressedDrawable 按下状态的Drawable
     * @param normalDrawable  正常状态的Drawable
     * @return 状态选择器
     */
    @JvmStatic
    fun getStateListDrawable(pressedDrawable: Drawable?, normalDrawable: Drawable?): StateListDrawable {
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), pressedDrawable)
        stateListDrawable.addState(intArrayOf(android.R.attr.state_enabled), normalDrawable)
        //设置不能用的状态
        //默认其他状态背景
        val gray = getSolidRectDrawable(10, android.graphics.Color.GRAY)
        stateListDrawable.addState(intArrayOf(), gray)
        return stateListDrawable
    }

    /**
     * 实体  状态选择器
     *
     * @param cornerRadius 圆角半径
     * @param pressedColor 按下颜色
     * @param normalColor  正常的颜色
     * @return 状态选择器
     */
    @JvmStatic
    fun getDrawable(cornerRadius: Int, pressedColor: Int, normalColor: Int): StateListDrawable {
        return getStateListDrawable(
            getSolidRectDrawable(cornerRadius, pressedColor),
            getSolidRectDrawable(cornerRadius, normalColor)
        )
    }

    /**
     * 得到 正常空心， 按下实体的状态选择器
     *
     * @param cornerRadiusPX 圆角半径
     * @param strokeWidthPX  边框宽度
     * @param subColor       表框颜色
     * @param mainColor      实心颜色
     * @return 状态选择器
     */
    @JvmStatic
    fun getStrokeSolidDrawable(cornerRadiusPX: Int, strokeWidthPX: Int, subColor: Int, mainColor: Int): StateListDrawable {
        //一般solidColor 为透明
        return getStateListDrawable(
            //实心
            getSolidRectDrawable(cornerRadiusPX, subColor),
            //空心
            getStrokeRectDrawable(cornerRadiusPX, mainColor, subColor, strokeWidthPX)
        )
    }

    /**
     * 得到 正常空心， 按下实体的状态选择器
     *
     * @param cornerRadiusPX 圆角半径
     * @param strokeWidthPX  边框宽度
     * @param subColor       表框颜色
     * @param mainColor      实心颜色
     * @return 状态选择器
     */
    @JvmStatic
    fun getSolidStrokeDrawable(cornerRadiusPX: Int, strokeWidthPX: Int, subColor: Int, mainColor: Int): StateListDrawable {
        //一般solidColor 为透明
        return getStateListDrawable(
            //空心
            getStrokeRectDrawable(cornerRadiusPX, subColor, mainColor, strokeWidthPX),
            //实心
            getSolidRectDrawable(cornerRadiusPX, mainColor)
        )
    }

    /**
     * 实体 按下的颜色加深
     *
     * @param cornerRadius 圆角半径
     * @param normalColor  正常的颜色
     * @return 状态选择器
     */
    @JvmStatic
    fun getDrawable(cornerRadius: Int, normalColor: Int): StateListDrawable {
        return getDrawable(cornerRadius, ColorUtils.colorDeep(normalColor), normalColor)
    }

    /**
     * 实体 得到随机色 状态选择器
     *
     * @param cornerRadius 圆角半径
     * @return 状态选择器
     */
    @JvmStatic
    fun getDrawable(cornerRadius: Int): StateListDrawable {
        return getDrawable(cornerRadius, ColorUtils.getRandomColor())
    }

    /**
     * 实体 得到随机色 状态选择器 默认10px
     *
     * @return 状态选择器
     */
    @JvmStatic
    fun getDrawable(): StateListDrawable {
        return getDrawable(10)
    }

    /**
     * 实心 得到 随机背景色并且带选择器, 并且可以设置圆角
     *
     * @param cornerRadius 圆角
     * @return 状态选择器
     */
    @JvmStatic
    fun getRandomColorDrawable(cornerRadius: Int): StateListDrawable {
        return getDrawable(cornerRadius, ColorUtils.getRandomColor(), ColorUtils.getRandomColor())
    }

    /**
     * 实心 得到随机背景色并且带选择器, 并且可以设置圆角
     *
     * @return 状态选择器
     */
    @JvmStatic
    fun getRandomColorDrawable(): StateListDrawable {
        return getRandomColorDrawable(10)
    }

    /**
     * 空心，按下实心 得到随机背景色并且带选择器, 并且可以设置圆角
     *
     * @return 状态选择器
     */
    @JvmStatic
    fun getStrokeRandomColorDrawable(): StateListDrawable {
        return getStrokeSolidDrawable(10, 4, ColorUtils.getRandomColor(), android.graphics.Color.TRANSPARENT)
    }

    /**
     * 默认空心 设置TextView 主题，
     *
     * @param textView     textView
     * @param strokeWidth  边框宽度 px
     * @param cornerRadius 圆角
     * @param color        颜色
     */
    @JvmStatic
    fun setTextStrokeTheme(textView: TextView, strokeWidth: Int, cornerRadius: Int, color: Int) {
        textView.setBackgroundDrawable(getStrokeSolidDrawable(cornerRadius, strokeWidth, color, android.graphics.Color.WHITE))
        textView.setTextColor(ColorUtils.getColorStateList(android.graphics.Color.WHITE, color))
        textView.paint.flags = Paint.FAKE_BOLD_TEXT_FLAG
    }

    /**
     * 默认空心 设置TextView 主题，随机颜色
     *
     * @param textView     textView
     * @param strokeWidth  边框宽度 px
     * @param cornerRadius 圆角
     */
    @JvmStatic
    fun setTextStrokeTheme(textView: TextView, strokeWidth: Int, cornerRadius: Int) {
        setTextStrokeTheme(textView, strokeWidth, cornerRadius, ColorUtils.getRandomColor())
    }

    /**
     * 默认空心 设置TextView 主题，随机颜色
     *
     * @param textView textView
     */
    @JvmStatic
    fun setTextStrokeTheme(textView: TextView) {
        setTextStrokeTheme(textView, 6, 10)
    }

    /**
     * 默认空心 设置TextView 主题，随机颜色
     *
     * @param textView 文本控件
     * @param color    颜色
     */
    @JvmStatic
    fun setTextStrokeTheme(textView: TextView, color: Int) {
        setTextStrokeTheme(textView, 6, 10, color)
    }

    /**
     * 默认实心 设置TextView 主题，
     *
     * @param textView     textView
     * @param strokeWidth  边框宽度 px
     * @param cornerRadius 圆角
     * @param color        颜色
     */
    @JvmStatic
    fun setTextSolidTheme(textView: TextView, strokeWidth: Int, cornerRadius: Int, color: Int) {
        textView.setBackgroundDrawable(getSolidStrokeDrawable(cornerRadius, strokeWidth, android.graphics.Color.WHITE, color))
        textView.setTextColor(ColorUtils.getColorStateList(color, android.graphics.Color.WHITE))
        textView.paint.flags = Paint.FAKE_BOLD_TEXT_FLAG
    }

    /**
     * 默认实心 设置TextView 主题，随机颜色
     *
     * @param textView     textView
     * @param strokeWidth  边框宽度 px
     * @param cornerRadius 圆角
     */
    @JvmStatic
    fun setTextSolidTheme(textView: TextView, strokeWidth: Int, cornerRadius: Int) {
        setTextSolidTheme(textView, strokeWidth, cornerRadius, ColorUtils.getRandomColor())
    }

    /**
     * 默认实心 设置TextView 主题，随机颜色
     *
     * @param textView textView
     */
    @JvmStatic
    fun setTextSolidTheme(textView: TextView) {
        setTextSolidTheme(textView, 6, 10)
    }

    /**
     * 设置控件背景
     *
     * @param view 控件
     * @param d    背景资源
     */
    @JvmStatic
    fun setBackgroundCompat(view: View, d: android.graphics.drawable.Drawable?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            @Suppress("DEPRECATION")
            view.background = d
        } else {
            view.background = d
        }
    }
}
