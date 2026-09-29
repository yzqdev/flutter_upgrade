package com.xuexiang.xupdate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.fragment.app.FragmentActivity
import com.xuexiang.xupdate.entity.PromptEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NETWORK
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_WIFI
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_UPDATING
import com.xuexiang.xupdate.entity.UpdateError.ERROR.PROMPT_ACTIVITY_DESTROY
import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.proxy.IUpdateChecker
import com.xuexiang.xupdate.proxy.IUpdateDownloader
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.proxy.IUpdateParser
import com.xuexiang.xupdate.proxy.IUpdatePrompter
import com.xuexiang.xupdate.proxy.IUpdateProxy
import com.xuexiang.xupdate.proxy.impl.DefaultUpdatePrompter
import com.xuexiang.xupdate.service.OnFileDownloadListener
import com.xuexiang.xupdate.utils.UpdateLog
import com.xuexiang.xupdate.utils.UpdateUtils
import java.lang.ref.WeakReference
import java.util.TreeMap

/**
 * 版本更新管理者
 *
 * @author xuexiang
 * @since 2018/7/1 下午9:49
 */
class UpdateManager private constructor(builder: Builder) : IUpdateProxy {

    /**
     * 版本更新代理
     */
    private var mUpdateProxy: IUpdateProxy? = null

    /**
     * 更新信息
     */
    private var mUpdateEntity: UpdateEntity? = null

    /**
     * 上下文
     */
    private val mContext: WeakReference<Context> = WeakReference(builder.context)

    //============请求参数==============//

    /**
     * 版本更新的url地址
     */
    private val mUpdateUrl: String? = builder.updateUrl

    /**
     * 请求参数
     */
    private val mParams: MutableMap<String, Any>? = builder.params

    /**
     * apk缓存的目录
     */
    private val mApkCacheDir: String? = builder.apkCacheDir

    //===========更新模式================//

    /**
     * 是否只在wifi下进行版本更新检查
     */
    private val mIsWifiOnly: Boolean = builder.isWifiOnly

    /**
     * 是否是Get请求
     */
    private val mIsGet: Boolean = builder.isGet

    /**
     * 是否是自动版本更新模式【无人干预,自动下载，自动更新】
     */
    private val mIsAutoMode: Boolean = builder.isAutoMode

    //===========更新组件===============//

    /**
     * 版本更新网络请求服务API
     */
    private var mIUpdateHttpService: IUpdateHttpService? = builder.updateHttpService

    /**
     * 版本更新检查器
     */
    private val mIUpdateChecker: IUpdateChecker = builder.updateChecker

    /**
     * 版本更新解析器
     */
    private val mIUpdateParser: IUpdateParser = builder.updateParser

    /**
     * 版本更新下载器
     */
    private var mIUpdateDownloader: IUpdateDownloader? = builder.updateDownLoader

    /**
     * 文件下载监听
     */
    private var mOnFileDownloadListener: OnFileDownloadListener? = builder.onFileDownloadListener

    /**
     * 版本更新提示器
     */
    private val mIUpdatePrompter: IUpdatePrompter = builder.updatePrompter

    /**
     * 版本更新提示器参数信息
     */
    private val mPromptEntity: PromptEntity = builder.promptEntity

    /**
     * 设置版本更新的代理，可自定义版本更新
     *
     * @param updateProxy 版本更新的代理
     * @return 版本更新管理者
     */
    fun setIUpdateProxy(updateProxy: IUpdateProxy): UpdateManager {
        mUpdateProxy = updateProxy
        return this
    }

    override fun getContext(): Context? {
        return mContext.get()
    }

    override fun getUrl(): String {
        return mUpdateUrl ?: ""
    }

    override fun getIUpdateHttpService(): IUpdateHttpService? {
        return mIUpdateHttpService
    }

    /**
     * 开始版本更新
     */
    override fun update() {
        UpdateLog.d("XUpdate.update()启动:$this")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.update()
        } else {
            doUpdate()
        }
    }

    /**
     * 执行版本更新操作
     */
    private fun doUpdate() {
        onBeforeCheck()

        doCheck()
    }

    private fun doCheck() {
        if (mIsWifiOnly) {
            if (UpdateUtils.checkWifi()) {
                checkVersion()
            } else {
                onAfterCheck()
                XupdateTool.onUpdateError(CHECK_NO_WIFI)
            }
        } else {
            if (UpdateUtils.checkNetwork()) {
                checkVersion()
            } else {
                onAfterCheck()
                XupdateTool.onUpdateError(CHECK_NO_NETWORK)
            }
        }
    }

    /**
     * 版本检查之前
     */
    override fun onBeforeCheck() {
        if (mUpdateProxy != null) {
            mUpdateProxy!!.onBeforeCheck()
        } else {
            mIUpdateChecker.onBeforeCheck()
        }
    }

    /**
     * 执行网络请求，检查应用的版本信息
     */
    override fun checkVersion() {
        UpdateLog.d("开始检查版本信息...")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.checkVersion()
        } else {
            if (TextUtils.isEmpty(mUpdateUrl)) {
                throw NullPointerException("[UpdateManager] : mUpdateUrl 不能为空")
            }
            mIUpdateChecker.checkVersion(mIsGet, mUpdateUrl!!, mParams ?: emptyMap(), this)
        }
    }

    /**
     * 版本检查之后
     */
    override fun onAfterCheck() {
        if (mUpdateProxy != null) {
            mUpdateProxy!!.onAfterCheck()
        } else {
            mIUpdateChecker.onAfterCheck()
        }
    }

    override fun isAsyncParser(): Boolean {
        return if (mUpdateProxy != null) {
            mUpdateProxy!!.isAsyncParser()
        } else {
            mIUpdateParser.isAsyncParser()
        }
    }

    @Throws(Exception::class)
    override fun parseJson(json: String): UpdateEntity? {
        UpdateLog.i("服务端返回的最新版本信息:$json")
        mUpdateEntity = if (mUpdateProxy != null) {
            mUpdateProxy!!.parseJson(json)
        } else {
            mIUpdateParser.parseJson(json)
        }
        mUpdateEntity = refreshParams(mUpdateEntity)
        return mUpdateEntity
    }

    @Throws(Exception::class)
    override fun parseJson(json: String, callback: IUpdateParseCallback) {
        UpdateLog.i("服务端返回的最新版本信息:$json")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.parseJson(json, IUpdateParseCallback { updateEntity ->
                refreshParams(updateEntity)
                callback.onParseResult(updateEntity)
            })
        } else {
            mIUpdateParser.parseJson(json, IUpdateParseCallback { updateEntity ->
                refreshParams(updateEntity)
                callback.onParseResult(updateEntity)
            })
        }
    }

    /**
     * 刷新本地参数
     *
     * @param updateEntity 版本更新信息
     */
    private fun refreshParams(updateEntity: UpdateEntity?): UpdateEntity? {
        //更新信息（本地信息）
        if (updateEntity != null) {
            updateEntity.setApkCacheDir(mApkCacheDir)
            updateEntity.setIsAutoMode(mIsAutoMode)
            updateEntity.iUpdateHttpService = mIUpdateHttpService
        }
        return updateEntity
    }

    /**
     * 发现新版本
     *
     * @param updateEntity 版本更新信息
     * @param updateProxy  版本更新代理
     */
    override fun findNewVersion(updateEntity: UpdateEntity, updateProxy: IUpdateProxy) {
        UpdateLog.i("发现新版本:$updateEntity")
        if (updateEntity.isSilent) {
            //静默下载，发现新版本后，直接下载更新
            if (!UpdateUtils.isApkDownloaded(updateEntity)) {
                startDownload(updateEntity, mOnFileDownloadListener)
            } else {
                //已经下载好的直接安装
                XupdateTool.startInstallApk(getContext()!!, mUpdateEntity)
            }
        } else {
            if (mUpdateProxy != null) {
                //否则显示版本更新提示
                mUpdateProxy!!.findNewVersion(updateEntity, updateProxy)
            } else {
                if (mIUpdatePrompter is DefaultUpdatePrompter) {
                    val context = getContext()
                    if (context is FragmentActivity && context.isFinishing) {
                        XupdateTool.onUpdateError(PROMPT_ACTIVITY_DESTROY)
                    } else {
                        mIUpdatePrompter.showPrompt(updateEntity, updateProxy, mPromptEntity)
                    }
                } else {
                    mIUpdatePrompter.showPrompt(updateEntity, updateProxy, mPromptEntity)
                }
            }
        }
    }

    /**
     * 未发现新版本
     *
     * @param throwable 未发现的原因
     */
    override fun noNewVersion(throwable: Throwable?) {
        UpdateLog.i(if (throwable != null) "未发现新版本:" + throwable.message else "未发现新版本!")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.noNewVersion(throwable)
        } else {
            mIUpdateChecker.noNewVersion(throwable)
        }
    }

    override fun startDownload(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
        UpdateLog.i("开始下载更新文件:$updateEntity")
        updateEntity.iUpdateHttpService = mIUpdateHttpService
        if (mUpdateProxy != null) {
            mUpdateProxy!!.startDownload(updateEntity, downloadListener)
        } else {
            mIUpdateDownloader?.startDownload(updateEntity, downloadListener)
        }
    }

    /**
     * 后台下载
     */
    override fun backgroundDownload() {
        UpdateLog.i("点击了后台更新按钮, 在通知栏中显示下载进度...")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.backgroundDownload()
        } else {
            mIUpdateDownloader?.backgroundDownload()
        }
    }

    override fun cancelDownload() {
        UpdateLog.d("正在取消更新文件的下载...")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.cancelDownload()
        } else {
            mIUpdateDownloader?.cancelDownload()
        }
    }

    override fun recycle() {
        UpdateLog.d("正在回收资源...")
        if (mUpdateProxy != null) {
            mUpdateProxy!!.recycle()
            mUpdateProxy = null
        }
        mParams?.clear()
        mIUpdateHttpService = null
        mIUpdateDownloader = null
        mOnFileDownloadListener = null
    }

    //============================对外提供的自定义使用api===============================//

    /**
     * 为外部提供简单的下载功能
     *
     * @param downloadUrl      下载地址
     * @param downloadListener 下载监听
     * @return 是否执行成功
     */
    fun download(downloadUrl: String, downloadListener: OnFileDownloadListener?): Boolean {
        if (XupdateTool.isAppUpdating("")) {
            XupdateTool.onUpdateError(CHECK_UPDATING)
            return false
        }
        startDownload(refreshParams(UpdateEntity().apply { this.downloadUrl = downloadUrl })!!, downloadListener)
        return true
    }

    /**
     * 直接更新，不使用版本更新检查器
     *
     * @param updateEntity 版本更新信息
     * @return 是否执行成功
     */
    fun update(updateEntity: UpdateEntity): Boolean {
        if (XupdateTool.isAppUpdating("")) {
            XupdateTool.onUpdateError(CHECK_UPDATING)
            return false
        }
        mUpdateEntity = refreshParams(updateEntity)
        try {
            UpdateUtils.processUpdateEntity(mUpdateEntity, "这里调用的是直接更新方法，因此没有json!", this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    override fun toString(): String {
        return "XUpdate{" +
                "mUpdateUrl='" + mUpdateUrl + '\'' +
                ", mParams=" + mParams +
                ", mApkCacheDir='" + mApkCacheDir + '\'' +
                ", mIsWifiOnly=" + mIsWifiOnly +
                ", mIsGet=" + mIsGet +
                ", mIsAutoMode=" + mIsAutoMode +
                '}'
    }

    //============================构建者===============================//

    /**
     * 版本更新管理构建者
     */
    class Builder internal constructor(context: Context) {
        //=======必填项========//
        var context: Context = context
            internal set

        /**
         * 版本更新的url地址
         */
        var updateUrl: String? = null
            internal set

        /**
         * 请求参数
         */
        var params: MutableMap<String, Any> = TreeMap()
            internal set

        /**
         * 版本更新网络请求服务API
         */
        var updateHttpService: IUpdateHttpService? = XupdateTool.iUpdateHttpService
            internal set

        /**
         * 版本更新解析器
         */
        var updateParser: IUpdateParser = XupdateTool.iUpdateParser!!
            internal set

        //===========更新模式================//

        /**
         * 是否使用的是Get请求
         */
        var isGet: Boolean = XupdateTool.isGet
            internal set

        /**
         * 是否只在wifi下进行版本更新检查
         */
        var isWifiOnly: Boolean = XupdateTool.isWifiOnly
            internal set

        /**
         * 是否是自动版本更新模式【无人干预,有版本更新直接下载、安装】
         */
        var isAutoMode: Boolean = XupdateTool.isAutoMode
            internal set

        //===========更新行为================//

        /**
         * 版本更新检查器
         */
        var updateChecker: IUpdateChecker = XupdateTool.iUpdateChecker!!
            internal set

        /**
         * 版本更新提示器参数信息
         */
        var promptEntity: PromptEntity = PromptEntity()
            internal set

        /**
         * 版本更新提示器
         */
        var updatePrompter: IUpdatePrompter = XupdateTool.iUpdatePrompter!!
            internal set

        /**
         * 下载器
         */
        var updateDownLoader: IUpdateDownloader? = XupdateTool.iUpdateDownLoader
            internal set

        /**
         * 下载监听
         */
        var onFileDownloadListener: OnFileDownloadListener? = null
            internal set

        /**
         * apk缓存的目录
         */
        var apkCacheDir: String? = XupdateTool.apkCacheDir
            internal set

        init {
            XupdateTool.params?.let { params.putAll(it) }
        }

        /**
         * 设置版本更新检查的url
         *
         * @param updateUrl 版本更新检查的url
         * @return this
         */
        fun updateUrl(updateUrl: String): Builder {
            this.updateUrl = updateUrl
            return this
        }

        /**
         * 设置请求参数
         *
         * @param params 请求参数
         * @return this
         */
        fun params(params: Map<String, Any>): Builder {
            this.params.putAll(params)
            return this
        }

        /**
         * 设置请求参数
         *
         * @param key   键
         * @param value 值
         * @return this
         */
        fun param(key: String, value: Any): Builder {
            params[key] = value
            return this
        }

        /**
         * 设置网络请求的请求服务API
         *
         * @param updateHttpService 网络请求的请求服务API
         * @return this
         */
        fun updateHttpService(updateHttpService: IUpdateHttpService): Builder {
            this.updateHttpService = updateHttpService
            return this
        }

        /**
         * 设置apk下载的缓存目录
         *
         * @param apkCacheDir apk下载的缓存目录
         * @return this
         */
        fun apkCacheDir(apkCacheDir: String): Builder {
            this.apkCacheDir = apkCacheDir
            return this
        }

        /**
         * 是否使用Get请求
         *
         * @param isGet 是否使用Get请求
         * @return this
         */
        fun isGet(isGet: Boolean): Builder {
            this.isGet = isGet
            return this
        }

        /**
         * 设置是否是自动版本更新模式【无人干预,有版本更新直接下载、安装，需要root权限】
         *
         * @param isAutoMode 是否是自动版本更新模式
         * @return this
         */
        fun isAutoMode(isAutoMode: Boolean): Builder {
            this.isAutoMode = isAutoMode
            return this
        }

        /**
         * 设置是否只在wifi下进行版本更新检查
         *
         * @param isWifiOnly 是否只在wifi下进行版本更新检查
         * @return this
         */
        fun isWifiOnly(isWifiOnly: Boolean): Builder {
            this.isWifiOnly = isWifiOnly
            return this
        }

        /**
         * 设置版本更新检查器
         *
         * @param updateChecker 版本更新检查器
         * @return this
         */
        fun updateChecker(updateChecker: IUpdateChecker): Builder {
            this.updateChecker = updateChecker
            return this
        }

        /**
         * 设置版本更新的解析器
         *
         * @param updateParser 版本更新的解析器
         * @return this
         */
        fun updateParser(updateParser: IUpdateParser): Builder {
            this.updateParser = updateParser
            return this
        }

        /**
         * 设置版本更新提示器
         *
         * @param updatePrompter 版本更新提示器
         * @return this
         */
        fun updatePrompter(updatePrompter: IUpdatePrompter): Builder {
            this.updatePrompter = updatePrompter
            return this
        }

        /**
         * 设置文件的下载监听
         *
         * @param onFileDownloadListener 文件下载监听
         * @return this
         */
        fun setOnFileDownloadListener(onFileDownloadListener: OnFileDownloadListener?): Builder {
            this.onFileDownloadListener = onFileDownloadListener
            return this
        }

        /**
         * 设置主题颜色
         *
         * @param themeColor 主题颜色资源
         * @return this
         */
        @Deprecated("")
        fun themeColor(themeColor: Int): Builder {
            promptEntity.themeColor = themeColor
            return this
        }

        /**
         * 设置主题颜色
         *
         * @param themeColor 主题颜色资源
         * @return this
         */
        fun promptThemeColor(@ColorInt themeColor: Int): Builder {
            promptEntity.themeColor = themeColor
            return this
        }

        /**
         * 设置顶部背景图片
         *
         * @param topResId 顶部背景图片资源
         * @return this
         */
        @Deprecated("")
        fun topResId(@DrawableRes topResId: Int): Builder {
            promptEntity.topResId = topResId
            return this
        }

        /**
         * 设置顶部背景图片
         *
         * @param topResId 顶部背景图片资源
         * @return this
         */
        fun promptTopResId(@DrawableRes topResId: Int): Builder {
            promptEntity.topResId = topResId
            return this
        }

        /**
         * 设置顶部背景图片
         *
         * @param topDrawable 顶部背景图片
         * @return this
         */
        fun promptTopDrawable(topDrawable: Drawable?): Builder {
            if (topDrawable != null) {
                val tag = XupdateTool.saveTopDrawable(topDrawable)
                promptEntity.topDrawableTag = tag
            }
            return this
        }

        /**
         * 设置顶部背景图片
         *
         * @param topBitmap 顶部背景图片
         * @return this
         */
        fun promptTopBitmap(topBitmap: Bitmap?): Builder {
            if (topBitmap != null) {
                val tag = XupdateTool.saveTopDrawable(BitmapDrawable(context.resources, topBitmap))
                promptEntity.topDrawableTag = tag
            }
            return this
        }

        /**
         * 设置按钮的文字颜色
         *
         * @param buttonTextColor 按钮的文字颜色
         * @return this
         */
        fun promptButtonTextColor(@ColorInt buttonTextColor: Int): Builder {
            promptEntity.buttonTextColor = buttonTextColor
            return this
        }

        /**
         * 设置是否支持后台更新
         *
         * @param supportBackgroundUpdate 是否支持后台更新
         * @return this
         */
        fun supportBackgroundUpdate(supportBackgroundUpdate: Boolean): Builder {
            promptEntity.isSupportBackgroundUpdate = supportBackgroundUpdate
            return this
        }

        /**
         * 设置版本更新提示器宽度占屏幕的比例，默认是-1，不做约束
         *
         * @param widthRatio 提示器宽度占屏幕的比例
         * @return this
         */
        fun promptWidthRatio(widthRatio: Float): Builder {
            promptEntity.widthRatio = widthRatio
            return this
        }

        /**
         * 设置版本更新提示器高度占屏幕的比例，默认是-1，不做约束
         *
         * @param heightRatio 提示器高度占屏幕的比例
         * @return this
         */
        fun promptHeightRatio(heightRatio: Float): Builder {
            promptEntity.heightRatio = heightRatio
            return this
        }

        /**
         * 设置是否忽略下载异常【【为true时，下载失败更新提示框不消失，默认是false】】
         *
         * @param ignoreDownloadError 是否忽略下载异常
         * @return this
         */
        fun promptIgnoreDownloadError(ignoreDownloadError: Boolean): Builder {
            promptEntity.isIgnoreDownloadError = ignoreDownloadError
            return this
        }

        /**
         * 设置版本更新提示器的样式
         *
         * @param promptEntity 版本更新提示器参数信息
         * @return this
         */
        fun promptStyle(promptEntity: PromptEntity): Builder {
            this.promptEntity = promptEntity
            return this
        }

        /**
         * 设备版本更新下载器
         *
         * @param updateDownLoader 版本更新下载器
         * @return this
         */
        fun updateDownLoader(updateDownLoader: IUpdateDownloader): Builder {
            this.updateDownLoader = updateDownLoader
            return this
        }

        /**
         * 构建版本更新管理者
         *
         * @return 版本更新管理者
         */
        fun build(): UpdateManager {
            UpdateUtils.requireNonNull(context, "[UpdateManager.Builder] : context == null")
            UpdateUtils.requireNonNull(updateHttpService, "[UpdateManager.Builder] : updateHttpService == null")

            if (TextUtils.isEmpty(apkCacheDir)) {
                apkCacheDir = UpdateUtils.defaultDiskCacheDirPath
            }
            return UpdateManager(this)
        }

        /**
         * 进行版本更新
         */
        fun update() {
            build().update()
        }

        /**
         * 进行版本更新
         *
         * @param updateProxy 版本更新代理
         */
        fun update(updateProxy: IUpdateProxy) {
            build().setIUpdateProxy(updateProxy)
                .update()
        }
    }
}
