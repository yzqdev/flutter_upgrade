package com.xuexiang.xupdate

import android.app.Application
import android.content.Context
import com.xuexiang.xupdate.entity.UpdateError
import com.xuexiang.xupdate.listener.OnInstallListener
import com.xuexiang.xupdate.listener.OnUpdateFailureListener
import com.xuexiang.xupdate.listener.impl.DefaultInstallListener
import com.xuexiang.xupdate.listener.impl.DefaultUpdateFailureListener
import com.xuexiang.xupdate.proxy.IFileEncryptor
import com.xuexiang.xupdate.proxy.IUpdateChecker
import com.xuexiang.xupdate.proxy.IUpdateDownloader
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.proxy.IUpdateParser
import com.xuexiang.xupdate.proxy.IUpdatePrompter
import com.xuexiang.xupdate.proxy.impl.DefaultFileEncryptor
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateChecker
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateDownloader
import com.xuexiang.xupdate.proxy.impl.DefaultUpdateParser
import com.xuexiang.xupdate.proxy.impl.DefaultUpdatePrompter
import com.xuexiang.xupdate.utils.ApkInstallUtils
import com.xuexiang.xupdate.utils.ILogger
import com.xuexiang.xupdate.utils.UpdateLog
import java.util.TreeMap

/**
 * 版本更新的入口
 *
 * @author xuexiang
 * @since 2018/6/29 下午7:47
 */
class XUpdate private constructor() {

    //========全局属性==========//

    /**
     * 请求参数【比如apk-key或者versionCode等】
     */
    internal var mParams: MutableMap<String, Any>? = null

    /**
     * 是否使用的是Get请求
     */
    internal var mIsGet: Boolean = false

    /**
     * 是否只在wifi下进行版本更新检查
     */
    internal var mIsWifiOnly: Boolean = true

    /**
     * 是否是自动版本更新模式【无人干预,有版本更新直接下载、安装】
     */
    internal var mIsAutoMode: Boolean = false

    /**
     * 下载的apk文件缓存目录
     */
    internal var mApkCacheDir: String? = null

    //========全局更新实现接口==========//

    /**
     * 版本更新网络请求服务API
     */
    internal var mUpdateHttpService: IUpdateHttpService? = null

    /**
     * 版本更新检查器【有默认】
     */
    internal var mUpdateChecker: IUpdateChecker = DefaultUpdateChecker()

    /**
     * 版本更新解析器【有默认】
     */
    internal var mUpdateParser: IUpdateParser = DefaultUpdateParser()

    /**
     * 版本更新提示器【有默认】
     */
    internal var mUpdatePrompter: IUpdatePrompter = DefaultUpdatePrompter()

    /**
     * 版本更新下载器【有默认】
     */
    internal var mUpdateDownloader: IUpdateDownloader = DefaultUpdateDownloader()

    /**
     * 文件加密器【有默认】
     */
    internal var mFileEncryptor: IFileEncryptor? = DefaultFileEncryptor()

    /**
     * APK安装监听【有默认】
     */
    internal var mOnInstallListener: OnInstallListener? = DefaultInstallListener()

    /**
     * 更新出错监听【有默认】
     */
    internal var mOnUpdateFailureListener: OnUpdateFailureListener? = DefaultUpdateFailureListener()

    private var mContext: Application? = null

    //===========================初始化===================================//

    /**
     * 初始化
     *
     * @param application 应用上下文
     */
    fun init(application: Application) {
        mContext = application
        UpdateError.init(application)
    }

    private val application: Application
        get() {
            testInitialize()
            return mContext!!
        }

    private fun testInitialize() {
        if (mContext == null) {
            throw ExceptionInInitializerError("请先在全局Application中调用 XUpdate.get().init() 初始化！")
        }
    }

    //===========================对外版本更新api===================================//

    /**
     * 设置全局的apk更新请求参数
     *
     * @param key   键
     * @param value 值
     * @return this
     */
    fun param(key: String, value: Any): XUpdate {
        if (mParams == null) {
            mParams = TreeMap()
        }
        UpdateLog.d("设置全局参数, key:$key, value:$value")
        mParams!![key] = value
        return this
    }

    /**
     * 设置全局的apk更新请求参数
     *
     * @param params apk更新请求参数
     * @return this
     */
    fun params(params: Map<String, Any>): XUpdate {
        logForParams(params)
        mParams = params.toMap(TreeMap())
        return this
    }

    private fun logForParams(params: Map<String, Any>) {
        val sb = StringBuilder("设置全局参数:{\n")
        for ((key, value) in params) {
            sb.append("key = ")
                .append(key)
                .append(", value = ")
                .append(value.toString())
                .append("\n")
        }
        sb.append("}")
        UpdateLog.d(sb.toString())
    }

    /**
     * 设置全局版本更新网络请求服务API
     */
    fun setIUpdateHttpService(updateHttpService: IUpdateHttpService): XUpdate {
        UpdateLog.d("设置全局更新网络请求服务:" + updateHttpService.javaClass.canonicalName)
        mUpdateHttpService = updateHttpService
        return this
    }

    /**
     * 设置全局版本更新检查
     */
    fun setIUpdateChecker(updateChecker: IUpdateChecker): XUpdate {
        mUpdateChecker = updateChecker
        return this
    }

    /**
     * 设置全局版本更新的解析器
     */
    fun setIUpdateParser(updateParser: IUpdateParser): XUpdate {
        mUpdateParser = updateParser
        return this
    }

    /**
     * 设置全局版本更新提示器
     */
    fun setIUpdatePrompter(updatePrompter: IUpdatePrompter?): XUpdate {
        mUpdatePrompter = updatePrompter ?: DefaultUpdatePrompter()
        return this
    }

    /**
     * 设置全局版本更新下载器
     */
    fun setIUpdateDownLoader(updateDownLoader: IUpdateDownloader): XUpdate {
        mUpdateDownloader = updateDownLoader
        return this
    }

    /**
     * 设置是否使用的是Get请求
     */
    fun isGet(isGet: Boolean): XUpdate {
        UpdateLog.d("设置全局是否使用的是Get请求:$isGet")
        mIsGet = isGet
        return this
    }

    /**
     * 设置是否只在wifi下进行版本更新检查
     */
    fun isWifiOnly(isWifiOnly: Boolean): XUpdate {
        UpdateLog.d("设置全局是否只在wifi下进行版本更新检查:$isWifiOnly")
        mIsWifiOnly = isWifiOnly
        return this
    }

    /**
     * 设置是否是自动版本更新模式【无人干预,有版本更新直接下载、安装】
     */
    fun isAutoMode(isAutoMode: Boolean): XUpdate {
        UpdateLog.d("设置全局是否是自动版本更新模式:$isAutoMode")
        mIsAutoMode = isAutoMode
        return this
    }

    /**
     * 设置apk的缓存路径
     */
    fun setApkCacheDir(apkCacheDir: String?): XUpdate {
        UpdateLog.d("设置全局apk的缓存路径:$apkCacheDir")
        mApkCacheDir = apkCacheDir
        return this
    }

    /**
     * 设置是否支持静默安装
     */
    fun supportSilentInstall(supportSilentInstall: Boolean): XUpdate {
        ApkInstallUtils.setSupportSilentInstall(supportSilentInstall)
        return this
    }

    /**
     * 设置是否是debug模式
     */
    fun debug(isDebug: Boolean): XUpdate {
        UpdateLog.debug(isDebug)
        return this
    }

    /**
     * 设置日志打印接口
     */
    fun setILogger(logger: ILogger): XUpdate {
        UpdateLog.setLogger(logger)
        return this
    }

    //===========================apk安装监听===================================//

    /**
     * 设置文件加密器
     */
    fun setIFileEncryptor(fileEncryptor: IFileEncryptor?): XUpdate {
        mFileEncryptor = fileEncryptor
        return this
    }

    /**
     * 设置安装监听
     */
    fun setOnInstallListener(onInstallListener: OnInstallListener?): XUpdate {
        mOnInstallListener = onInstallListener
        return this
    }

    //===========================更新出错===================================//

    /**
     * 设置更新出错的监听
     */
    fun setOnUpdateFailureListener(onUpdateFailureListener: OnUpdateFailureListener): XUpdate {
        mOnUpdateFailureListener = onUpdateFailureListener
        return this
    }

    companion object {

        private val sInstance = XUpdate()

        /**
         * 获取版本更新的入口
         *
         * @return 版本更新的入口
         */
        @JvmStatic
        fun get(): XUpdate = sInstance

        /**
         * 获取版本更新构建者
         *
         * @param context 上下文
         * @return 版本更新构建者
         */
        @JvmStatic
        fun newBuild(context: Context): UpdateManager.Builder {
            return UpdateManager.Builder(context)
        }

        /**
         * 获取版本更新构建者
         *
         * @param context   上下文
         * @param updateUrl 版本更新检查的地址
         * @return 版本更新构建者
         */
        @JvmStatic
        fun newBuild(context: Context, updateUrl: String): UpdateManager.Builder {
            return UpdateManager.Builder(context)
                .updateUrl(updateUrl)
        }

        @JvmStatic
        val context: Context
            get() = get().application
    }
}
