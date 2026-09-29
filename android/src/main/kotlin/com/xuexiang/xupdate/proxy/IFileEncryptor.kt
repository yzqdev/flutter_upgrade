package com.xuexiang.xupdate.proxy

import java.io.File

/**
 * 文件加密器【默认是MD5加密】
 *
 * @author xuexiang
 * @since 2020/11/23 12:22 AM
 */
interface IFileEncryptor {

    /**
     * 加密文件
     *
     * @param file 目标文件
     * @return 文件的加密值
     */
    fun encryptFile(file: File): String

    /**
     * 检验文件是否有效（加密是否一致）
     *
     * @param encrypt 加密值
     * @param file    需要校验的文件
     * @return 文件是否有效
     */
    fun isFileValid(encrypt: String?, file: File?): Boolean
}
