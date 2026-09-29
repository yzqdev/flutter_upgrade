package com.xuexiang.xupdate.proxy

import com.xuexiang.xupdate.entity.PromptEntity
import com.xuexiang.xupdate.entity.UpdateEntity

/**
 * 版本更新提示者
 *
 * @author xuexiang
 * @since 2018/7/2 下午4:05
 */
interface IUpdatePrompter {

    /**
     * 显示版本更新提示
     *
     * @param updateEntity 更新信息
     * @param updateProxy  更新代理
     * @param promptEntity 提示界面参数
     */
    fun showPrompt(updateEntity: UpdateEntity, updateProxy: IUpdateProxy, promptEntity: PromptEntity)
}
