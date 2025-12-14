package com.xuexiang.flutter_xupdate

import com.hjq.http.annotation.HttpIgnore
import com.hjq.http.config.IRequestApi
import com.hjq.http.config.IRequestServer

class EasyRequestUrl(
    /** 主机地址  */
    @field:HttpIgnore private val mHost: String,
    /** 接口地址  */
    @field:HttpIgnore private val mApi: String
) : IRequestServer, IRequestApi {
    constructor(url: String) : this(url, "")

    override fun getHost(): String {
        return mHost
    }

    override fun getApi(): String {
        return mApi
    }

    override fun toString(): String {
        return mHost + mApi
    }
}