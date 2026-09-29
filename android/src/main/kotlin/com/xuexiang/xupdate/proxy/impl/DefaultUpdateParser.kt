package com.xuexiang.xupdate.proxy.impl

import android.text.TextUtils
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.utils.UpdateLog
import com.xuexiang.xupdate.utils.UpdateUtils
import org.json.JSONException
import org.json.JSONObject

/**
 * 默认版本更新解析器【使用JSONObject进行解析，减少第三方的依赖】
 *
 * @author xuexiang
 * @since 2018/7/5 下午4:36
 */
open class DefaultUpdateParser : AbstractUpdateParser() {

    @Throws(Exception::class)
    override fun parseJson(json: String): UpdateEntity? {
        if (!TextUtils.isEmpty(json)) {
            val jsonObject = JSONObject(json)
            return if (jsonObject.has(APIKeyUpper.CODE)) {
                // 首字母大写的Json
                parseDefaultUpperFormatJson(jsonObject)
            } else {
                // 首字母小写的Json
                parseDefaultLowerFormatJson(jsonObject)
            }
        }
        return null
    }

    /**
     * 解析默认接口字段为首字母大写的Json
     */
    @Throws(JSONException::class)
    private fun parseDefaultUpperFormatJson(jsonObject: JSONObject): UpdateEntity? {
        val code = jsonObject.getInt(APIKeyUpper.CODE)
        if (code == APIConstant.REQUEST_SUCCESS) {
            val versionCode = jsonObject.getInt(APIKeyUpper.VERSION_CODE)
            val versionName = jsonObject.optString(APIKeyUpper.VERSION_NAME)
            val updateStatus = checkUpdateStatus(jsonObject.getInt(APIKeyUpper.UPDATE_STATUS), versionCode, versionName)
            val updateEntity = UpdateEntity()
            if (updateStatus == APIConstant.NO_NEW_VERSION) {
                updateEntity.isHasUpdate = false
            } else {
                if (updateStatus == APIConstant.HAVE_NEW_VERSION_FORCED_UPDATE) {
                    updateEntity.isForce = true
                } else if (updateStatus == APIConstant.HAVE_NEW_VERSION_IGNORE_UPDATE) {
                    updateEntity.isIgnorable = true
                }
                updateEntity.isHasUpdate = true
                updateEntity.updateContent = jsonObject.getString(APIKeyUpper.MODIFY_CONTENT)
                updateEntity.versionCode = versionCode
                updateEntity.versionName = versionName
                updateEntity.downloadUrl = jsonObject.getString(APIKeyUpper.DOWNLOAD_URL)
                updateEntity.size = jsonObject.optLong(APIKeyUpper.APK_SIZE)
                updateEntity.md5 = jsonObject.optString(APIKeyUpper.APK_MD5)
            }
            return updateEntity
        }
        return null
    }

    /**
     * 解析默认接口字段为首字母小写的Json
     */
    @Throws(JSONException::class)
    private fun parseDefaultLowerFormatJson(jsonObject: JSONObject): UpdateEntity? {
        val code = jsonObject.getInt(APIKeyLower.CODE)
        if (code == APIConstant.REQUEST_SUCCESS) {
            val versionCode = jsonObject.getInt(APIKeyLower.VERSION_CODE)
            val versionName = jsonObject.optString(APIKeyLower.VERSION_NAME)
            val updateStatus = checkUpdateStatus(jsonObject.getInt(APIKeyLower.UPDATE_STATUS), versionCode, versionName)
            val updateEntity = UpdateEntity()
            if (updateStatus == APIConstant.NO_NEW_VERSION) {
                updateEntity.isHasUpdate = false
            } else {
                if (updateStatus == APIConstant.HAVE_NEW_VERSION_FORCED_UPDATE) {
                    updateEntity.isForce = true
                } else if (updateStatus == APIConstant.HAVE_NEW_VERSION_IGNORE_UPDATE) {
                    updateEntity.isIgnorable = true
                }
                updateEntity.isHasUpdate = true
                updateEntity.updateContent = jsonObject.getString(APIKeyLower.MODIFY_CONTENT)
                updateEntity.versionCode = versionCode
                updateEntity.versionName = versionName
                updateEntity.downloadUrl = jsonObject.getString(APIKeyLower.DOWNLOAD_URL)
                updateEntity.size = jsonObject.optLong(APIKeyLower.APK_SIZE)
                updateEntity.md5 = jsonObject.optString(APIKeyLower.APK_MD5)
            }
            return updateEntity
        }
        return null
    }

    /**
     * 本地校验版本更新的状态。【默认处理：当最新版本小于等于应用当前的版本时，不需要更新。】
     * 【注意：这里只是用于本地校验，应当以云端为主，如果云端没有判断逻辑，才会移至本地】
     *
     * 【==可重写该方法进行自定义处理==】
     *
     * @param updateStatus     更新状态
     * @param cloudVersionCode 云端获取的版本号
     * @param cloudVersionName 云端获取的版本名称
     * @return 版本更新的状态
     */
    protected open fun checkUpdateStatus(updateStatus: Int, cloudVersionCode: Int, cloudVersionName: String?): Int {
        if (updateStatus == APIConstant.NO_NEW_VERSION) {
            // 优先以云端版本为主
            return updateStatus
        }
        val localVersionCode = UpdateUtils.getVersionCode(XUpdate.context)
        if (cloudVersionCode <= localVersionCode) {
            UpdateLog.i("云端获取的最新版本小于等于应用当前的版本，不需要更新！当前版本:$localVersionCode, 云端版本:$cloudVersionCode")
            return APIConstant.NO_NEW_VERSION
        }
        return updateStatus
    }

    /**
     * 默认接口的API Key【所有接口字段首字母大写】
     */
    interface APIKeyUpper {
        companion object {
            const val CODE = "Code"
            const val UPDATE_STATUS = "UpdateStatus"
            const val VERSION_CODE = "VersionCode"
            const val MODIFY_CONTENT = "ModifyContent"
            const val VERSION_NAME = "VersionName"
            const val DOWNLOAD_URL = "DownloadUrl"
            const val APK_SIZE = "ApkSize"
            const val APK_MD5 = "ApkMd5"
        }
    }

    /**
     * 默认接口的API Key【所有接口字段首字母小写】
     */
    interface APIKeyLower {
        companion object {
            const val CODE = "code"
            const val UPDATE_STATUS = "updateStatus"
            const val VERSION_CODE = "versionCode"
            const val MODIFY_CONTENT = "modifyContent"
            const val VERSION_NAME = "versionName"
            const val DOWNLOAD_URL = "downloadUrl"
            const val APK_SIZE = "apkSize"
            const val APK_MD5 = "apkMd5"
        }
    }

    /**
     * 默认接口的API常量
     *
     * 0:无版本更新
     * 1:有版本更新，不需要强制升级
     * 2:有版本更新，需要强制升级
     */
    interface APIConstant {
        companion object {
            /**
             * 请求成功的code码
             */
            const val REQUEST_SUCCESS = 0

            /**
             * 无版本更新
             */
            const val NO_NEW_VERSION = 0

            /**
             * 有版本更新，不需要强制升级
             */
            const val HAVE_NEW_VERSION = 1

            /**
             * 有版本更新，需要强制升级
             */
            const val HAVE_NEW_VERSION_FORCED_UPDATE = 2

            /**
             * 有版本更新, 可忽略的版本升级
             */
            const val HAVE_NEW_VERSION_IGNORE_UPDATE = 3
        }
    }
}
