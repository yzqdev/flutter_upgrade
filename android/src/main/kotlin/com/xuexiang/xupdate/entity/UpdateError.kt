package com.xuexiang.xupdate.entity

import android.content.Context
import android.text.TextUtils
import android.util.SparseArray
import com.xuexiang.flutter_xupdate.R

/**
 * 更新错误
 *
 * @author xuexiang
 * @since 2018/6/29 下午9:01
 */
class UpdateError : Throwable {

    /**
     * 错误码
     */
    val code: Int

    constructor(code: Int) : this(code, null as String?)

    constructor(code: Int, message: String?) : super(make(code, message)) {
        this.code = code
    }

    constructor(e: Throwable) : super(e) {
        code = ERROR.UPDATE_UNKNOWN
    }

    override fun toString(): String = message ?: ""

    /**
     * 获取详细的错误信息
     */
    val detailMsg: String
        get() = "Code:$code, msg:$message"

    /**
     * 版本更新错误码
     */
    object ERROR {
        /**
         * 查询更新失败
         */
        const val CHECK_NET_REQUEST = 2000
        const val CHECK_NO_WIFI = CHECK_NET_REQUEST + 1
        const val CHECK_NO_NETWORK = CHECK_NO_WIFI + 1
        const val CHECK_UPDATING = CHECK_NO_NETWORK + 1
        const val CHECK_NO_NEW_VERSION = CHECK_UPDATING + 1
        const val CHECK_JSON_EMPTY = CHECK_NO_NEW_VERSION + 1
        const val CHECK_PARSE = CHECK_JSON_EMPTY + 1
        const val CHECK_IGNORED_VERSION = CHECK_PARSE + 1
        const val CHECK_APK_CACHE_DIR_EMPTY = CHECK_IGNORED_VERSION + 1

        const val PROMPT_UNKNOWN = 3000
        const val PROMPT_ACTIVITY_DESTROY = PROMPT_UNKNOWN + 1

        const val DOWNLOAD_FAILED = 4000
        const val DOWNLOAD_PERMISSION_DENIED = DOWNLOAD_FAILED + 1

        /**
         * apk安装错误
         */
        const val INSTALL_FAILED = 5000

        /**
         * 未知的错误
         */
        const val UPDATE_UNKNOWN = 5100
    }

    companion object {
        private val sMessages = SparseArray<String>()

        /**
         * 初始化错误信息
         */
        fun init(context: Context) {
            sMessages.append(ERROR.CHECK_NET_REQUEST, context.getString(R.string.xupdate_error_check_net_request))
            sMessages.append(ERROR.CHECK_NO_WIFI, context.getString(R.string.xupdate_error_check_no_wifi))
            sMessages.append(ERROR.CHECK_NO_NETWORK, context.getString(R.string.xupdate_error_check_no_network))
            sMessages.append(ERROR.CHECK_UPDATING, context.getString(R.string.xupdate_error_check_updating))
            sMessages.append(ERROR.CHECK_NO_NEW_VERSION, context.getString(R.string.xupdate_error_check_no_new_version))
            sMessages.append(ERROR.CHECK_JSON_EMPTY, context.getString(R.string.xupdate_error_check_json_empty))
            sMessages.append(ERROR.CHECK_PARSE, context.getString(R.string.xupdate_error_check_parse))
            sMessages.append(ERROR.CHECK_IGNORED_VERSION, context.getString(R.string.xupdate_error_check_ignored_version))
            sMessages.append(ERROR.CHECK_APK_CACHE_DIR_EMPTY, context.getString(R.string.xupdate_error_check_apk_cache_dir_empty))

            sMessages.append(ERROR.PROMPT_UNKNOWN, context.getString(R.string.xupdate_error_prompt_unknown))
            sMessages.append(ERROR.PROMPT_ACTIVITY_DESTROY, context.getString(R.string.xupdate_error_prompt_activity_destroy))

            sMessages.append(ERROR.DOWNLOAD_FAILED, context.getString(R.string.xupdate_error_download_failed))
            sMessages.append(ERROR.DOWNLOAD_PERMISSION_DENIED, context.getString(R.string.xupdate_error_download_permission_denied))

            sMessages.append(ERROR.INSTALL_FAILED, context.getString(R.string.xupdate_error_install_failed))
        }

        private fun make(code: Int, message: String?): String {
            val m = sMessages.get(code)
            if (TextUtils.isEmpty(m)) {
                return ""
            }
            if (TextUtils.isEmpty(message) || message == "null") {
                return m!!
            }
            return m + "(" + message + ")"
        }
    }
}
