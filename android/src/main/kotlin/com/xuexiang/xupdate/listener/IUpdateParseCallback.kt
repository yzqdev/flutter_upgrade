package com.xuexiang.xupdate.listener

import com.xuexiang.xupdate.entity.UpdateEntity

/**
 * 异步解析的回调
 *
 * @author xuexiang
 * @since 2020-02-15 17:23
 */
fun interface IUpdateParseCallback {

    /**
     * 解析结果
     *
     * @param updateEntity 版本更新信息实体
     */
    fun onParseResult(updateEntity: UpdateEntity?)
}
