package com.xuexiang.xupdate.utils

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * MD5加密工具类
 *
 * @author xuexiang
 * @since 2018/7/2 下午3:14
 */
object Md5Utils {

    /**
     * 获取文件的MD5值
     */
    @JvmStatic
    fun getFileMD5(file: File?): String {
        if (!FileUtils.isFileExists(file)) {
            return ""
        }
        var fis: InputStream? = null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            fis = FileUtils.getFileInputStream(file)
            val buffer = ByteArray(8192)
            var len: Int
            while (fis!!.read(buffer).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
            bytes2Hex(digest.digest())
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            FileUtils.closeIOQuietly(fis)
        }
    }

    /**
     * 一个byte转为2个hex字符
     *
     * @param src byte数组
     * @return 16进制大写字符串
     */
    private fun bytes2Hex(src: ByteArray): String {
        val res = CharArray(src.size shl 1)
        val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')
        for (i in src.indices) {
            var j = i shl 1
            res[j++] = hexDigits[src[i].toInt() ushr 4 and 0x0F]
            res[j] = hexDigits[src[i].toInt() and 0x0F]
        }
        return String(res)
    }
}
