package com.xuexiang.xupdate.listener

import com.xuexiang.xupdate.entity.UpdateError

/**
 * 更新出错监听
 *
 * @author xuexiang
 * @since 2018/6/29 下午4:15
 */
fun interface OnUpdateFailureListener {

    /**
     * 更新出错
     *
     * @param error 错误
     */
    fun onFailure(error: UpdateError)
}
