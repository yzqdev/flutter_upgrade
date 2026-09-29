package com.xuexiang.xupdate.listener.impl

import com.xuexiang.xupdate.entity.UpdateError
import com.xuexiang.xupdate.listener.OnUpdateFailureListener
import com.xuexiang.xupdate.utils.UpdateLog

/**
 * 默认的更新出错的处理(简单地打印日志）
 *
 * @author xuexiang
 * @since 2018/7/1 下午7:48
 */
class DefaultUpdateFailureListener : OnUpdateFailureListener {

    override fun onFailure(error: UpdateError) {
        UpdateLog.e(error)
    }
}
