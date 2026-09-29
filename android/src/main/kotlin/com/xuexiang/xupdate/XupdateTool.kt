package com.xuexiang.xupdate

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.LruCache
import com.xuexiang.xupdate.entity.DownloadEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError
import com.xuexiang.xupdate.entity.UpdateError.ERROR.INSTALL_FAILED
import com.xuexiang.xupdate.listener.OnInstallListener
import com.xuexiang.xupdate.listener.OnUpdateFailureListener
import com.xuexiang.xupdate.listener.impl.DefaultInstallListener
import com.xuexiang.xupdate.listener.impl.DefaultUpdateFailureListener
import com.xuexiang.xupdate.proxy.IUpdateChecker
import com.xuexiang.xupdate.proxy.IUpdateDownloader
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.proxy.IUpdateParser
import com.xuexiang.xupdate.proxy.IUpdatePrompter
import com.xuexiang.xupdate.proxy.impl.DefaultFileEncryptor
import com.xuexiang.xupdate.service.DownloadService
import com.xuexiang.xupdate.utils.UpdateLog
import com.xuexiang.xupdate.utils.UpdateUtils
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 内部版本更新参数的获取
 *
 * @author xuexiang
 * @since 2018/7/10 下午4:27
 */
object XupdateTool {

    /**
     * 存储正在进行检查版本的状态，key为url，value为是否正在检查
     */
    private val sCheckMap = ConcurrentHashMap<String, Boolean>()

    /**
     * 存储是否正在显示版本更新，key为url，value为是否正在显示版本更新
     */
    private val sPrompterMap = ConcurrentHashMap<String, Boolean>()

    /**
     * Runnable等待队列
     */
    private val sWaitRunnableMap = ConcurrentHashMap<String, Runnable>()

    /**
     * 存储顶部图片资源
     */
    private val sTopDrawableCache = LruCache<String, Drawable>(4)

    private val sMainHandler = Handler(Looper.getMainLooper())

    /**
     * 10秒的检查延迟
     */
    private const val CHECK_TIMEOUT = 10 * 1000L

    /**
     * 获取是否正在进行更新
     *
     * @param url 请求地址
     */
    @JvmStatic
    fun isAppUpdating(url: String): Boolean {
        return DownloadService.isRunning() || getCheckUrlStatus(url) || isPrompterShow(url)
    }

    /**
     * 设置版本检查的状态【防止重复检查】
     *
     * @param url        请求地址
     * @param isChecking 是否正在检查
     */
    @JvmStatic
    fun setCheckUrlStatus(url: String?, isChecking: Boolean) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        val key = url!!
        sCheckMap[key] = isChecking
        val waitRunnable = sWaitRunnableMap[key]
        if (waitRunnable != null) {
            sMainHandler.removeCallbacks(waitRunnable)
            sWaitRunnableMap.remove(key)
        }
        if (isChecking) {
            val newRunnable = Runnable {
                // 处理超时情况
                sWaitRunnableMap.remove(key)
                sCheckMap[key] = false
            }
            sMainHandler.postDelayed(newRunnable, CHECK_TIMEOUT)
            sWaitRunnableMap[key] = newRunnable
        }
    }

    /**
     * 获取版本检查的状态
     *
     * @param url 请求地址
     * @return 是否正在检查
     */
    @JvmStatic
    fun getCheckUrlStatus(url: String?): Boolean {
        return sCheckMap[url] == true
    }

    /**
     * 设置版本更新弹窗是否已经显示
     *
     * @param url    请求地址
     * @param isShow 是否已经显示
     */
    @JvmStatic
    fun setIsPrompterShow(url: String?, isShow: Boolean) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        sPrompterMap[url!!] = isShow
    }

    /**
     * 获取版本更新弹窗是否已经显示
     *
     * @param url 请求地址
     * @return 是否正在显示
     */
    @JvmStatic
    fun isPrompterShow(url: String?): Boolean {
        return sPrompterMap[url] == true
    }

    /**
     * 保存顶部背景图片
     *
     * @param drawable 图片
     * @return 图片标识
     */
    @JvmStatic
    fun saveTopDrawable(drawable: Drawable): String {
        val tag = UUID.randomUUID().toString()
        sTopDrawableCache.put(tag, drawable)
        return tag
    }

    /**
     * 获取顶部背景图片
     *
     * @param drawableTag 图片标识
     * @return 顶部背景图片
     */
    @JvmStatic
    fun getTopDrawable(drawableTag: String?): Drawable? {
        if (TextUtils.isEmpty(drawableTag)) {
            return null
        }
        return sTopDrawableCache.get(drawableTag)
    }

    //===========================属性设置===================================//
    val params: Map<String, Any>?
        get() = XUpdate.get().mParams

    val iUpdateHttpService: IUpdateHttpService?
        get() = XUpdate.get().mUpdateHttpService

    val iUpdateChecker: IUpdateChecker?
        get() = XUpdate.get().mUpdateChecker

    val iUpdateParser: IUpdateParser?
        get() = XUpdate.get().mUpdateParser

    val iUpdatePrompter: IUpdatePrompter?
        get() = XUpdate.get().mUpdatePrompter

    val iUpdateDownLoader: IUpdateDownloader?
        get() = XUpdate.get().mUpdateDownloader

    val isGet: Boolean
        get() = XUpdate.get().mIsGet

    val isWifiOnly: Boolean
        get() = XUpdate.get().mIsWifiOnly

    val isAutoMode: Boolean
        get() = XUpdate.get().mIsAutoMode

    val apkCacheDir: String?
        get() = XUpdate.get().mApkCacheDir

    //===========================文件加密===================================//

    /**
     * 加密文件
     *
     * @param file 需要加密的文件
     */
    @JvmStatic
    fun encryptFile(file: File): String {
        if (XUpdate.get().mFileEncryptor == null) {
            XUpdate.get().mFileEncryptor = DefaultFileEncryptor()
        }
        return XUpdate.get().mFileEncryptor!!.encryptFile(file)
    }

    /**
     * 验证文件是否有效（加密是否一致）
     *
     * @param encrypt 加密值，不能为空
     * @param file    需要校验的文件
     * @return 文件是否有效
     */
    @JvmStatic
    fun isFileValid(encrypt: String?, file: File?): Boolean {
        if (XUpdate.get().mFileEncryptor == null) {
            XUpdate.get().mFileEncryptor = DefaultFileEncryptor()
        }
        return XUpdate.get().mFileEncryptor!!.isFileValid(encrypt, file)
    }

    //===========================apk安装监听===================================//
    val onInstallListener: OnInstallListener?
        get() = XUpdate.get().mOnInstallListener

    /**
     * 开始安装apk文件
     *
     * @param context 传activity可以获取安装的返回值
     * @param apkFile apk文件
     */
    @JvmStatic
    fun startInstallApk(context: Context, apkFile: File) {
        startInstallApk(context, apkFile, DownloadEntity())
    }

    /**
     * 开始安装apk文件
     *
     * @param context      传activity可以获取安装的返回值
     * @param updateEntity 版本更新信息实体
     */
    @JvmStatic
    fun startInstallApk(context: Context, updateEntity: UpdateEntity?) {
        startInstallApk(context, UpdateUtils.getApkFileByUpdateEntity(updateEntity!!), updateEntity!!.downloadEntity)
    }

    /**
     * 开始安装apk文件
     *
     * @param context        传activity可以获取安装的返回值
     * @param apkFile        apk文件
     * @param downloadEntity 文件下载信息
     */
    @JvmStatic
    fun startInstallApk(context: Context, apkFile: File, downloadEntity: DownloadEntity) {
        UpdateLog.d("开始安装apk文件, 文件路径:" + apkFile.absolutePath + ", 下载信息:" + downloadEntity)
        if (onInstallApk(context, apkFile, downloadEntity)) {
            onApkInstallSuccess() //静默安装的话，不会回调到这里
        } else {
            onUpdateError(INSTALL_FAILED)
        }
    }

    /**
     * 安装apk
     */
    private fun onInstallApk(context: Context, apkFile: File, downloadEntity: DownloadEntity): Boolean {
        if (XUpdate.get().mOnInstallListener == null) {
            XUpdate.get().mOnInstallListener = DefaultInstallListener()
        }
        return XUpdate.get().mOnInstallListener!!.onInstallApk(context, apkFile, downloadEntity)
    }

    /**
     * apk安装完毕
     */
    private fun onApkInstallSuccess() {
        if (XUpdate.get().mOnInstallListener == null) {
            XUpdate.get().mOnInstallListener = DefaultInstallListener()
        }
        XUpdate.get().mOnInstallListener!!.onInstallApkSuccess()
    }

    //===========================更新出错===================================//
    val onUpdateFailureListener: OnUpdateFailureListener?
        get() = XUpdate.get().mOnUpdateFailureListener

    /**
     * 更新出现错误
     *
     * @param errorCode 错误码
     */
    @JvmStatic
    fun onUpdateError(errorCode: Int) {
        onUpdateError(UpdateError(errorCode))
    }

    /**
     * 更新出现错误
     *
     * @param errorCode 错误码
     * @param message   错误信息
     */
    @JvmStatic
    fun onUpdateError(errorCode: Int, message: String?) {
        onUpdateError(UpdateError(errorCode, message))
    }

    /**
     * 更新出现错误
     *
     * @param updateError 更新错误
     */
    @JvmStatic
    fun onUpdateError(updateError: UpdateError) {
        if (XUpdate.get().mOnUpdateFailureListener == null) {
            XUpdate.get().mOnUpdateFailureListener = DefaultUpdateFailureListener()
        }
        XUpdate.get().mOnUpdateFailureListener!!.onFailure(updateError)
    }
}
