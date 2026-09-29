package com.xuexiang.xupdate.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * 弹窗工具类
 *
 * @author xuexiang
 * @since 2021/11/16 1:19 PM
 */
object DialogUtils {

    /**
     * 显示窗口【同步窗口系统view的可见度, 解决全屏下显示窗口导致界面退出全屏的问题】
     *
     * @param activity      活动窗口
     * @param window        需要显示的窗口
     * @param iWindowShower 窗口显示接口
     * @return 是否执行成功
     */
    @JvmStatic
    fun showWindow(activity: Activity?, window: Window?, iWindowShower: IWindowShower?): Boolean {
        if (activity == null || window == null || iWindowShower == null) {
            return false
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        iWindowShower.show(window)
        syncSystemUiVisibility(activity, window)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        return true
    }

    /**
     * 同步窗口的系统view的可见度【解决全屏下显示窗口导致界面退出全屏的问题】
     *
     * @param original 活动窗口
     * @param target   目标窗口
     * @return 是否执行成功
     */
    @JvmStatic
    fun syncSystemUiVisibility(original: Activity?, target: Window?): Boolean {
        if (original == null) {
            return false
        }
        return syncSystemUiVisibility(original.window, target)
    }

    /**
     * 同步两个窗口的系统view的可见度【解决全屏下显示窗口导致界面退出全屏的问题】
     *
     * @param original 原始窗口
     * @param target   目标窗口
     * @return 是否执行成功
     */
    @JvmStatic
    fun syncSystemUiVisibility(original: Window?, target: Window?): Boolean {
        if (original == null || target == null) {
            return false
        }
        @Suppress("DEPRECATION")
        target.decorView.systemUiVisibility = original.decorView.systemUiVisibility
        return true
    }

    /**
     * 窗口显示接口
     */
    fun interface IWindowShower {
        /**
         * 显示窗口
         *
         * @param window 窗口
         */
        fun show(window: Window?)
    }

    /**
     * 根据上下文获取Activity
     *
     * @param context 上下文
     * @return Activity
     */
    @JvmStatic
    fun findActivity(context: Context?): Activity? {
        if (context is Activity) {
            return context
        }
        if (context is ContextWrapper) {
            return findActivity(context.baseContext)
        }
        return null
    }

    /**
     * 根据用户点击的坐标获取用户在窗口上触摸到的View，判断这个View是否是EditText来判断是否需要隐藏键盘
     *
     * @param window 窗口
     * @param event  用户点击事件
     * @return 是否需要隐藏键盘
     */
    @JvmStatic
    fun isShouldHideInput(window: Window?, event: android.view.MotionEvent?): Boolean {
        if (window == null || event == null) {
            return false
        }
        if (!isSoftInputShow(window)) {
            return false
        }
        if (window.currentFocus !is EditText) {
            return false
        }
        val decorView = window.decorView
        return if (decorView is ViewGroup) {
            findTouchEditText(decorView, event) == null
        } else {
            false
        }
    }

    private fun findTouchEditText(viewGroup: ViewGroup?, event: android.view.MotionEvent): View? {
        if (viewGroup == null) {
            return null
        }
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child == null || !child.isShown) {
                continue
            }
            if (!isTouchView(child, event)) {
                continue
            }
            if (child is EditText) {
                return child
            } else if (child is ViewGroup) {
                return findTouchEditText(child, event)
            }
        }
        return null
    }

    /**
     * 判断view是否在触摸区域内
     *
     * @param view  view
     * @param event 点击事件
     * @return view是否在触摸区域内
     */
    private fun isTouchView(view: View?, event: android.view.MotionEvent?): Boolean {
        if (view == null || event == null) {
            return false
        }
        val rect = Rect()
        view.getGlobalVisibleRect(rect)
        return rect.contains(event.x.toInt(), event.y.toInt())
    }

    /**
     * 输入键盘是否在显示
     *
     * @param window 应用窗口
     */
    private fun isSoftInputShow(window: Window?): Boolean {
        if (window != null && window.decorView is ViewGroup) {
            return isSoftInputShow(window.decorView as ViewGroup)
        }
        return false
    }

    /**
     * 输入键盘是否在显示
     *
     * @param rootView 根布局
     */
    private fun isSoftInputShow(rootView: ViewGroup?): Boolean {
        if (rootView == null) {
            return false
        }
        val viewHeight = rootView.height
        //获取View可见区域的bottom
        val rect = Rect()
        rootView.getWindowVisibleDisplayFrame(rect)
        val space = viewHeight - rect.bottom - getNavigationBarHeight(rootView.context)
        return space > 0
    }

    /**
     * 获取系统底部导航栏的高度
     *
     * @param context 上下文
     * @return 系统状态栏的高度
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun getNavigationBarHeight(context: Context): Int {
        val windowManager: WindowManager = if (context is Activity) {
            context.windowManager
        } else {
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
        val defaultDisplay = windowManager.defaultDisplay ?: return 0

        val realDisplayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            defaultDisplay.getRealMetrics(realDisplayMetrics)
        }
        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        defaultDisplay.getMetrics(displayMetrics)

        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

        if (realHeight - displayHeight > 0) {
            return realHeight - displayHeight
        }
        return maxOf(realWidth - displayWidth, 0)
    }

    /**
     * 动态隐藏软键盘
     *
     * @param view 视图
     */
    @JvmStatic
    fun hideSoftInput(view: View?) {
        if (view == null) {
            return
        }
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
