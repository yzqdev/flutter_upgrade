package com.xuexiang.xupdate.proxy.impl

import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.proxy.IUpdateParser

/**
 * 默认是使用同步解析器，因此异步解析方法不需要实现
 *
 * @author xuexiang
 * @since 2020-02-15 17:56
 */
abstract class AbstractUpdateParser : IUpdateParser {

    @Throws(Exception::class)
    override fun parseJson(json: String, callback: IUpdateParseCallback) {
        //当isAsyncParser为 true时调用该方法
    }

    override fun isAsyncParser(): Boolean = false
}
