package com.xuexiang.xupdate.utils

import android.text.TextUtils
import android.util.Log

/**
 * 更新日志打印
 *
 * @author xuexiang
 * @since 2018/6/29 下午7:57
 */
object UpdateLog {

    //==============常量================//

    /**
     * 默认tag
     */
    const val DEFAULT_LOG_TAG = "[XUpdate]"

    /**
     * 最大日志优先级【日志优先级为最大等级，所有日志都不打印】
     */
    private const val MAX_LOG_PRIORITY = 10

    /**
     * 最小日志优先级【日志优先级为最小等级，所有日志都打印】
     */
    private const val MIN_LOG_PRIORITY = 0

    //==============属性================//

    /**
     * 默认的日志记录为Logcat
     */
    private var sILogger: ILogger? = LogcatLogger()

    private var sTag: String = DEFAULT_LOG_TAG

    /**
     * 是否是调试模式
     */
    private var sIsDebug = false

    /**
     * 日志打印优先级
     */
    private var sLogPriority = MAX_LOG_PRIORITY

    //==============属性设置================//

    /**
     * 设置日志记录者的接口
     */
    @JvmStatic
    fun setLogger(logger: ILogger) {
        sILogger = logger
    }

    /**
     * 设置日志的tag
     */
    @JvmStatic
    fun setTag(tag: String) {
        sTag = tag
    }

    /**
     * 设置是否是调试模式
     */
    @JvmStatic
    fun setDebug(isDebug: Boolean) {
        sIsDebug = isDebug
    }

    /**
     * 设置打印日志的等级（只打印改等级以上的日志）
     */
    @JvmStatic
    fun setPriority(priority: Int) {
        sLogPriority = priority
    }

    //===================对外接口=======================//

    /**
     * 设置是否打开调试
     */
    @JvmStatic
    fun debug(isDebug: Boolean) {
        if (isDebug) {
            debug(DEFAULT_LOG_TAG)
        } else {
            debug("")
        }
    }

    /**
     * 设置调试模式
     */
    @JvmStatic
    fun debug(tag: String) {
        if (!TextUtils.isEmpty(tag)) {
            setDebug(true)
            setPriority(MIN_LOG_PRIORITY)
            setTag(tag)
        } else {
            setDebug(false)
            setPriority(MAX_LOG_PRIORITY)
            setTag("")
        }
    }

    //=============打印方法===============//

    /**
     * 打印任何（所有）信息
     */
    @JvmStatic
    fun v(msg: String) {
        if (enableLog(Log.VERBOSE)) {
            sILogger?.log(Log.VERBOSE, sTag, msg, null)
        }
    }

    /**
     * 打印任何（所有）信息
     */
    @JvmStatic
    fun vTag(tag: String, msg: String) {
        if (enableLog(Log.VERBOSE)) {
            sILogger?.log(Log.VERBOSE, tag, msg, null)
        }
    }

    /**
     * 打印调试信息
     */
    @JvmStatic
    fun d(msg: String) {
        if (enableLog(Log.DEBUG)) {
            sILogger?.log(Log.DEBUG, sTag, msg, null)
        }
    }

    /**
     * 打印调试信息
     */
    @JvmStatic
    fun dTag(tag: String, msg: String) {
        if (enableLog(Log.DEBUG)) {
            sILogger?.log(Log.DEBUG, tag, msg, null)
        }
    }

    /**
     * 打印提示性的信息
     */
    @JvmStatic
    fun i(msg: String) {
        if (enableLog(Log.INFO)) {
            sILogger?.log(Log.INFO, sTag, msg, null)
        }
    }

    /**
     * 打印提示性的信息
     */
    @JvmStatic
    fun iTag(tag: String, msg: String) {
        if (enableLog(Log.INFO)) {
            sILogger?.log(Log.INFO, tag, msg, null)
        }
    }

    /**
     * 打印warning警告信息
     */
    @JvmStatic
    fun w(msg: String) {
        if (enableLog(Log.WARN)) {
            sILogger?.log(Log.WARN, sTag, msg, null)
        }
    }

    /**
     * 打印warning警告信息
     */
    @JvmStatic
    fun wTag(tag: String, msg: String) {
        if (enableLog(Log.WARN)) {
            sILogger?.log(Log.WARN, tag, msg, null)
        }
    }

    /**
     * 打印出错信息
     */
    @JvmStatic
    fun e(msg: String) {
        if (enableLog(Log.ERROR)) {
            sILogger?.log(Log.ERROR, sTag, msg, null)
        }
    }

    /**
     * 打印出错信息
     */
    @JvmStatic
    fun eTag(tag: String, msg: String) {
        if (enableLog(Log.ERROR)) {
            sILogger?.log(Log.ERROR, tag, msg, null)
        }
    }

    /**
     * 打印出错堆栈信息
     */
    @JvmStatic
    fun e(t: Throwable) {
        if (enableLog(Log.ERROR)) {
            sILogger?.log(Log.ERROR, sTag, null, t)
        }
    }

    /**
     * 打印出错堆栈信息
     */
    @JvmStatic
    fun eTag(tag: String, t: Throwable) {
        if (enableLog(Log.ERROR)) {
            sILogger?.log(Log.ERROR, tag, null, t)
        }
    }

    /**
     * 打印出错堆栈信息
     */
    @JvmStatic
    fun e(msg: String, t: Throwable) {
        if (enableLog(Log.ERROR)) {
            sILogger?.log(Log.ERROR, sTag, msg, t)
        }
    }

    /**
     * 打印出错堆栈信息
     */
    @JvmStatic
    fun eTag(tag: String, msg: String, t: Throwable) {
        if (enableLog(Log.ERROR)) {
            sILogger?.log(Log.ERROR, tag, msg, t)
        }
    }

    /**
     * 打印严重的错误信息
     */
    @JvmStatic
    fun wtf(msg: String) {
        if (enableLog(Log.ASSERT)) {
            sILogger?.log(Log.ASSERT, sTag, msg, null)
        }
    }

    /**
     * 打印严重的错误信息
     */
    @JvmStatic
    fun wtfTag(tag: String, msg: String) {
        if (enableLog(Log.ASSERT)) {
            sILogger?.log(Log.ASSERT, tag, msg, null)
        }
    }

    /**
     * 能否打印
     */
    private fun enableLog(logPriority: Int): Boolean {
        return sILogger != null && sIsDebug && logPriority >= sLogPriority
    }
}
