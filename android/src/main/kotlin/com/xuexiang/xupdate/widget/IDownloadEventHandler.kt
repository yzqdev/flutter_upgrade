package com.xuexiang.xupdate.widget

import java.io.File

/**
 * 下载事件处理者
 *
 * @author xuexiang
 * @since 2020/12/23 10:47 PM
 */
interface IDownloadEventHandler {

    /**
     * 处理开始下载
     */
    fun handleStart()

    /**
     * 处理下载中的进度更新
     *
     * @param progress 下载进度
     */
    fun handleProgress(progress: Float)

    /**
     * 处理下载完毕
     *
     * @param file 下载文件
     * @return 下载完毕后是否打开文件进行安装<br></br>{@code true} ：安装<br></br>{@code false} ：不安装
     */
    fun handleCompleted(file: File?): Boolean

    /**
     * 处理下载失败
     *
     * @param throwable 失败原因
     */
    fun handleError(throwable: Throwable)
}
