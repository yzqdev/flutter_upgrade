package com.xuexiang.xupdate.listener.impl

import android.content.Context
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.DownloadEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.INSTALL_FAILED
import com.xuexiang.xupdate.listener.OnInstallListener
import com.xuexiang.xupdate.utils.ApkInstallUtils
import java.io.File
import java.io.IOException

/**
 * 默认的apk安装监听【自定义安装监听可继承该类，并重写相应的方法】
 *
 * @author xuexiang
 * @since 2018/7/1 下午11:58
 */
open class DefaultInstallListener : OnInstallListener {

    override fun onInstallApk(context: Context, apkFile: File, downloadEntity: DownloadEntity): Boolean {
        return if (checkApkFile(downloadEntity, apkFile)) {
            installApkFile(context, apkFile)
        } else {
            XupdateTool.onUpdateError(INSTALL_FAILED, "Apk file verify failed, please check whether the MD5 value you set is correct！")
            false
        }
    }

    /**
     * 检验apk文件的有效性（默认是使用MD5进行校验,可重写该方法）
     *
     * @param downloadEntity 下载信息实体
     * @param apkFile        apk文件
     * @return apk文件是否有效
     */
    protected fun checkApkFile(downloadEntity: DownloadEntity?, apkFile: File): Boolean {
        return downloadEntity != null && downloadEntity.isApkFileValid(apkFile)
    }

    /**
     * 安装apk文件【此处可自定义apk的安装方法,可重写该方法】
     *
     * @param context 上下文
     * @param apkFile apk文件
     * @return 是否安装成功
     */
    protected fun installApkFile(context: Context, apkFile: File): Boolean {
        return try {
            ApkInstallUtils.install(context, apkFile)
        } catch (e: IOException) {
            XupdateTool.onUpdateError(INSTALL_FAILED, "An error occurred while install apk:" + e.message)
            false
        }
    }

    override fun onInstallApkSuccess() {
    }
}
