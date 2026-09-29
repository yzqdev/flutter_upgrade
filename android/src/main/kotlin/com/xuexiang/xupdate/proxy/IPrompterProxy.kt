package com.xuexiang.xupdate.proxy

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.service.OnFileDownloadListener

/**
 * 版本更新提示器代理
 *
 * @author xuexiang
 * @since 2020/6/9 12:16 AM
 */
interface IPrompterProxy {

    /**
     * 获取版本更新的地址
     *
     * @return 版本更新的地址
     */
    fun getUrl(): String

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
