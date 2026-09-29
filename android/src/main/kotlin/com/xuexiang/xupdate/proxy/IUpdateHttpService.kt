package com.xuexiang.xupdate.proxy

import java.io.File

/**
 * 版本更新网络请求服务API
 *
 * @author xuexiang
 * @since 2018/6/29 下午8:44
 */
interface IUpdateHttpService {

    /**
     * 异步get
     *
     * @param url      get请求地址
     * @param params   get参数
     * @param callBack 回调
     */
    fun asyncGet(url: String, params: Map<String, Any>, callBack: Callback)

    /**
     * 异步post
     *
     * @param url      post请求地址
     * @param params   post请求参数
     * @param callBack 回调
     */
    fun asyncPost(url: String, params: Map<String, Any>, callBack: Callback)

    /**
     * 文件下载
     *
     * @param url      下载地址
     * @param path     文件保存路径
     * @param fileName 文件名称
     * @param callback 文件下载回调
     */
    fun download(url: String, path: String, fileName: String, callback: DownloadCallback)

    /**
     * 取消文件下载
     *
     * @param url 下载地址
     */
    fun cancelDownload(url: String)

    /**
     * 网络请求回调
     */
    interface Callback {
        /**
         * 结果回调
         *
         * @param result 结果
         */
        fun onSuccess(result: String)

        /**
         * 错误回调
         *
         * @param throwable 错误提示
         */
        fun onError(throwable: Throwable)
    }

    /**
     * 下载回调
     */
    interface DownloadCallback {
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
         * 结果回调
         *
         * @param file 下载好的文件
         */
        fun onSuccess(file: File)

        /**
         * 错误回调
         *
         * @param throwable 错误提示
         */
        fun onError(throwable: Throwable)
    }
}
