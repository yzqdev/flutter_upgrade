package com.xuexiang.xupdate.proxy.impl

import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.PromptEntity
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.proxy.IPrompterProxy
import com.xuexiang.xupdate.proxy.IUpdatePrompter
import com.xuexiang.xupdate.proxy.IUpdateProxy
import com.xuexiang.xupdate.service.OnFileDownloadListener
import com.xuexiang.xupdate.utils.UpdateLog
import com.xuexiang.xupdate.widget.UpdateDialog
import com.xuexiang.xupdate.widget.UpdateDialogActivity
import com.xuexiang.xupdate.widget.UpdateDialogFragment

/**
 * 默认版本更新提示器代理
 *
 * @author xuexiang
 * @since 2020/6/9 12:19 AM
 */
class DefaultPrompterProxyImpl(private var mUpdateProxy: IUpdateProxy?) : IPrompterProxy {

    override fun getUrl(): String {
        return mUpdateProxy?.getUrl() ?: ""
    }

    override fun startDownload(updateEntity: UpdateEntity, downloadListener: OnFileDownloadListener?) {
        mUpdateProxy?.startDownload(updateEntity, downloadListener)
    }

    override fun backgroundDownload() {
        mUpdateProxy?.backgroundDownload()
    }

    override fun cancelDownload() {
        XupdateTool.setIsPrompterShow(getUrl(), false)
        mUpdateProxy?.cancelDownload()
    }

    override fun recycle() {
        mUpdateProxy?.recycle()
        mUpdateProxy = null
    }
}

/**
 * 默认的更新提示器
 *
 * @author xuexiang
 * @since 2018/7/2 下午4:05
 */
open class DefaultUpdatePrompter : IUpdatePrompter {

    /**
     * 显示版本更新提示
     *
     * @param updateEntity 更新信息
     * @param updateProxy  更新代理
     * @param promptEntity 提示界面参数
     */
    override fun showPrompt(updateEntity: UpdateEntity, updateProxy: IUpdateProxy, promptEntity: PromptEntity) {
        val context = updateProxy.getContext()
        if (context == null) {
            UpdateLog.e("showPrompt failed, context is null!")
            return
        }
        beforeShowPrompt(updateEntity, promptEntity)
        UpdateLog.d("[DefaultUpdatePrompter] showPrompt, $promptEntity")
        if (context is androidx.fragment.app.FragmentActivity) {
            UpdateDialogFragment.show(
                context.supportFragmentManager,
                updateEntity,
                getPrompterProxy(updateProxy),
                promptEntity
            )
        } else if (context is android.app.Activity) {
            UpdateDialog.newInstance(context, updateEntity, getPrompterProxy(updateProxy), promptEntity).show()
        } else {
            UpdateDialogActivity.show(context, updateEntity, getPrompterProxy(updateProxy), promptEntity)
        }
    }

    /**
     * 显示版本更新提示之前的处理【可自定义属于自己的显示逻辑】
     *
     * @param updateEntity 更新信息
     * @param promptEntity 提示界面参数
     */
    protected open fun beforeShowPrompt(updateEntity: UpdateEntity, promptEntity: PromptEntity) {
        // 如果是强制更新的话，默认设置是否忽略下载异常为true，保证即使是下载异常也不退出提示。
        if (updateEntity.isForce) {
            promptEntity.isIgnoreDownloadError = true
        }
    }

    /**
     * 构建版本更新提示器代理【可自定义属于自己的业务逻辑】
     *
     * @param updateProxy 版本更新代理
     * @return 版本更新提示器代理
     */
    protected open fun getPrompterProxy(updateProxy: IUpdateProxy): IPrompterProxy {
        return DefaultPrompterProxyImpl(updateProxy)
    }
}
