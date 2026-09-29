package com.xuexiang.xupdate.proxy.impl

import android.text.TextUtils
import com.xuexiang.xupdate.utils.UpdateLog
import com.xuexiang.xupdate.proxy.IFileEncryptor
import com.xuexiang.xupdate.utils.Md5Utils
import java.io.File

/**
 * 默认的文件加密器【使用MD5加密】
 *
 * @author xuexiang
 * @since 2020/11/23 12:22 AM
 */
class DefaultFileEncryptor : IFileEncryptor {

    /**
     * 加密文件
     *
     * @param file 目标文件
     * @return 文件的加密值
     */
    override fun encryptFile(file: File): String {
        return Md5Utils.getFileMD5(file)
    }

    /**
     * 检验文件是否有效（加密是否一致）
     *
     * @param encrypt 加密值, 如果encrypt为空，直接认为是有效的
     * @param file    需要校验的文件
     * @return 文件是否有效
     */
    override fun isFileValid(encrypt: String?, file: File?): Boolean {
        if (TextUtils.isEmpty(encrypt)) {
            return true
        }
        if (file == null) {
            return false
        }
        val fileEncrypt = encryptFile(file)
        val result = encrypt.equals(fileEncrypt, ignoreCase = true)
        if (!result) {
            UpdateLog.d("File verification failed! Target encrypt value is: $encrypt, but file encrypt value is: $fileEncrypt")
        }
        return result
    }
}
