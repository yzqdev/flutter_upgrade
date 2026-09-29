package com.xuexiang.xupdate.service

import java.io.File

/**
 * 下载服务下载监听
 *
 * @author xuexiang
 * @since 2018/7/10 上午10:05
 */
interface OnFileDownloadListener {

    /**
     * 下载之前
     */
    fun onStart()

    /**
     * 更新进度
     *
     * @param progress 进度0.00 - 0.50  - 1.00
     * @param total    文件总大小 单位字节
     */
    fun onProgress(progress: Float, total: Long)

    /**
     * 下载完毕
     *
     * @param file 下载好的文件
     * @return 下载完毕后是否打开文件进行安装<br></br>{@code true} ：安装<br></br>{@code false} ：不安装
     */
    fun onCompleted(file: File?): Boolean

    /**
     * 错误回调
     *
     * @param throwable 错误提示
     */
    fun onError(throwable: Throwable)
}
