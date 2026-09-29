package com.xuexiang.xupdate.proxy.impl

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.text.TextUtils
import java.io.IOException
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.proxy.IUpdateDownloader
import com.xuexiang.xupdate.service.DownloadService
import com.xuexiang.xupdate.service.OnFileDownloadListener
import com.xuexiang.xupdate.utils.UpdateUtils

/**
 * 默认版本更新下载器
 *
 * @author xuexiang
 * @since 2018/7/5 下午5:06
 */
open class DefaultUpdateDownloader : IUpdateDownloader {

    private var mDownloadBinder: DownloadService.DownloadBinder? = null

    /**
     * 服务绑定连接
     */
    private var mServiceConnection: ServiceConnection? = null

    /**
     * 是否已绑定下载服务
     */
    private var mIsBound = false

    override fun startDownload(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
        if (isDownloadUrl(updateEntity)) {
            startDownloadService(updateEntity, downloadListener)
        } else {
            startOpenHtml(updateEntity, downloadListener)
        }
    }

    /**
     * 地址是否是下载地址，需要开启下载服务进行下载【可以根据自己的逻辑进行重写】
     *
     * @param updateEntity 版本更新信息
     * @return 地址是否是下载地址
     */
    protected open fun isDownloadUrl(updateEntity: UpdateEntity): Boolean {
        return isApkDownloadUrl(updateEntity) || !isStaticHtmlUrl(updateEntity)
    }

    /**
     * 地址是否是apk的下载地址
     *
     * @param updateEntity 版本更新信息
     * @return 地址是否是apk的下载地址
     */
    protected open fun isApkDownloadUrl(updateEntity: UpdateEntity): Boolean {
        val downloadUrl = updateEntity.downloadUrl
        return !TextUtils.isEmpty(downloadUrl) &&
                downloadUrl!!.substring(downloadUrl.lastIndexOf("/") + 1).endsWith(".apk")
    }

    /**
     * 地址是否是静态网页
     *
     * @param updateEntity 版本更新信息
     * @return 地址是否是静态网页
     */
    protected open fun isStaticHtmlUrl(updateEntity: UpdateEntity): Boolean {
        val downloadUrl = updateEntity.downloadUrl ?: return false
        val urlContent = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1)
        return urlContent.contains(".htm") || urlContent.contains(".shtm")
    }

    /**
     * 开启下载服务
     *
     * @param updateEntity     版本更新信息
     * @param downloadListener 下载监听
     */
    protected open fun startDownloadService(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
        mServiceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mIsBound = true
                startDownload(service as DownloadService.DownloadBinder, updateEntity, downloadListener)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                mIsBound = false
            }
        }
        DownloadService.bindService(mServiceConnection!!)
    }

    /**
     * 使用系统的api打开网页
     *
     * @param updateEntity     版本更新信息
     * @param downloadListener 监听回调
     */
    protected open fun startOpenHtml(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateEntity.downloadUrl))
        val result = UpdateUtils.startActivity(intent)
        if (downloadListener != null) {
            if (result) {
                // 强制更新的话，不能关闭更新弹窗
                if (!updateEntity.isForce) {
                    downloadListener.onCompleted(null)
                }
            } else {
                downloadListener.onError(IOException("Failed to open web page: " + updateEntity.downloadUrl))
            }
        }
    }

    /**
     * 开始下载
     *
     * @param binder           下载服务绑定
     * @param updateEntity     版本更新信息
     * @param downloadListener 下载监听
     */
    private fun startDownload(binder: DownloadService.DownloadBinder, updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
        mDownloadBinder = binder
        mDownloadBinder!!.start(updateEntity, downloadListener)
    }

    override fun cancelDownload() {
        mDownloadBinder?.stop("取消下载")
        if (mIsBound && mServiceConnection != null) {
            XUpdate.context.unbindService(mServiceConnection!!)
            mIsBound = false
        }
    }

    /**
     * 后台下载更新
     */
    override fun backgroundDownload() {
        mDownloadBinder?.showNotification()
    }
}
