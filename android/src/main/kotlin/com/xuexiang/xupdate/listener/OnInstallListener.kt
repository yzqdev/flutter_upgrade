package com.xuexiang.xupdate.listener

import android.content.Context
import com.xuexiang.xupdate.entity.DownloadEntity
import java.io.File

/**
 * 安装监听
 *
 * @author xuexiang
 * @since 2018/6/29 下午4:14
 */
interface OnInstallListener {

    /**
     * 开始安装apk的监听
     *
     * @param apkFile        安装的apk文件
     * @param downloadEntity 文件下载信息
     */
    fun onInstallApk(context: Context, apkFile: File, downloadEntity: DownloadEntity): Boolean

    /**
     * apk安装完毕
     */
    fun onInstallApkSuccess()
}
