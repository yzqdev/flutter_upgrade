package com.xuexiang.xupdate.proxy.impl

import android.text.TextUtils
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_JSON_EMPTY
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NET_REQUEST
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_NO_NEW_VERSION
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_PARSE
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_UPDATING
import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.proxy.IUpdateChecker
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.proxy.IUpdateProxy
import com.xuexiang.xupdate.utils.UpdateUtils

/**
 * 默认版本更新检查者
 *
 * @author xuexiang
 * @since 2018/7/2 下午10:21
 */
open class DefaultUpdateChecker : IUpdateChecker {

    override fun onBeforeCheck() {
    }

    override fun checkVersion(isGet: Boolean, url: String, params: Map<String, Any>, updateProxy: IUpdateProxy) {
        if (XupdateTool.isAppUpdating(url)) {
            updateProxy.onAfterCheck()
            XupdateTool.onUpdateError(CHECK_UPDATING)
            return
        }

        XupdateTool.setCheckUrlStatus(url, true)

        val callback = object : IUpdateHttpService.Callback {
            override fun onSuccess(result: String) {
                onCheckSuccess(url, result, updateProxy)
            }

            override fun onError(error: Throwable) {
                onCheckError(url, updateProxy, error)
            }
        }
        if (isGet) {
            updateProxy.getIUpdateHttpService()!!.asyncGet(url, params, callback)
        } else {
            updateProxy.getIUpdateHttpService()!!.asyncPost(url, params, callback)
        }
    }

    override fun onAfterCheck() {
    }

    /**
     * 查询成功
     *
     * @param url         查询地址
     * @param result      查询结果
     * @param updateProxy 更新代理
     */
    private fun onCheckSuccess(url: String, result: String, updateProxy: IUpdateProxy) {
        XupdateTool.setCheckUrlStatus(url, false)
        updateProxy.onAfterCheck()
        if (!TextUtils.isEmpty(result)) {
            processCheckResult(result, updateProxy)
        } else {
            XupdateTool.onUpdateError(CHECK_JSON_EMPTY)
        }
    }

    /**
     * 查询失败
     *
     * @param url         查询地址
     * @param updateProxy 更新代理
     * @param error       错误
     */
    private fun onCheckError(url: String, updateProxy: IUpdateProxy, error: Throwable) {
        XupdateTool.setCheckUrlStatus(url, false)
        updateProxy.onAfterCheck()
        XupdateTool.onUpdateError(CHECK_NET_REQUEST, error.message)
    }

    override fun processCheckResult(result: String, updateProxy: IUpdateProxy) {
        try {
            if (updateProxy.isAsyncParser()) {
                //异步解析
                updateProxy.parseJson(result, IUpdateParseCallback { updateEntity ->
                    try {
                        UpdateUtils.processUpdateEntity(updateEntity, result, updateProxy)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        XupdateTool.onUpdateError(CHECK_PARSE, e.message)
                    }
                })
            } else {
                //同步解析
                UpdateUtils.processUpdateEntity(updateProxy.parseJson(result), result, updateProxy)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            XupdateTool.onUpdateError(CHECK_PARSE, e.message)
        }
    }

    override fun noNewVersion(throwable: Throwable?) {
        XupdateTool.onUpdateError(CHECK_NO_NEW_VERSION, throwable?.message)
    }
}
