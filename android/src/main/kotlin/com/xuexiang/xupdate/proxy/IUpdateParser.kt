package com.xuexiang.xupdate.proxy

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.listener.IUpdateParseCallback

/**
 * 版本更新解析器[异步解析和同步解析方法只需要实现一个就行了，当isAsyncParser为true时需要实现异步解析方法，否则实现同步解析方法]
 *
 * @author xuexiang
 * @since 2018/6/29 下午8:30
 */
interface IUpdateParser {

    /**
     * [同步解析方法]
     * 将请求的json结果解析为版本更新信息实体
     */
    @Throws(Exception::class)
    fun parseJson(json: String): UpdateEntity?

    /**
     * [异步解析方法]
     * 将请求的json结果解析为版本更新信息实体
     */
    @Throws(Exception::class)
    fun parseJson(json: String, callback: IUpdateParseCallback)

    /**
     * @return 是否是异步解析
     */
    fun isAsyncParser(): Boolean
}
