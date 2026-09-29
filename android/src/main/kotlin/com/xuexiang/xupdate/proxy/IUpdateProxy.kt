package com.xuexiang.xupdate.proxy

import android.content.Context
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.service.OnFileDownloadListener

/**
 * 版本更新代理
 *
 * @author xuexiang
 * @since 2018/7/1 下午9:45
 */
interface IUpdateProxy {

    /**
     * 获取上下文
     */
    fun getContext(): Context?

    /**
     * 获取版本更新的地址
     */
    fun getUrl(): String

    /**
     * 获取版本更新网络请求服务API
     */
    fun getIUpdateHttpService(): IUpdateHttpService?

    /**
     * 开始版本更新
     */
    fun update()

    //============ICheckerProxy=================//

    /**
     * 版本检查之前
     */
    fun onBeforeCheck()

    /**
     * 执行网络请求，检查应用的版本信息
     */
    fun checkVersion()

    /**
     * 版本检查之后
     */
    fun onAfterCheck()

    /**
     * 发现新版本
     *
     * @param updateEntity 版本更新信息
     * @param updateProxy  版本更新代理
     */
    fun findNewVersion(updateEntity: UpdateEntity, updateProxy: IUpdateProxy)

    /**
     * 未发现新版本
     *
     * @param throwable 未发现的原因
     */
    fun noNewVersion(throwable: Throwable?)

    //=============IParserProxy================//

    /**
     * 是否是异步解析者
     */
    fun isAsyncParser(): Boolean

    /**
     * 将请求的json结果解析为版本更新信息实体【同步方法】
     */
    @Throws(Exception::class)
    fun parseJson(json: String): UpdateEntity?

    /**
     * 将请求的json结果解析为版本更新信息实体【异步方法】
     */
    @Throws(Exception::class)
    fun parseJson(json: String, callback: IUpdateParseCallback)

    //=============IPrompterProxy================//

    /**
     * 开始下载更新
     *
     * @param updateEntity     更新信息
     * @param downloadListener 文件下载监听
     */
    fun startDownload(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?)

    /**
     * 后台下载
     */
    fun backgroundDownload()

    /**
     * 取消下载
     */
    fun cancelDownload()

    /**
     * 资源回收
     */
    fun recycle()
}
