package com.xuexiang.xupdate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xuexiang.flutter_xupdate.R
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.DownloadEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.DOWNLOAD_FAILED
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.utils.ApkInstallUtils
import com.xuexiang.xupdate.utils.FileUtils
import com.xuexiang.xupdate.utils.UpdateLog
import com.xuexiang.xupdate.utils.UpdateUtils
import java.io.File

/**
 * APK下载服务
 *
 * @author xuexiang
 * @since 2018/7/5 上午11:15
 */
class DownloadService : Service() {

    private var mNotificationManager: NotificationManagerCompat? = null
    private var mBuilder: NotificationCompat.Builder? = null

    //=====================绑定服务============================//

    /**
     * 停止下载服务
     */
    private fun stop(contentText: String) {
        val builder = mBuilder
        if (builder != null) {
            builder.setContentTitle(UpdateUtils.getAppName(this@DownloadService))
                .setContentText(contentText)
            val notification = builder.build()
            notification.flags = Notification.FLAG_AUTO_CANCEL
            mNotificationManager?.notify(DOWNLOAD_NOTIFY_ID, notification)
        }
        close()
    }

    /**
     * 关闭服务
     */
    private fun close() {
        sIsRunning = false
        stopSelf()
    }

    //=====================生命周期============================//

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = NotificationManagerCompat.from(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        sIsRunning = true
        return DownloadBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        sIsRunning = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        mNotificationManager = null
        mBuilder = null
        super.onDestroy()
    }

    //========================下载通知===================================//

    /**
     * 创建通知
     */
    private fun setUpNotification(downloadEntity: DownloadEntity) {
        if (!downloadEntity.isShowNotification) {
            return
        }
        initNotification()
    }

    /**
     * 初始化通知
     */
    private fun initNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            channel.enableVibration(false)
            channel.enableLights(false)
            mNotificationManager?.createNotificationChannel(channel)
        }

        mBuilder = notificationBuilder
        mNotificationManager?.notify(DOWNLOAD_NOTIFY_ID, mBuilder!!.build())
    }

    private val notificationBuilder: NotificationCompat.Builder
        get() = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.xupdate_start_download))
            .setContentText(getString(R.string.xupdate_connecting_service))
            .setSmallIcon(R.drawable.xupdate_icon_app_update)
            .setLargeIcon(UpdateUtils.drawable2Bitmap(UpdateUtils.getAppIcon(this)))
            .setOngoing(true)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())

    /**
     * DownloadBinder中定义了一些实用的方法
     *
     * @author xuexiang
     * @since 2021/1/24 1:59 AM
     */
    inner class DownloadBinder : Binder() {

        private var mFileDownloadCallBack: FileDownloadCallBack? = null

        private var mUpdateEntity: UpdateEntity? = null

        /**
         * 开始下载
         *
         * @param updateEntity     新app信息
         * @param downloadListener 下载监听
         */
        fun start(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
            //下载
            mUpdateEntity = updateEntity
            mFileDownloadCallBack = FileDownloadCallBack(updateEntity, downloadListener)
            startDownload(updateEntity, mFileDownloadCallBack!!)
        }

        /**
         * 停止下载服务
         */
        fun stop(msg: String) {
            mFileDownloadCallBack?.let {
                it.onCancel()
                mFileDownloadCallBack = null
            }
            val httpService = mUpdateEntity?.iUpdateHttpService
            if (httpService != null) {
                httpService.cancelDownload(mUpdateEntity!!.downloadUrl!!)
            } else {
                UpdateLog.e("cancelDownload failed, mUpdateEntity.getIUpdateHttpService() is null!")
            }
            this@DownloadService.stop(msg)
        }

        /**
         * 显示通知
         */
        fun showNotification() {
            if (mBuilder == null && sIsRunning) {
                initNotification()
            }
        }
    }

    /**
     * 下载模块
     */
    private fun startDownload(updateEntity: UpdateEntity, fileDownloadCallBack: FileDownloadCallBack) {
        val apkUrl = updateEntity.downloadUrl
        if (TextUtils.isEmpty(apkUrl)) {
            stop(getString(R.string.xupdate_tip_download_url_error))
            return
        }
        val apkName = UpdateUtils.getApkNameByDownloadUrl(apkUrl)

        var apkCacheDir = FileUtils.getFileByPath(updateEntity.apkCacheDir)
        if (apkCacheDir == null) {
            apkCacheDir = UpdateUtils.getDefaultDiskCacheDir()
        }
        try {
            if (!FileUtils.isFileExists(apkCacheDir)) {
                apkCacheDir!!.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val target = apkCacheDir.toString() + File.separator + updateEntity.versionName

        UpdateLog.d("开始下载更新文件, 下载地址:$apkUrl, 保存路径:$target, 文件名:$apkName")
        val httpService = updateEntity.iUpdateHttpService
        if (httpService != null) {
            httpService.download(apkUrl!!, target, apkName, fileDownloadCallBack)
        } else {
            UpdateLog.e("startDownload failed, updateEntity.getIUpdateHttpService() is null!")
        }
    }

    /**
     * 文件下载处理
     */
    private inner class FileDownloadCallBack(
        updateEntity: UpdateEntity,
        private var mOnFileDownloadListener: OnFileDownloadListener?
    ) : IUpdateHttpService.DownloadCallback {

        /**
         * 文件下载监听
         */

        /**
         * 是否下载完成后自动安装
         */
        private val mIsAutoInstall: Boolean = updateEntity.isAutoInstall

        private var mOldRate = 0

        private var mIsCancel = false

        private val mMainHandler = Handler(Looper.getMainLooper())

        private val mDownloadEntity: DownloadEntity = updateEntity.downloadEntity

        override fun onStart() {
            if (mIsCancel) {
                return
            }

            //清空通知栏状态
            mNotificationManager?.cancel(DOWNLOAD_NOTIFY_ID)
            mBuilder = null

            //初始化通知栏
            setUpNotification(mDownloadEntity)
            dispatchOnStart()
        }

        private fun dispatchOnStart() {
            if (UpdateUtils.isMainThread()) {
                mOnFileDownloadListener?.onStart()
            } else {
                mMainHandler.post {
                    mOnFileDownloadListener?.onStart()
                }
            }
        }

        override fun onProgress(progress: Float, total: Long) {
            if (mIsCancel) {
                return
            }

            val rate = Math.round(progress * 100)
            //做一下判断，防止自回调过于频繁，造成更新通知栏进度过于频繁，而出现卡顿的问题。
            if (canRefreshProgress(rate)) {
                dispatchOnProgress(progress, total)

                val builder = mBuilder
                if (builder != null) {
                    builder.setContentTitle(getString(R.string.xupdate_lab_downloading) + UpdateUtils.getAppName(this@DownloadService))
                        .setContentText(rate.toString() + "%")
                        .setProgress(100, rate, false)
                        .setWhen(System.currentTimeMillis())
                    val notification = builder.build()
                    notification.flags = Notification.FLAG_AUTO_CANCEL or Notification.FLAG_ONLY_ALERT_ONCE
                    mNotificationManager?.notify(DOWNLOAD_NOTIFY_ID, notification)
                }
                //重新赋值
                mOldRate = rate
            }
        }

        /**
         * 是否可以刷新进度
         *
         * @param newRate 最新进度
         * @return 是否可以刷新进度
         */
        private fun canRefreshProgress(newRate: Int): Boolean {
            return if (mBuilder != null) {
                // 系统通知栏对单个应用通知队列通长度进行了限制。
                // notify方法会将Notification加入系统的通知队列，当前应用发出的Notification数量超过50时，不再继续向系统的通知队列添加Notification。
                Math.abs(newRate - mOldRate) >= 4
            } else {
                Math.abs(newRate - mOldRate) >= 1
            }
        }

        private fun dispatchOnProgress(progress: Float, total: Long) {
            if (UpdateUtils.isMainThread()) {
                mOnFileDownloadListener?.onProgress(progress, total)
            } else {
                mMainHandler.post {
                    mOnFileDownloadListener?.onProgress(progress, total)
                }
            }
        }

        override fun onSuccess(file: File) {
            if (UpdateUtils.isMainThread()) {
                handleOnSuccess(file)
            } else {
                mMainHandler.post { handleOnSuccess(file) }
            }
        }

        private fun handleOnSuccess(file: File) {
            if (mIsCancel) {
                return
            }

            if (mOnFileDownloadListener != null) {
                if (!mOnFileDownloadListener!!.onCompleted(file)) {
                    close()
                    return
                }
            }
            UpdateLog.d("更新文件下载完成, 文件路径:" + file.absolutePath)
            try {
                if (UpdateUtils.isAppOnForeground(this@DownloadService)) {
                    //App前台运行
                    mNotificationManager?.cancel(DOWNLOAD_NOTIFY_ID)

                    if (mIsAutoInstall) {
                        XupdateTool.startInstallApk(this@DownloadService, file, mDownloadEntity)
                    } else {
                        showDownloadCompleteNotification(file)
                    }
                } else {
                    showDownloadCompleteNotification(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //下载完自杀
            close()
        }

        override fun onError(throwable: Throwable) {
            if (mIsCancel) {
                return
            }

            XupdateTool.onUpdateError(DOWNLOAD_FAILED, throwable.message)
            //App前台运行
            dispatchOnError(throwable)
            try {
                mNotificationManager?.cancel(DOWNLOAD_NOTIFY_ID)
                close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun dispatchOnError(throwable: Throwable) {
            if (UpdateUtils.isMainThread()) {
                mOnFileDownloadListener?.onError(throwable)
            } else {
                mMainHandler.post {
                    mOnFileDownloadListener?.onError(throwable)
                }
            }
        }

        /**
         * 取消下载
         */
        fun onCancel() {
            mOnFileDownloadListener = null
            mIsCancel = true
        }
    }

    private fun showDownloadCompleteNotification(file: File) {
        //App后台运行
        //更新参数,注意flags要使用FLAG_UPDATE_CURRENT
        val installAppIntent = ApkInstallUtils.getInstallAppIntent(file)
        val contentIntent = PendingIntent.getActivity(
            this, 0, installAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = mBuilder ?: notificationBuilder.also { mBuilder = it }
        builder.setContentIntent(contentIntent)
            .setContentTitle(UpdateUtils.getAppName(this))
            .setContentText(getString(R.string.xupdate_download_complete))
            .setProgress(0, 0, false)
            .setDefaults(Notification.DEFAULT_ALL)
        val notification = builder.build()
        notification.flags = Notification.FLAG_AUTO_CANCEL
        mNotificationManager?.notify(DOWNLOAD_NOTIFY_ID, notification)
    }

    companion object {

        private const val DOWNLOAD_NOTIFY_ID = 1000

        private var sIsRunning = false

        private const val CHANNEL_ID = "xupdate_channel_id"
        private const val CHANNEL_NAME = "xupdate_channel_name"

        /**
         * 绑定服务
         *
         * @param connection 服务连接
         */
        @JvmStatic
        fun bindService(connection: ServiceConnection) {
            val intent = Intent(XUpdate.context, DownloadService::class.java)
            XUpdate.context.startService(intent)
            XUpdate.context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            sIsRunning = true
        }

        /**
         * 下载服务是否在运行
         *
         * @return 是否在运行
         */
        @JvmStatic
        fun isRunning(): Boolean {
            return sIsRunning
        }
    }
}
