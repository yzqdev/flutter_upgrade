package com.xuexiang.xupdate.widget

import com.xuexiang.xupdate.service.OnFileDownloadListener
import java.io.File
import java.lang.ref.WeakReference

/**
 * 弱引用文件下载监听, 解决内存泄漏问题
 *
 * @author xuexiang
 * @since 2020/11/15 10:58 PM
 */
class WeakFileDownloadListener(handler: IDownloadEventHandler) :
    OnFileDownloadListener {

    private val mDownloadHandlerRef = WeakReference(handler)

    override fun onStart() {
        eventHandler?.handleStart()
    }

    override fun onProgress(progress: Float, total: Long) {
        eventHandler?.handleProgress(progress)
    }

    override fun onCompleted(file: File?): Boolean {
        val handler = eventHandler ?: return true
        return handler.handleCompleted(file)
    }

    override fun onError(throwable: Throwable) {
        eventHandler?.handleError(throwable)
    }

    private val eventHandler: IDownloadEventHandler?
        get() = mDownloadHandlerRef.get()
}
