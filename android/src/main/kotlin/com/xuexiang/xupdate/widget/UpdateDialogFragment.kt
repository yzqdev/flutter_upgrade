package com.xuexiang.xupdate.widget

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.xuexiang.flutter_xupdate.R
import com.xuexiang.flutter_xupdate.databinding.XupdateLayoutUpdatePrompterBinding
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.PromptEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.DOWNLOAD_PERMISSION_DENIED
import com.xuexiang.xupdate.entity.UpdateError.ERROR.PROMPT_UNKNOWN
import com.xuexiang.xupdate.proxy.IPrompterProxy
import com.xuexiang.xupdate.utils.ColorUtils
import com.xuexiang.xupdate.utils.DialogUtils
import com.xuexiang.xupdate.utils.DrawableUtils
import com.xuexiang.xupdate.utils.UpdateUtils
import java.io.File

/**
 * 版本更新提示器【DialogFragment实现】
 *
 * @author xuexiang
 * @since 2018/7/2 上午11:40
 */
class UpdateDialogFragment : DialogFragment(View.NO_ID), View.OnClickListener, IDownloadEventHandler {

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
     * 提示器参数信息
     */
    private var mPromptEntity: PromptEntity? = null

    /**
     * 当前屏幕方向
     */
    private var mCurrentOrientation = 0

    private lateinit var binding: XupdateLayoutUpdatePrompterBinding

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // 升级
                installApp()
            } else {
                XupdateTool.onUpdateError(DOWNLOAD_PERMISSION_DENIED)
                dismissDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        XupdateTool.setIsPrompterShow(url, true)
        setStyle(STYLE_NO_TITLE, R.style.XUpdate_Fragment_Dialog)
        mCurrentOrientation = resources.configuration.orientation
    }

    override fun onStart() {
        val dialog = dialog ?: return
        val window = dialog.window ?: return
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        // 在super.onStart();中调用mDialog.show
        super.onStart()
        DialogUtils.syncSystemUiVisibility(activity, window)
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        initDialog()
    }

    private fun initDialog() {
        val dialog = dialog ?: return
        dialog.setCanceledOnTouchOutside(false)
        setCancelable(false)
        val window = dialog.window ?: return
        val promptEntity = getPromptEntity()
        window.setGravity(Gravity.CENTER)
        val lp = window.attributes
        val displayMetrics = resources.displayMetrics
        if (promptEntity.widthRatio > 0 && promptEntity.widthRatio < 1) {
            lp.width = (displayMetrics.widthPixels * promptEntity.widthRatio).toInt()
        }
        if (promptEntity.heightRatio > 0 && promptEntity.heightRatio < 1) {
            lp.height = (displayMetrics.heightPixels * promptEntity.heightRatio).toInt()
        }
        window.attributes = lp
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = XupdateLayoutUpdatePrompterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initData()
    }

    private fun initView(view: View) {
        // 顶部图片
        mIvTop = view.findViewById(R.id.iv_top)
        // 标题
        mTvTitle = view.findViewById(R.id.tv_title)
        // 提示内容
        mTvUpdateInfo = view.findViewById(R.id.tv_update_info)
        // 更新按钮
        mBtnUpdate = view.findViewById(R.id.btn_update)
        // 后台更新按钮
        mBtnBackgroundUpdate = view.findViewById(R.id.btn_background_update)
        // 忽略
        mTvIgnore = view.findViewById(R.id.tv_ignore)
        // 进度条
        mNumberProgressBar = view.findViewById(R.id.npb_progress)

        // 关闭按钮+线 的整个布局
        mLlClose = view.findViewById(R.id.ll_close)
        // 关闭按钮
        mIvClose = view.findViewById(R.id.iv_close)
    }

    /**
     * 初始化数据
     */
    private fun initData() {
        val bundle = arguments ?: return
        mPromptEntity =
            bundle.getParcelable(KEY_UPDATE_PROMPT_ENTITY) ?: PromptEntity()
        initTheme(mPromptEntity!!.themeColor, mPromptEntity!!.topResId, mPromptEntity!!.buttonTextColor)
        mUpdateEntity = bundle.getParcelable(KEY_UPDATE_ENTITY)
        if (mUpdateEntity != null) {
            initUpdateInfo(mUpdateEntity!!)
            initListeners()
        }
    }

    /**
     * @return 版本更新提示器参数信息
     */
    private fun getPromptEntity(): PromptEntity {
        // 先从bundle中去取
        if (mPromptEntity == null) {
            arguments?.let {
                mPromptEntity = it.getParcelable(KEY_UPDATE_PROMPT_ENTITY)
            }
        }
        // 如果还不存在就使用默认的
        if (mPromptEntity == null) {
            mPromptEntity = PromptEntity()
        }
        return mPromptEntity!!
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

    /**
     * 初始化主题色
     */
    private fun initTheme(themeColor: Int, topResId: Int, buttonTextColor: Int) {
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
        setDialogTheme(themeColor, topResId, buttonTextColor)
    }

    /**
     * 设置
     *
     * @param themeColor 主题色
     * @param topResId   图片
     */
    private fun setDialogTheme(themeColor: Int, topResId: Int, buttonTextColor: Int) {
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
    }

    private fun initListeners() {
        mBtnUpdate!!.setOnClickListener(this)
        mBtnBackgroundUpdate!!.setOnClickListener(this)
        mIvClose!!.setOnClickListener(this)
        mTvIgnore!!.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val i = view.id
        // 点击版本升级按钮【下载apk】
        if (i == R.id.btn_update) {
            // 权限判断是否有访问外部存储空间权限
            val flag = ActivityCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (!UpdateUtils.isPrivateApkCacheDir(mUpdateEntity!!) && flag != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                installApp()
            }
        } else if (i == R.id.btn_background_update) {
            // 点击后台更新按钮
            sIPrompterProxy?.backgroundDownload()
            dismissDialog()
        } else if (i == R.id.iv_close) {
            // 点击关闭按钮
            sIPrompterProxy?.cancelDownload()
            dismissDialog()
        } else if (i == R.id.tv_ignore) {
            // 点击忽略按钮
            UpdateUtils.saveIgnoreVersion(activity, mUpdateEntity!!.versionName)
            dismissDialog()
        }
    }

    private fun installApp() {
        val entity = mUpdateEntity ?: return
        if (UpdateUtils.isApkDownloaded(entity)) {
            onInstallApk()
            // 安装完自杀
            // 如果上次是强制更新，但是用户在下载完，强制杀掉后台，重新启动app后，则会走到这一步，所以要进行强制更新的判断。
        if (!entity.isForce) {
                dismissDialog()
            } else {
                showInstallButton()
            }
        } else {
            sIPrompterProxy?.startDownload(entity, WeakFileDownloadListener(this))
            // 忽略版本在点击更新按钮后隐藏
            if (entity.isIgnorable) {
                mTvIgnore!!.visibility = View.GONE
            }
        }
    }

    override fun handleStart() {
        if (!this.isRemoving) {
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
        if (!this.isRemoving) {
            if (mNumberProgressBar!!.visibility == View.GONE) {
                doStart()
            }
            mNumberProgressBar!!.progress = Math.round(progress * 100)
            mNumberProgressBar!!.maxProgress = 100
        }
    }

    override fun handleCompleted(file: File?): Boolean {
        if (!this.isRemoving) {
            mBtnBackgroundUpdate!!.visibility = View.GONE
            if (mUpdateEntity!!.isForce) {
                showInstallButton()
            } else {
                dismissDialog()
            }
        }
        // 返回true，自动进行apk安装
        return true
    }

    override fun handleError(throwable: Throwable) {
        if (!this.isRemoving) {
            if (mPromptEntity!!.isIgnoreDownloadError) {
                refreshUpdateButton()
            } else {
                dismissDialog()
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
        XupdateTool.startInstallApk(context!!, mUpdateEntity)
    }

    /**
     * 弹窗消失
     */
    private fun dismissDialog() {
        XupdateTool.setIsPrompterShow(url, false)
        clearIPrompterProxy()
        dismissAllowingStateLoss()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (manager.isDestroyed || manager.isStateSaved) {
                return
            }
        }
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            XupdateTool.onUpdateError(PROMPT_UNKNOWN, e.message)
        }
    }

    /**
     * 显示更新提示
     *
     * @param manager 管理者
     */
    fun show(manager: FragmentManager) {
        show(manager, "update_dialog")
    }

    override fun onDestroyView() {
        XupdateTool.setIsPrompterShow(url, false)
        clearIPrompterProxy()
        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation != mCurrentOrientation) {
            reloadView()
        }
        mCurrentOrientation = newConfig.orientation
    }

    private fun reloadView() {
        val view = LayoutInflater.from(context).inflate(R.layout.xupdate_layout_update_prompter, null)
        val root = view as? ViewGroup ?: this.view as? ViewGroup
        if (root != null) {
            root.removeAllViews()
            root.addView(view)
            initView(root)
            initData()
        }
    }

    private val url: String
        get() = sIPrompterProxy?.getUrl() ?: ""

    companion object {
        const val KEY_UPDATE_ENTITY = "key_update_entity"
        const val KEY_UPDATE_PROMPT_ENTITY = "key_update_prompt_entity"

        /**
         * 更新代理
         */
        private var sIPrompterProxy: IPrompterProxy? = null

        /**
         * 获取更新提示
         *
         * @param fragmentManager fragment管理者
         * @param updateEntity    更新信息
         * @param prompterProxy   更新代理
         * @param promptEntity    提示器参数信息
         */
        @JvmStatic
        fun show(
            fragmentManager: FragmentManager,
            updateEntity: UpdateEntity,
            prompterProxy: IPrompterProxy,
            promptEntity: PromptEntity
        ) {
            val fragment = UpdateDialogFragment()
            val args = Bundle()
            args.putParcelable(KEY_UPDATE_ENTITY, updateEntity)
            args.putParcelable(KEY_UPDATE_PROMPT_ENTITY, promptEntity)
            fragment.arguments = args
            setIPrompterProxy(prompterProxy)
            fragment.show(fragmentManager)
        }

        private fun setIPrompterProxy(prompterProxy: IPrompterProxy?) {
            sIPrompterProxy = prompterProxy
        }

        private fun clearIPrompterProxy() {
            sIPrompterProxy?.recycle()
            sIPrompterProxy = null
        }
    }
}
