package com.xuexiang.xupdate.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import com.xuexiang.xupdate.XupdateTool
import com.xuexiang.xupdate.entity.UpdateError.ERROR.INSTALL_FAILED
import com.xuexiang.xupdate.utils.ShellUtils.CommandResult
import java.io.File
import java.io.IOException

/**
 * APK安装工具类
 *
 * @author xuexiang
 * @since 2018/7/2 上午1:18
 */
object ApkInstallUtils {

    /**
     * apk安装的请求码
     */
    const val REQUEST_CODE_INSTALL_APP = 999

    /**
     * 是否支持静默安装【默认是true】
     */
    @JvmStatic
    var isSupportSilentInstall: Boolean = true
        private set

    /**
     * 设置是否支持静默安装
     *
     * @param supportSilentInstall 是否支持静默安装
     */
    @JvmStatic
    fun setSupportSilentInstall(supportSilentInstall: Boolean) {
        ApkInstallUtils.isSupportSilentInstall = supportSilentInstall
    }

    /**
     * 自适应apk安装（如果设备有root权限就自动静默安装）
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    fun install(context: Context, apkFile: File): Boolean {
        return if (isSupportSilentInstall) install(context, apkFile.canonicalPath)
        else installNormal(context, apkFile.canonicalPath)
    }

    /**
     * 自适应apk安装（如果设备有root权限就自动静默安装）
     *
     * @param filePath apk文件的路径
     * @return
     */
    @JvmStatic
    fun install(context: Context, filePath: String): Boolean {
        if (isSystemApplication(context) || ShellUtils.checkRootPermission()) {
            return installAppSilent(context, filePath)
        }
        return installNormal(context, filePath)
    }

    /**
     * 静默安装 App
     * <p>非 root 需添加权限
     * {@code <uses-permission android:name="android.permission.INSTALL_PACKAGES" />}</p>
     *
     * @param filePath 文件路径
     * @return {@code true}: 安装成功<br></br>{@code false}: 安装失败
     */
    @JvmStatic
    @RequiresPermission(android.Manifest.permission.INSTALL_PACKAGES)
    fun installAppSilent(context: Context, filePath: String): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            installAppSilentBelow24(context, filePath)
        } else {
            installAppSilentAbove24(context.packageName, filePath)
        }
    }

    /**
     * 静默安装 App 在Android7.0以下起作用
     */
    @RequiresPermission(android.Manifest.permission.INSTALL_PACKAGES)
    private fun installAppSilentBelow24(context: Context, filePath: String): Boolean {
        val file = FileUtils.getFileByPath(filePath)
        if (!FileUtils.isFileExists(file)) {
            return false
        }

        val pmParams = " -r " + installLocationParams

        val command = StringBuilder()
            .append("LD_LIBRARY_PATH=/vendor/lib:/system/lib pm install ")
            .append(pmParams).append(" ")
            .append(filePath.replace(" ", "\\ "))
        val commandResult = ShellUtils.execCommand(
            command.toString(), !isSystemApplication(context), true
        )
        return commandResult.successMsg != null &&
                (commandResult.successMsg!!.contains("Success") || commandResult.successMsg!!.contains("success"))
    }

    /**
     * get params for pm install location
     */
    private val installLocationParams: String
        get() = when (installLocation) {
            APP_INSTALL_INTERNAL -> "-f"
            APP_INSTALL_EXTERNAL -> "-s"
            else -> ""
        }

    /**
     * get system install location<br></br>
     * can be set by System Menu Setting->Storage->Prefered install location
     *
     * @return
     */
    @JvmStatic
    val installLocation: Int
        get() {
            val commandResult = ShellUtils.execCommand(
                "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm get-install-location", false, true
            )
            if (commandResult.result == 0 && commandResult.successMsg != null && commandResult.successMsg!!.isNotEmpty()) {
                try {
                    val location = commandResult.successMsg!!.substring(0, 1).toInt()
                    when (location) {
                        APP_INSTALL_INTERNAL -> return APP_INSTALL_INTERNAL
                        APP_INSTALL_EXTERNAL -> return APP_INSTALL_EXTERNAL
                    }
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            }
            return APP_INSTALL_AUTO
        }

    //===============================//

    /**
     * 静默安装 App 在Android7.0及以上起作用
     */
    @RequiresPermission(android.Manifest.permission.INSTALL_PACKAGES)
    private fun installAppSilentAbove24(packageName: String, filePath: String): Boolean {
        val file = FileUtils.getFileByPath(filePath)
        if (!FileUtils.isFileExists(file)) {
            return false
        }
        val isRoot = isDeviceRooted
        val command = "pm install -i $packageName --user 0 $filePath"
        val commandResult = ShellUtils.execCommand(command, isRoot)
        return commandResult.successMsg != null &&
                commandResult.successMsg!!.lowercase().contains("success")
    }

    /**
     * 使用系统的意图安装
     *
     * @param filePath file path of package
     * @return whether apk exist
     */
    private fun installNormal(context: Context, filePath: String): Boolean {
        val file = FileUtils.getFileByPath(filePath)
        return FileUtils.isFileExists(file) && installNormal(context, file!!)
    }

    /**
     * 使用系统的意图进行apk安装
     *
     * @param context 上下文
     * @param appFile 应用文件
     * @return 安装是否成功
     */
    private fun installNormal(context: Context, appFile: File): Boolean {
        return try {
            val intent = getInstallAppIntent(appFile)
            if (intent != null && context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()) {
                if (context is Activity) {
                    context.startActivityForResult(intent, REQUEST_CODE_INSTALL_APP)
                } else {
                    context.startActivity(intent)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            XupdateTool.onUpdateError(INSTALL_FAILED, "Apk installation failed using the intent of the system!")
            false
        }
    }

    /**
     * 获取安装apk的意图
     */
    @JvmStatic
    fun getInstallAppIntent(appFile: File): Intent? {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            val fileUri = FileUtils.getUriByFile(appFile)
            intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
            intent
        } catch (e: Exception) {
            XupdateTool.onUpdateError(INSTALL_FAILED, "Failed to get intent for installation！")
            null
        }
    }

    /**
     * 判断设备是否 root
     */
    private val isDeviceRooted: Boolean
        get() {
            val su = "su"
            val locations = arrayOf(
                "/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/",
                "/system/bin/failsafe/", "/data/local/xbin/", "/data/local/bin/", "/data/local/"
            )
            for (location in locations) {
                if (File(location + su).exists()) {
                    return true
                }
            }
            return false
        }

    /**
     * whether context is system application
     */
    private fun isSystemApplication(context: Context?): Boolean {
        return context != null && isSystemApplication(context, context.packageName)
    }

    /**
     * whether packageName is system application
     */
    private fun isSystemApplication(context: Context?, packageName: String?): Boolean {
        return context != null && isSystemApplication(context.packageManager, packageName)
    }

    /**
     * whether packageName is system application
     */
    private fun isSystemApplication(packageManager: PackageManager?, packageName: String?): Boolean {
        if (packageManager == null || packageName == null || packageName.isEmpty()) {
            return false
        }
        return try {
            val app = packageManager.getApplicationInfo(packageName, 0)
            app != null && app.flags and ApplicationInfo.FLAG_SYSTEM > 0
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    private const val APP_INSTALL_AUTO = 0
    private const val APP_INSTALL_INTERNAL = 1
    private const val APP_INSTALL_EXTERNAL = 2
}
