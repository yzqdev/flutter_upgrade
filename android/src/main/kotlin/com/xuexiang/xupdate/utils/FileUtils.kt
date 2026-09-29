package com.xuexiang.xupdate.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.xuexiang.xupdate.XUpdate
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/**
 * 文件操作工具类
 *
 * @author xuexiang
 * @since 2020/6/6 11:52 AM
 */
object FileUtils {

    /**
     * 只读模式
     */
    const val MODE_READ_ONLY = "r"

    private val EXT_STORAGE_PATH: String = extStoragePath

    private val EXT_STORAGE_DIR: String = EXT_STORAGE_PATH + File.separator

    private val APP_EXT_STORAGE_PATH: String = EXT_STORAGE_DIR + "Android"

    private val EXT_DOWNLOADS_PATH: String = extDownloadsPath

    private val EXT_PICTURES_PATH: String = extPicturesPath

    private val EXT_DCIM_PATH: String = extDCIMPath

    /**
     * 根据文件路径获取文件
     *
     * @param filePath 文件路径
     * @return 文件
     */
    @JvmStatic
    fun getFileByPath(filePath: String?): File? {
        return if (isSpace(filePath)) null else File(filePath!!)
    }

    /**
     * 判断文件是否存在
     *
     * @param file 文件
     * @return {@code true}: 存在<br></br>{@code false}: 不存在
     */
    @JvmStatic
    fun isFileExists(file: File?): Boolean {
        if (file == null) {
            return false
        }
        if (file.exists()) {
            return true
        }
        return isFileExists(file.absolutePath)
    }

    /**
     * 判断文件是否存在
     *
     * @param filePath 文件路径
     * @return {@code true}: 存在<br></br>{@code false}: 不存在
     */
    @JvmStatic
    fun isFileExists(filePath: String?): Boolean {
        val file = getFileByPath(filePath) ?: return false
        if (file.exists()) {
            return true
        }
        return isFileExistsApi29(filePath!!)
    }

    /**
     * Android 10判断文件是否存在的方法
     */
    private fun isFileExistsApi29(filePath: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var afd: android.content.res.AssetFileDescriptor? = null
            try {
                val uri = Uri.parse(filePath)
                afd = openAssetFileDescriptor(uri)
                if (afd == null) {
                    return false
                } else {
                    closeIOQuietly(afd)
                }
            } catch (e: FileNotFoundException) {
                return false
            } finally {
                closeIOQuietly(afd)
            }
            return true
        }
        return false
    }

    /**
     * 获取文件输入流
     */
    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun getFileInputStream(file: File?): InputStream {
        return if (isScopedStorageMode) {
            contentResolver.openInputStream(getUriByFile(file!!)!!)!!
        } else {
            FileInputStream(file)
        }
    }

    /**
     * 根据文件获取uri
     */
    @JvmStatic
    fun getUriByFile(file: File?): Uri? {
        if (file == null) {
            return null
        }
        return if (isScopedStorageMode && isPublicPath(file)) {
            val filePath = file.absolutePath
            when {
                filePath.startsWith(EXT_DOWNLOADS_PATH) -> getDownloadContentUri(XUpdate.context, file)
                filePath.startsWith(EXT_PICTURES_PATH) || filePath.startsWith(EXT_DCIM_PATH) ->
                    getMediaContentUri(XUpdate.context, file)
                else -> getUriForFile(file)
            }
        } else {
            getUriForFile(file)
        }
    }

    /**
     * Return a content URI for a given file.
     *
     * @param file The file.
     * @return a content URI for a given file
     */
    @JvmStatic
    fun getUriForFile(file: File?): Uri? {
        if (file == null) {
            return null
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val authority = XUpdate.context.packageName + ".updateFileProvider"
            FileProvider.getUriForFile(XUpdate.context, authority, file)
        } else {
            Uri.fromFile(file)
        }
    }

    /**
     * 是否是分区存储模式：在公共目录下file的api无效了
     */
    val isScopedStorageMode: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Environment.isExternalStorageLegacy()

    /**
     * 将媒体文件转化为资源定位符
     */
    @JvmStatic
    fun getMediaContentUri(context: Context, mediaFile: File): Uri? {
        val filePath = mediaFile.absolutePath
        val baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver.query(
            baseUri,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + "=? ",
            arrayOf(filePath),
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            cursor.close()
            return Uri.withAppendedPath(baseUri, "" + id)
        } else {
            if (mediaFile.exists()) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DATA, filePath)
                return context.contentResolver.insert(baseUri, values)
            }
            return null
        }
    }

    @JvmStatic
    fun getDownloadContentUri(context: Context, file: File): Uri? {
        val filePath = file.absolutePath
        val baseUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val cursor = context.contentResolver.query(
            baseUri,
            arrayOf(MediaStore.Downloads._ID),
            MediaStore.Downloads.DATA + "=? ",
            arrayOf(filePath),
            null
        )
        if (cursor != null && cursor.moveToFirst()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.DownloadColumns._ID))
            cursor.close()
            return Uri.withAppendedPath(baseUri, "" + id)
        } else {
            if (file.exists()) {
                val values = ContentValues()
                values.put(MediaStore.Downloads.DATA, filePath)
                return context.contentResolver.insert(baseUri, values)
            }
            return null
        }
    }

    /**
     * 是否是私有目录
     *
     * <pre>path: /data/data/package/</pre>
     * <pre>path: /storage/emulated/0/Android/data/package/</pre>
     *
     * @param path 需要判断的目录
     * @return 是否是私有目录
     */
    @JvmStatic
    fun isPrivatePath(context: Context, path: String): Boolean {
        if (isSpace(path)) {
            return false
        }
        val appIntPath = getAppIntPath(context)
        val appExtPath = getAppExtPath(context)
        return (!TextUtils.isEmpty(appIntPath) && path.startsWith(appIntPath!!)) ||
                (!TextUtils.isEmpty(appExtPath) && path.startsWith(appExtPath!!))
    }

    /**
     * 是否是公有目录
     *
     * @return 是否是公有目录
     */
    @JvmStatic
    fun isPublicPath(file: File?): Boolean {
        if (file == null) {
            return false
        }
        return try {
            isPublicPath(file.canonicalPath)
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 是否是公有目录
     *
     * @return 是否是公有目录
     */
    @JvmStatic
    fun isPublicPath(filePath: String?): Boolean {
        if (isSpace(filePath)) {
            return false
        }
        return filePath!!.startsWith(EXT_STORAGE_PATH) && !filePath.startsWith(APP_EXT_STORAGE_PATH)
    }

    private fun isSpace(s: String?): Boolean {
        if (s == null) {
            return true
        }
        for (element in s) {
            if (!Character.isWhitespace(element)) {
                return false
            }
        }
        return true
    }

    /**
     * 安静关闭 IO
     *
     * @param closeables closeables
     */
    @JvmStatic
    fun closeIOQuietly(vararg closeables: Closeable?) {
        if (closeables == null) {
            return
        }
        for (closeable in closeables) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    /**
     * 从uri资源符中读取文件描述
     *
     * @param uri 文本资源符
     * @return AssetFileDescriptor
     */
    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun openAssetFileDescriptor(uri: Uri): android.content.res.AssetFileDescriptor {
        return contentResolver.openAssetFileDescriptor(uri, MODE_READ_ONLY)!!
    }

    private val contentResolver: android.content.ContentResolver
        get() = XUpdate.context.contentResolver

    /**
     * 获取 Android 外置储存的根目录
     * <pre>path: /storage/emulated/0</pre>
     *
     * @return 外置储存根目录
     */
    val extStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    /**
     * 获取下载目录
     * <pre>path: /storage/emulated/0/Download</pre>
     *
     * @return 下载目录
     */
    val extDownloadsPath: String
        get() = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .absolutePath

    /**
     * 获取图片目录
     * <pre>path: /storage/emulated/0/Pictures</pre>
     *
     * @return 图片目录
     */
    val extPicturesPath: String
        get() = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .absolutePath

    /**
     * 获取相机拍摄的照片和视频的目录
     * <pre>path: /storage/emulated/0/DCIM</pre>
     *
     * @return 照片和视频目录
     */
    val extDCIMPath: String
        get() = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .absolutePath

    /**
     * 获取此应用的私有存储目录
     * <pre>path: /data/data/package/</pre>
     *
     * @return 此应用的缓存目录
     */
    @JvmStatic
    fun getAppIntPath(context: Context): String? {
        val appIntCacheFile = context.cacheDir ?: return null
        return getDirName(appIntCacheFile.absolutePath)
    }

    /**
     * 获取此应用在外置储存中的私有存储目录
     * <pre>path: /storage/emulated/0/Android/data/package/</pre>
     *
     * @return 此应用在外置储存中的缓存目录
     */
    @JvmStatic
    fun getAppExtPath(context: Context): String? {
        val appExtCacheFile = context.getExternalCacheDir() ?: return null
        return getDirName(appExtCacheFile.absolutePath)
    }

    /**
     * 获取全路径中的最长目录
     *
     * @param filePath 文件路径
     * @return filePath 最长目录
     */
    @JvmStatic
    fun getDirName(filePath: String?): String? {
        if (isSpace(filePath)) {
            return filePath
        }
        val lastSep = filePath!!.lastIndexOf(File.separator)
        return if (lastSep == -1) "" else filePath.substring(0, lastSep + 1)
    }
}
