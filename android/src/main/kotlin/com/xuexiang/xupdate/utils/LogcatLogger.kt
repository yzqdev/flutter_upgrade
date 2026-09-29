package com.xuexiang.xupdate.utils

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Logcat日志打印
 *
 * @author xuexiang
 * @since 2018/6/29 下午7:57
 */
open class LogcatLogger : ILogger {

    /**
     * 打印信息
     *
     * @param priority 优先级
     * @param tag      标签
     * @param message  信息
     * @param t        出错信息
     */
    override fun log(priority: Int, tag: String?, message: String?, t: Throwable?) {
        var msg = message
        if (msg != null && msg.isEmpty()) {
            msg = null
        }
        if (msg == null) {
            if (t == null) {
                return  // Swallow message if it's null and there's no throwable.
            }
            msg = getStackTraceString(t)
        } else {
            if (t != null) {
                msg += "\n" + getStackTraceString(t)
            }
        }
        log(priority, tag, msg)
    }

    private fun getStackTraceString(t: Throwable): String {
        // Don't replace this with Log.getStackTraceString() - it hides
        // UnknownHostException, which is not what we want.
        val sw = StringWriter(256)
        val pw = PrintWriter(sw, false)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    /**
     * 使用LogCat输出日志，字符长度超过4000则自动换行.
     *
     * @param priority 优先级
     * @param tag      标签
     * @param message  信息
     */
    private fun log(priority: Int, tag: String?, message: String) {
        val subNum = message.length / MAX_LOG_LENGTH
        if (subNum > 0) {
            var index = 0
            for (i in 0 until subNum) {
                val lastIndex = index + MAX_LOG_LENGTH
                val sub = message.substring(index, lastIndex)
                logSub(priority, tag ?: "", sub)
                index = lastIndex
            }
            logSub(priority, tag ?: "", message.substring(index, message.length))
        } else {
            logSub(priority, tag ?: "", message)
        }
    }

    /**
     * 使用LogCat输出日志.
     *
     * @param priority 优先级
     * @param tag      标签
     * @param sub      信息
     */
    private fun logSub(priority: Int, tag: String, sub: String) {
        when (priority) {
            Log.VERBOSE -> Log.v(tag, sub)
            Log.DEBUG -> Log.d(tag, sub)
            Log.INFO -> Log.i(tag, sub)
            Log.WARN -> Log.w(tag, sub)
            Log.ERROR -> Log.e(tag, sub)
            Log.ASSERT -> Log.wtf(tag, sub)
            else -> Log.v(tag, sub)
        }
    }

    companion object {
        /**
         * logcat里日志的最大长度.
         */
        private const val MAX_LOG_LENGTH = 4000
    }
}
