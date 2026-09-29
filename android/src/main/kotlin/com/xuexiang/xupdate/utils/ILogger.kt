package com.xuexiang.xupdate.utils

/**
 * 日志打印接口
 *
 * @author xuexiang
 * @since 2018/6/29 下午7:55
 */
fun interface ILogger {

    /**
     * 打印信息
     *
     * @param priority 优先级
     * @param tag      标签
     * @param message  信息
     * @param t        出错信息
     */
    fun log(priority: Int, tag: String?, message: String?, t: Throwable?)
}
