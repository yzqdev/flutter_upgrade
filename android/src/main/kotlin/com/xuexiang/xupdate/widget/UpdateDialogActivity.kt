package com.xuexiang.xupdate.widget

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.xuexiang.flutter_xupdate.R
import com.xuexiang.flutter_xupdate.databinding.XupdateLayoutUpdatePrompterBinding
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.PromptEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.DOWNLOAD_PERMISSION_DENIED
import com.xuexiang.xupdate.proxy.IPrompterProxy
import com.xuexiang.xupdate.utils.ColorUtils
import com.xuexiang.xupdate.utils.DrawableUtils
import com.xuexiang.xupdate.utils.UpdateUtils
import java.io.File

/**
 * 版本更新提示器【AppCompatActivity实现】
 *
 * @author xuexiang
 * @since 2020/6/8 10:47 PM
 */
class UpdateDialogActivity : AppCompatActivity(), IDownloadEventHandler {

    private lateinit var binding: XupdateLayoutUpdatePrompterBinding

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
        binding = XupdateLayoutUpdatePrompterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        XupdateTool.setIsPrompterShow(url, true)
        initView()
        initData()
    }

    private fun initView() {
        // 顶部图片
        mIvTop = findViewById(R.id.iv_top)
        // 标题
        mTvTitle = findViewById(R.id.tv_title)
        // 提示内容
        mTvUpdateInfo = findViewById(R.id.tv_update_info)
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

    /**
     * 初始化数据
     */
    private fun initData() {
        val bundle = intent.extras ?: return
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
            intent.extras?.let {
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
        val updateInfo = UpdateUtils.getDisplayUpdateInfo(this, updateEntity)
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
            themeColor = ColorUtils.getColor(this, R.color.xupdate_default_theme_color)
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
        DrawableUtils.setBackgroundCompat(mBtnUpdate!!, DrawableUtils.getDrawable(UpdateUtils.dip2px(4, this), themeColor))
        DrawableUtils.setBackgroundCompat(mBtnBackgroundUpdate!!, DrawableUtils.getDrawable(UpdateUtils.dip2px(4, this), themeColor))
        mNumberProgressBar!!.progressTextColor = themeColor
        mNumberProgressBar!!.reachedBarColor = themeColor
        mBtnUpdate!!.setTextColor(buttonTextColor)
        mBtnBackgroundUpdate!!.setTextColor(buttonTextColor)
    }

    private fun initListeners() {
        mBtnUpdate?.setOnClickListener {
            // 权限判断是否有访问外部存储空间权限
            val flag = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (!UpdateUtils.isPrivateApkCacheDir(mUpdateEntity!!) && flag != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                installApp()
            }
        }
        mBtnBackgroundUpdate?.setOnClickListener {
            // 点击后台更新按钮
            sIPrompterProxy?.backgroundDownload()
            dismissDialog()
        }
        mIvClose?.setOnClickListener {
            // 点击关闭按钮
            sIPrompterProxy?.cancelDownload()
            dismissDialog()
        }
        mTvIgnore?.setOnClickListener {
            // 点击忽略按钮
            UpdateUtils.saveIgnoreVersion(this, mUpdateEntity!!.versionName)
            dismissDialog()
        }
    }

    override fun onStart() {
        super.onStart()
        initWindowStyle()
    }

    private fun initWindowStyle() {
        val window = window ?: return
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 禁用返回键
        return keyCode == KeyEvent.KEYCODE_BACK
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
        if (!isFinishing) {
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
        if (!isFinishing) {
            if (mNumberProgressBar!!.visibility == View.GONE) {
                doStart()
            }
            mNumberProgressBar!!.progress = Math.round(progress * 100)
            mNumberProgressBar!!.maxProgress = 100
        }
    }

    override fun handleCompleted(file: File?): Boolean {
        if (!isFinishing) {
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
        if (!isFinishing) {
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
    }

    /**
     * 显示升级的按钮
     */
    private fun showUpdateButton() {
        mNumberProgressBar!!.visibility = View.GONE
        mBtnBackgroundUpdate!!.visibility = View.GONE
        mBtnUpdate!!.setText(R.string.xupdate_lab_update)
        mBtnUpdate!!.visibility = View.VISIBLE
    }

    private fun onInstallApk() {
        XupdateTool.startInstallApk(this, mUpdateEntity)
    }

    /**
     * 弹窗消失
     */
    private fun dismissDialog() {
        finish()
    }

    override fun onStop() {
        if (isFinishing) {
            XupdateTool.setIsPrompterShow(url, false)
            clearIPrompterProxy()
        }
        super.onStop()
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
         * 显示更新提示
         *
         * @param updateEntity  更新信息
         * @param prompterProxy 更新代理
         * @param promptEntity  提示器参数信息
         */
        @JvmStatic
        fun show(
            context: Context,
            updateEntity: UpdateEntity,
            prompterProxy: IPrompterProxy,
            promptEntity: PromptEntity
        ) {
            val intent = Intent(context, UpdateDialogActivity::class.java)
            intent.putExtra(KEY_UPDATE_ENTITY, updateEntity)
            intent.putExtra(KEY_UPDATE_PROMPT_ENTITY, promptEntity)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            setIPrompterProxy(prompterProxy)
            context.startActivity(intent)
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
