package com.xuexiang.xupdate.proxy

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.service.OnFileDownloadListener

/**
 * 版本更新下载者
 *
 * @author xuexiang
 * @since 2018/7/5 下午5:05
 */
interface IUpdateDownloader {

    /**
     * 开始下载更新文件
     *
     * @param updateEntity     更新信息
     * @param downloadListener 文件下载监听
     */
    fun startDownload(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?)

    /**
     * 后台下载更新
     */
    fun backgroundDownload()

    /**
     * 取消下载
     */
    fun cancelDownload()
}
