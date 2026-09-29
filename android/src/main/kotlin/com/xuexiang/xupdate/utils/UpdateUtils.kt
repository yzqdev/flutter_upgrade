package com.xuexiang.xupdate.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.os.Environment
import android.os.Looper
import android.text.TextUtils
import com.xuexiang.flutter_xupdate.R
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_APK_CACHE_DIR_EMPTY
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_IGNORED_VERSION
import com.xuexiang.xupdate.entity.UpdateError.ERROR.CHECK_PARSE
import com.xuexiang.xupdate.proxy.IUpdateProxy
import java.io.File

/**
 * 更新工具类
 *
 * @author xuexiang
 * @since 2018/7/2 下午3:24
 */
object UpdateUtils {

    /**
     * 处理解析获取到的最新版本更新信息【版本处理的核心】
     *
     * @param updateEntity 版本更新信息
     * @param result       版本的json信息
     * @param updateProxy  更新代理
     */
    @JvmStatic
    @Throws(Exception::class)
    fun processUpdateEntity(updateEntity: UpdateEntity?, result: String, updateProxy: IUpdateProxy) {
        if (updateEntity != null) {
            if (updateEntity.isHasUpdate) {
                //校验是否是已忽略版本
                if (updateEntity.isIgnorable && isIgnoreVersion(updateProxy.getContext(), updateEntity.versionName)) {
                    XupdateTool.onUpdateError(CHECK_IGNORED_VERSION)
                    //校验apk下载缓存目录是否为空
                } else if (TextUtils.isEmpty(updateEntity.apkCacheDir)) {
                    XupdateTool.onUpdateError(CHECK_APK_CACHE_DIR_EMPTY)
                } else {
                    updateProxy.findNewVersion(updateEntity, updateProxy)
                }
            } else {
                UpdateLog.i("未发现新版本, 解析后的版本更新信息如下:$updateEntity")
                updateProxy.noNewVersion(null)
            }
        } else {
            XupdateTool.onUpdateError(CHECK_PARSE, "json:$result")
        }
    }

    /**
     * 不能为null
     */
    @JvmStatic
    fun <T> requireNonNull(`object`: T?, message: String): T {
        if (`object` == null) {
            throw NullPointerException(message)
        }
        return `object`
    }

    /**
     * 检测当前网络是否是wifi
     *
     * @return 当前网络是否是wifi
     */
    @JvmStatic
    fun checkWifi(): Boolean {
        val connectivity = XUpdate.context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val info = connectivity.activeNetworkInfo ?: return false
        return info.isConnected && info.type == ConnectivityManager.TYPE_WIFI
    }

    /**
     * 检查当前是否有网
     *
     * @return 当前是否有网
     */
    @JvmStatic
    fun checkNetwork(): Boolean {
        val connectivity = XUpdate.context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val info = connectivity.activeNetworkInfo ?: return false
        return info.isConnected
    }

    /**
     * 获取应用的VersionCode
     */
    @JvmStatic
    fun getVersionCode(context: Context?): Int {
        val packageInfo = getPackageInfo(context)
        return packageInfo?.versionCode ?: -1
    }

    /**
     * 获取应用的VersionName
     */
    @JvmStatic
    fun getVersionName(context: Context?): String {
        val packageInfo = getPackageInfo(context)
        return packageInfo?.versionName ?: ""
    }

    /**
     * 比较两个版本号
     *
     * @return [> 0 versionName1 > versionName2] [= 0 versionName1 = versionName2]  [< 0 versionName1 < versionName2]
     */
    @JvmStatic
    fun compareVersionName(versionName1: String, versionName2: String): Int {
        if (versionName1 == versionName2) {
            return 0
        }
        val versionArray1 = versionName1.split("\\.").toTypedArray() //注意此处为正则匹配，不能用"."
        val versionArray2 = versionName2.split("\\.").toTypedArray()
        var idx = 0
        val minLength = minOf(versionArray1.size, versionArray2.size) //取最小长度值
        var diff = 0
        while (idx < minLength
            && versionArray1[idx].length.also { diff = it - versionArray2[idx].length } == 0 //先比较长度
            && versionArray1[idx].compareTo(versionArray2[idx]).also { diff = it } == 0
        ) { //再比较字符
            ++idx
        }
        //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大
        diff = if (diff != 0) diff else versionArray1.size - versionArray2.size
        return diff
    }

    //=============显示====================//
    @JvmStatic
    fun dip2px(dip: Int, context: Context?): Int {
        return (dip * getDensity(context) + 0.5f).toInt()
    }

    private fun getDensity(context: Context?): Float {
        return getDisplayMetrics(context).density
    }

    private fun getDisplayMetrics(context: Context?) = context!!.resources.displayMetrics

    /**
     * Drawable to bitmap.
     *
     * @param drawable The drawable.
     * @return bitmap
     */
    @JvmStatic
    fun drawable2Bitmap(drawable: Drawable?): Bitmap {
        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap: Bitmap
        if (drawable == null || drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            bitmap = Bitmap.createBitmap(
                1, 1,
                if (drawable != null && drawable.opacity != PixelFormat.OPAQUE)
                    Bitmap.Config.ARGB_8888
                else
                    Bitmap.Config.RGB_565
            )
        } else {
            bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                if (drawable.opacity != PixelFormat.OPAQUE)
                    Bitmap.Config.ARGB_8888
                else
                    Bitmap.Config.RGB_565
            )
        }
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        return bitmap
    }

    private fun getSP(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    }

    /**
     * 保存忽略的版本信息
     *
     * @param context    上下文
     * @param newVersion 新版本
     */
    @JvmStatic
    fun saveIgnoreVersion(context: Context?, newVersion: String?) {
        getSP(context!!).edit().putString(IGNORE_VERSION, newVersion).apply()
    }

    /**
     * 是否是忽略版本
     *
     * @param context    上下文
     * @param newVersion 新版本
     * @return 是否是忽略版本
     */
    @JvmStatic
    fun isIgnoreVersion(context: Context?, newVersion: String?): Boolean {
        return getSP(context!!).getString(IGNORE_VERSION, "") == newVersion
    }

    /**
     * 获取版本更新展示信息
     */
    @JvmStatic
    fun getDisplayUpdateInfo(context: Context?, updateEntity: UpdateEntity): String {
        val targetSize = byte2FitMemorySize(updateEntity.size * 1024)
        val updateContent = updateEntity.updateContent

        var updateInfo = ""
        if (!TextUtils.isEmpty(targetSize)) {
            updateInfo = context!!.getString(R.string.xupdate_lab_new_version_size) + targetSize + "\n"
        }
        if (!TextUtils.isEmpty(updateContent)) {
            updateInfo += updateContent
        }
        return updateInfo
    }

    /**
     * 字节数转合适内存大小
     * <p>保留 1 位小数</p>
     *
     * @param byteNum 字节数
     * @return 合适内存大小
     */
    @SuppressLint("DefaultLocale")
    private fun byte2FitMemorySize(byteNum: Long): String {
        return when {
            byteNum <= 0 -> ""
            byteNum < 1024 -> String.format("%.1fB", byteNum.toDouble())
            byteNum < 1048576 -> String.format("%.1fKB", byteNum.toDouble() / 1024)
            byteNum < 1073741824 -> String.format("%.1fMB", byteNum.toDouble() / 1048576)
            else -> String.format("%.1fGB", byteNum.toDouble() / 1073741824)
        }
    }

    //=============下载====================//

    /**
     * 判断更新的安装包是否已下载完成【比较md5值】
     *
     * @param updateEntity 更新信息
     */
    @JvmStatic
    fun isApkDownloaded(updateEntity: UpdateEntity): Boolean {
        val appFile = getApkFileByUpdateEntity(updateEntity)
        return !TextUtils.isEmpty(updateEntity.md5) &&
                FileUtils.isFileExists(appFile) &&
                XupdateTool.isFileValid(updateEntity.md5, appFile)
    }

    /**
     * 根据更新信息获取apk安装文件
     *
     * @param updateEntity 更新信息
     */
    @JvmStatic
    fun getApkFileByUpdateEntity(updateEntity: UpdateEntity): File {
        val appName = getApkNameByDownloadUrl(updateEntity.downloadUrl)
        val dir = updateEntity.apkCacheDir ?: ""
        return File(dir + File.separator + updateEntity.versionName + File.separator + appName)
    }

    /**
     * 根据下载地址获取文件名
     */
    @JvmStatic
    fun getApkNameByDownloadUrl(downloadUrl: String?): String {
        if (TextUtils.isEmpty(downloadUrl)) {
            return "temp_" + System.currentTimeMillis() + ".apk"
        } else {
            var appName = downloadUrl!!.substring(downloadUrl.lastIndexOf("/") + 1)
            if (!appName.endsWith(".apk")) {
                appName = "temp_" + System.currentTimeMillis() + ".apk"
            }
            return appName
        }
    }

    /**
     * 获取应用的缓存目录
     *
     * @param uniqueName 缓存目录
     */
    @JvmStatic
    fun getDiskCacheDir(context: Context, uniqueName: String): String {
        val cachePath: String
        if (isSDCardEnable && context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir()!!.path
        } else {
            cachePath = context.cacheDir.path
        }
        return cachePath + File.separator + uniqueName
    }

    /**
     * @return 版本更新的默认缓存路径
     */
    @JvmStatic
    fun getDefaultDiskCacheDir(): File {
        return FileUtils.getFileByPath(defaultDiskCacheDirPath)!!
    }

    /**
     * ApkCacheDir是否是私有目录
     *
     * @param updateEntity 版本更新信息实体
     */
    @JvmStatic
    fun isPrivateApkCacheDir(updateEntity: UpdateEntity): Boolean {
        return FileUtils.isPrivatePath(XUpdate.context, updateEntity.apkCacheDir!!)
    }

    /**
     * @return 版本更新的默认缓存路径
     */
    @JvmStatic
    val defaultDiskCacheDirPath: String
        get() = getDiskCacheDir(XUpdate.context, KEY_XUPDATE)

    private val isSDCardEnable: Boolean
        get() = Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() ||
                !Environment.isExternalStorageRemovable()

    private fun getPackageInfo(context: Context?): PackageInfo? {
        return try {
            context!!.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    @JvmStatic
    fun getAppName(context: Context?): String {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null && packageInfo.applicationInfo != null)
            packageInfo.applicationInfo!!.loadLabel(context!!.packageManager).toString()
        else ""
    }

    @JvmStatic
    fun getAppIcon(context: Context?): Drawable? {
        val packageInfo = getPackageInfo(context)
        return if (packageInfo != null && packageInfo.applicationInfo != null)
            packageInfo.applicationInfo!!.loadIcon(context!!.packageManager)
        else null
    }

    /**
     * 应用是否在前台
     */
    @JvmStatic
    fun isAppOnForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packageName = context.packageName
        val appProcesses = activityManager.runningAppProcesses ?: return false
        for (appProcess in appProcesses) {
            if (appProcess.processName == packageName &&
                appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            ) {
                return true
            }
        }
        return false
    }

    /**
     * 是否是主线程
     *
     * @return 是否是主线程
     */
    @JvmStatic
    fun isMainThread(): Boolean {
        return Looper.getMainLooper() == Looper.myLooper()
    }

    /**
     * 页面跳转
     *
     * @param intent 跳转意图
     */
    @JvmStatic
    fun startActivity(intent: Intent?): Boolean {
        if (intent == null) {
            UpdateLog.e("[startActivity failed]: intent == null")
            return false
        }
        if (XUpdate.context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                XUpdate.context.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                UpdateLog.e(e)
            }
        } else {
            UpdateLog.e(
                "[resolveActivity failed]: " +
                        (if (intent.component != null) intent.component!!.className else intent.action) +
                        " do not register in manifest"
            )
        }
        return false
    }

    private const val IGNORE_VERSION = "xupdate_ignore_version"
    private const val PREFS_FILE = "xupdate_prefs"

    private const val KEY_XUPDATE = "xupdate"
}
