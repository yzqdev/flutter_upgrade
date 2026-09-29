package com.xuexiang.xupdate.proxy

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.service.OnFileDownloadListener

/**
 * 版本更新检查者
 *
 * @author xuexiang
 * @since 2018/7/2 下午10:21
 */
interface IUpdateChecker {

    /**
     * 版本检查之前
     */
    fun onBeforeCheck()

    /**
     * 执行网络请求，检查应用的版本信息
     *
     * @param isGet       是否使用Get请求
     * @param url         检查更新的地址
     * @param params      请求参数
     * @param updateProxy 更新代理
     */
    fun checkVersion(isGet: Boolean, url: String, params: Map<String, Any>, updateProxy: IUpdateProxy)

    /**
     * 版本检查之后
     */
    fun onAfterCheck()

    /**
     * 对检查结果进行处理
     *
     * @param result      检查结果
     * @param updateProxy 更新代理
     */
    fun processCheckResult(result: String, updateProxy: IUpdateProxy)

    /**
     * 未发现新版本
     *
     * @param throwable 未发现的原因
     */
    fun noNewVersion(throwable: Throwable?)
}
