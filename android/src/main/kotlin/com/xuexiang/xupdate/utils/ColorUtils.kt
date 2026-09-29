package com.xuexiang.xupdate.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import kotlin.random.Random

/**
 * 颜色工具类
 *
 * @author xuexiang
 * @since 2018/7/2 下午3:12
 */
object ColorUtils {

    /**
     * 颜色选择器
     *
     * @param pressedColor 按下的颜色
     * @param normalColor  正常的颜色
     * @return 颜色选择器
     */
    @JvmStatic
    fun getColorStateList(pressedColor: Int, normalColor: Int): ColorStateList {
        //其他状态默认为白色
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed),
                intArrayOf(android.R.attr.state_enabled),
                intArrayOf()
            ),
            intArrayOf(pressedColor, normalColor, Color.WHITE)
        )
    }

    /**
     * 加深颜色
     *
     * @param color 原色
     * @return 加深后的
     */
    @JvmStatic
    fun colorDeep(color: Int): Int {
        val alpha = Color.alpha(color)
        var red = Color.red(color)
        var green = Color.green(color)
        var blue = Color.blue(color)
        val ratio = 0.8f
        red = (red * ratio).toInt()
        green = (green * ratio).toInt()
        blue = (blue * ratio).toInt()
        return Color.argb(alpha, red, green, blue)
    }

    /**
     * 是否是深色的颜色
     */
    @JvmStatic
    fun isColorDark(@ColorInt color: Int): Boolean {
        val darkness = 1 -
                (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    /**
     * 按条件的到随机颜色
     *
     * @param alpha 透明
     * @param lower 下边界
     * @param upper 上边界
     * @return 颜色值
     */
    @JvmStatic
    fun getRandomColor(alpha: Int, lower: Int, upper: Int): Int {
        return RandomColor(alpha, lower, upper).color
    }

    /**
     * @return 获取随机色
     */
    @JvmStatic
    fun getRandomColor(): Int {
        return RandomColor(255, 80, 200).color
    }

    /**
     * 获取Color值
     */
    @JvmStatic
    fun getColor(context: Context?, @ColorRes resId: Int): Int {
        return context!!.resources.getColor(resId)
    }

    /**
     * 随机颜色
     */
    class RandomColor(alpha: Int, lower: Int, upper: Int) {

        var alpha = 0
            private set
        var lower = 0
            private set
        var upper = 0
            private set

        init {
            require(upper > lower) { "must be lower < upper" }
            setAlpha(alpha)
            setLower(lower)
            setUpper(upper)
        }

        val color: Int
            get() {
                //随机数是前闭  后开
                val red = lower + Random.nextInt(upper - lower + 1)
                val green = lower + Random.nextInt(upper - lower + 1)
                val blue = lower + Random.nextInt(upper - lower + 1)
                return Color.argb(alpha, red, green, blue)
            }

        fun setAlpha(alpha: Int) {
            this.alpha = alpha.coerceIn(0, 255)
        }

        fun setLower(lower: Int) {
            this.lower = if (lower < 0) 0 else lower
        }

        fun setUpper(upper: Int) {
            this.upper = if (upper > 255) 255 else upper
        }
    }
}
