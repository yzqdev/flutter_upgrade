package com.xuexiang.xupdate.widget

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.xuexiang.flutter_xupdate.R
import com.xuexiang.flutter_xupdate.databinding.XupdateDialogUpdateBinding
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.PromptEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.proxy.IPrompterProxy
import com.xuexiang.xupdate.utils.ColorUtils
import com.xuexiang.xupdate.utils.DialogUtils
import com.xuexiang.xupdate.utils.DrawableUtils
import com.xuexiang.xupdate.utils.UpdateUtils
import java.io.File

/**
 * 版本更新弹窗
 *
 * @author xuexiang
 * @since 2018/7/24 上午9:29
 */
class UpdateDialog @JvmOverloads constructor(
    context: Context
) : Dialog(context, R.style.XUpdate_Dialog), View.OnClickListener, IDownloadEventHandler {

    /**
     * 是否同步系统控制器显示状态，默认false【状态栏、三键导航栏等】
     */
    private var mIsSyncSystemUiVisibility = false

    private lateinit var binding: XupdateDialogUpdateBinding

    //======顶部========//

    /**
     * 顶部图片
     */
    private var mIvTop: ImageView? = null

    /**
     * 标题
     */
    private var mTvTitle: TextView? = null

    //======更新内容========//

    /**
     * 版本更新内容
     */
    private var mTvUpdateInfo: TextView? = null

    /**
     * 版本更新
     */
    private var mBtnUpdate: Button? = null

    /**
     * 后台更新
     */
    private var mBtnBackgroundUpdate: Button? = null

    /**
     * 忽略版本
     */
    private var mTvIgnore: TextView? = null

    /**
     * 进度条
     */
    private var mNumberProgressBar: NumberProgressBar? = null

    //======底部========//

    /**
     * 底部关闭
     */
    private var mLlClose: LinearLayout? = null
    private var mIvClose: ImageView? = null

    //======更新信息========//

    /**
     * 更新信息
     */
    private var mUpdateEntity: UpdateEntity? = null

    /**
     * 更新代理
     */
    private var mPrompterProxy: IPrompterProxy? = null

    /**
     * 提示器参数信息
     */
    private var mPromptEntity: PromptEntity? = null

    init {
        init()
    }

    private fun init() {
        binding = XupdateDialogUpdateBinding.inflate(layoutInflater)

        setContentView(binding.root)

        setCanceledOnTouchOutside(true)

        initViews()
        initListeners()
    }

    /**
     * 设置弹窗的宽和高
     *
     * @param width  宽
     * @param height 高
     */
    protected fun setDialogSize(width: Int, height: Int): UpdateDialog {
        // 获取对话框当前的参数值
        val window = window
        if (window != null) {
            val p = window.attributes
            p.width = width
            p.height = height
            window.attributes = p
        }
        return this
    }

    protected fun getString(resId: Int): String {
        return context.resources.getString(resId)
    }

    protected fun getDrawable(resId: Int): Drawable? {
        return ContextCompat.getDrawable(context, resId)
    }

    /**
     * 设置是否同步系统控制器显示状态
     *
     * @param isSyncSystemUiVisibility 是否同步系统控制器显示状态
     * @return this
     */
    fun setIsSyncSystemUiVisibility(isSyncSystemUiVisibility: Boolean): UpdateDialog {
        mIsSyncSystemUiVisibility = isSyncSystemUiVisibility
        return this
    }

    /**
     * 显示弹窗，是否同步系统控制器显示状态
     *
     * @param isSyncSystemUiVisibility 是否同步系统控制器显示状态
     */
    fun showIfSync(isSyncSystemUiVisibility: Boolean) {
        if (isSyncSystemUiVisibility) {
            val isHandled = DialogUtils.showWindow(
                DialogUtils.findActivity(context),
                window
            ) { showWindow(showWindow = it) }
            if (!isHandled) {
                performShow()
            }
        } else {
            performShow()
        }
    }

    private fun showWindow(showWindow: Window?) {
        performShow()
    }

    /**
     * 真正执行显示的方法
     */
    protected fun performShow() {
        super.show()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (DialogUtils.isShouldHideInput(window, ev)) {
                DialogUtils.hideSoftInput(currentFocus)
            }
        }
        return super.onTouchEvent(ev)
    }

    protected fun initViews() {
        // 顶部图片
        mIvTop = binding.ivTop
        // 标题
        mTvTitle = binding.tvTitle
        // 提示内容
        mTvUpdateInfo = binding.tvUpdateInfo
        // 更新按钮
        mBtnUpdate = findViewById(R.id.btn_update)
        // 后台更新按钮
        mBtnBackgroundUpdate = findViewById(R.id.btn_background_update)
        // 忽略
        mTvIgnore = findViewById(R.id.tv_ignore)
        // 进度条
        mNumberProgressBar = findViewById(R.id.npb_progress)

        // 关闭按钮+线 的整个布局
        mLlClose = findViewById(R.id.ll_close)
        // 关闭按钮
        mIvClose = findViewById(R.id.iv_close)
    }

    protected fun initListeners() {
        mBtnUpdate?.setOnClickListener(this)
        mBtnBackgroundUpdate?.setOnClickListener(this)
        mIvClose?.setOnClickListener(this)
        mTvIgnore?.setOnClickListener(this)

        setCancelable(false)
        setCanceledOnTouchOutside(false)
        setIsSyncSystemUiVisibility(true)
    }

    //====================生命周期============================//
    private val url: String
        get() = mPrompterProxy?.getUrl() ?: ""

    override fun show() {
        XupdateTool.setIsPrompterShow(url, true)
        showIfSync(mIsSyncSystemUiVisibility)
    }

    override fun dismiss() {
        XupdateTool.setIsPrompterShow(url, false)
        clearIPrompterProxy()
        super.dismiss()
    }

    private fun clearIPrompterProxy() {
        mPrompterProxy?.recycle()
        mPrompterProxy = null
    }

    //====================UI构建============================//
    fun setUpdateEntity(updateEntity: UpdateEntity): UpdateDialog {
        mUpdateEntity = updateEntity
        initUpdateInfo(mUpdateEntity!!)
        return this
    }

    /**
     * 初始化更新信息
     *
     * @param updateEntity 版本更新信息
     */
    private fun initUpdateInfo(updateEntity: UpdateEntity) {
        // 弹出对话框
        val newVersion = updateEntity.versionName
        val updateInfo = UpdateUtils.getDisplayUpdateInfo(context, updateEntity)
        // 更新内容
        mTvUpdateInfo!!.text = updateInfo
        mTvTitle!!.text = String.format(getString(R.string.xupdate_lab_ready_update), newVersion)

        // 刷新升级按钮显示
        refreshUpdateButton()

        // 强制更新,不显示关闭按钮
        if (updateEntity.isForce) {
            mLlClose!!.visibility = View.GONE
        }
    }

    fun setPromptEntity(promptEntity: PromptEntity?): UpdateDialog {
        mPromptEntity = promptEntity
        return this
    }

    /**
     * 初始化主题色
     */
    private fun initTheme(
        themeColor: Int,
        topResId: Int,
        buttonTextColor: Int,
        widthRatio: Float,
        heightRatio: Float
    ) {
        var themeColor = themeColor
        var topResId = topResId
        var buttonTextColor = buttonTextColor
        if (themeColor == -1) {
            themeColor = ColorUtils.getColor(context, R.color.xupdate_default_theme_color)
        }
        if (topResId == -1) {
            topResId = R.drawable.xupdate_bg_app_top
        }
        if (buttonTextColor == 0) {
            buttonTextColor = if (ColorUtils.isColorDark(themeColor)) Color.WHITE else Color.BLACK
        }
        setDialogTheme(themeColor, topResId, buttonTextColor, widthRatio, heightRatio)
    }

    /**
     * 设置弹窗主题
     *
     * @param themeColor      主色
     * @param topResId        图片
     * @param buttonTextColor 按钮文字颜色
     * @param widthRatio      宽和屏幕的比例
     * @param heightRatio     高和屏幕的比例
     */
    private fun setDialogTheme(
        themeColor: Int,
        topResId: Int,
        buttonTextColor: Int,
        widthRatio: Float,
        heightRatio: Float
    ) {
        val topDrawable = XupdateTool.getTopDrawable(mPromptEntity?.topDrawableTag)
        if (topDrawable != null) {
            mIvTop!!.setImageDrawable(topDrawable)
        } else {
            mIvTop!!.setImageResource(topResId)
        }
        DrawableUtils.setBackgroundCompat(mBtnUpdate!!, DrawableUtils.getDrawable(UpdateUtils.dip2px(4, context), themeColor))
        DrawableUtils.setBackgroundCompat(mBtnBackgroundUpdate!!, DrawableUtils.getDrawable(UpdateUtils.dip2px(4, context), themeColor))
        mNumberProgressBar!!.progressTextColor = themeColor
        mNumberProgressBar!!.reachedBarColor = themeColor
        mBtnUpdate!!.setTextColor(buttonTextColor)
        mBtnBackgroundUpdate!!.setTextColor(buttonTextColor)

        initWindow(widthRatio, heightRatio)
    }

    private fun initWindow(widthRatio: Float, heightRatio: Float) {
        val window = window ?: return
        val lp = window.attributes
        val displayMetrics = context.resources.displayMetrics
        if (widthRatio > 0 && widthRatio < 1) {
            lp.width = (displayMetrics.widthPixels * widthRatio).toInt()
        }
        if (heightRatio > 0 && heightRatio < 1) {
            lp.height = (displayMetrics.heightPixels * heightRatio).toInt()
        }
        window.attributes = lp
    }

    //====================更新功能============================//
    private fun setIPrompterProxy(prompterProxy: IPrompterProxy?): UpdateDialog {
        mPrompterProxy = prompterProxy
        return this
    }

    override fun onClick(view: View) {
        val i = view.id
        //点击版本升级按钮【下载apk】
        if (i == R.id.btn_update) {
            //权限判断是否有访问外部存储空间权限
            val flag = androidx.core.app.ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (!UpdateUtils.isPrivateApkCacheDir(mUpdateEntity!!) && flag != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf("android.permission.READ_EXTERNAL_STORAGE"),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                installApp()
            }
        } else if (i == R.id.btn_background_update) {
            //点击后台更新按钮
            mPrompterProxy!!.backgroundDownload()
            dismiss()
        } else if (i == R.id.iv_close) {
            //点击关闭按钮
            mPrompterProxy!!.cancelDownload()
            dismiss()
        } else if (i == R.id.tv_ignore) {
            //点击忽略按钮
            UpdateUtils.saveIgnoreVersion(context, mUpdateEntity!!.versionName)
            dismiss()
        }
    }

    private fun installApp() {
        val entity = mUpdateEntity ?: return
        if (UpdateUtils.isApkDownloaded(entity)) {
            onInstallApk()
            //安装完自杀
            //如果上次是强制更新，但是用户在下载完，强制杀掉后台，重新启动app后，则会走到这一步，所以要进行强制更新的判断。
        if (!entity.isForce) {
                dismiss()
            } else {
                showInstallButton()
            }
        } else {
            mPrompterProxy?.startDownload(entity, WeakFileDownloadListener(this))
            //忽略版本在点击更新按钮后隐藏
            if (entity.isIgnorable) {
                mTvIgnore!!.visibility = View.GONE
            }
        }
    }

    override fun handleStart() {
        if (isShowing) {
            doStart()
        }
    }

    private fun doStart() {
        mNumberProgressBar!!.visibility = View.VISIBLE
        mNumberProgressBar!!.progress = 0
        mBtnUpdate!!.visibility = View.GONE
        if (mPromptEntity!!.isSupportBackgroundUpdate) {
            mBtnBackgroundUpdate!!.visibility = View.VISIBLE
        } else {
            mBtnBackgroundUpdate!!.visibility = View.GONE
        }
    }

    override fun handleProgress(progress: Float) {
        if (isShowing) {
            if (mNumberProgressBar!!.visibility == View.GONE) {
                doStart()
            }
            mNumberProgressBar!!.progress = Math.round(progress * 100)
            mNumberProgressBar!!.maxProgress = 100
        }
    }

    override fun handleCompleted(file: File?): Boolean {
        if (isShowing) {
            mBtnBackgroundUpdate!!.visibility = View.GONE
            if (mUpdateEntity!!.isForce) {
                showInstallButton()
            } else {
                dismiss()
            }
        }
        // 返回true，自动进行apk安装
        return true
    }

    override fun handleError(throwable: Throwable) {
        if (isShowing) {
            if (mPromptEntity!!.isIgnoreDownloadError) {
                refreshUpdateButton()
            } else {
                dismiss()
            }
        }
    }

    /**
     * 刷新升级按钮显示
     */
    private fun refreshUpdateButton() {
        if (UpdateUtils.isApkDownloaded(mUpdateEntity!!)) {
            showInstallButton()
        } else {
            showUpdateButton()
        }
        mTvIgnore!!.visibility = if (mUpdateEntity!!.isIgnorable) View.VISIBLE else View.GONE
    }

    /**
     * 显示安装的按钮
     */
    private fun showInstallButton() {
        mNumberProgressBar!!.visibility = View.GONE
        mBtnBackgroundUpdate!!.visibility = View.GONE
        mBtnUpdate!!.setText(R.string.xupdate_lab_install)
        mBtnUpdate!!.visibility = View.VISIBLE
        mBtnUpdate!!.setOnClickListener(this)
    }

    /**
     * 显示升级的按钮
     */
    private fun showUpdateButton() {
        mNumberProgressBar!!.visibility = View.GONE
        mBtnBackgroundUpdate!!.visibility = View.GONE
        mBtnUpdate!!.setText(R.string.xupdate_lab_update)
        mBtnUpdate!!.visibility = View.VISIBLE
        mBtnUpdate!!.setOnClickListener(this)
    }

    private fun onInstallApk() {
        XupdateTool.startInstallApk(context, mUpdateEntity)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        XupdateTool.setIsPrompterShow(url, true)
    }

    override fun onDetachedFromWindow() {
        XupdateTool.setIsPrompterShow(url, false)
        clearIPrompterProxy()
        super.onDetachedFromWindow()
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 11122

        /**
         * 获取更新提示
         *
         * @param updateEntity  更新信息
         * @param prompterProxy 更新代理
         * @param promptEntity  提示器参数信息
         * @return 更新提示
         */
        @JvmStatic
        fun newInstance(
            context: Context,
            updateEntity: UpdateEntity,
            prompterProxy: IPrompterProxy,
            promptEntity: PromptEntity
        ): UpdateDialog {
            val dialog = UpdateDialog(context)
            dialog.setIPrompterProxy(prompterProxy)
                .setUpdateEntity(updateEntity)
                .setPromptEntity(promptEntity)
            dialog.initTheme(
                promptEntity.themeColor, promptEntity.topResId,
                promptEntity.buttonTextColor, promptEntity.widthRatio, promptEntity.heightRatio
            )
            return dialog
        }
    }
}
