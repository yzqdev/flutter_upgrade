package com.xuexiang.xupdate.entity

import android.os.Parcel
import android.os.Parcelable
import com.xuexiang.xupdate.XupdateTool
import java.io.File

/**
 * 下载信息实体
 *
 * @author xuexiang
 * @since 2018/7/9 上午11:41
 */
class DownloadEntity : Parcelable {

    /**
     * 下载地址
     */
    var downloadUrl: String? = null

    /**
     * 文件下载的目录
     */
    var cacheDir: String? = null

    /**
     * 下载文件的加密值，用于校验，防止下载的apk文件被替换【当然你也可以不使用MD5加密】
     */
    var md5: String? = null

    /**
     * 下载文件的大小【单位：KB】
     */
    var size: Long = 0

    /**
     * 是否在通知栏上显示下载进度
     */
    var isShowNotification: Boolean = false

    constructor()

    private constructor(source: Parcel) {
        downloadUrl = source.readString()
        cacheDir = source.readString()
        md5 = source.readString()
        size = source.readLong()
        isShowNotification = source.readByte().toInt() != 0
    }

    /**
     * 验证文件是否有效【没设置md5默认不校验，直接有效】
     *
     * @param apkFile 需要校验的文件
     * @return 文件是否有效
     */
    fun isApkFileValid(apkFile: File?): Boolean {
        return XupdateTool.isFileValid(md5, apkFile)
    }

    override fun toString(): String {
        return "DownloadEntity{" +
                "mDownloadUrl='" + downloadUrl + '\'' +
                ", mCacheDir='" + cacheDir + '\'' +
                ", mMd5='" + md5 + '\'' +
                ", mSize=" + size +
                ", mIsShowNotification=" + isShowNotification +
                '}'
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(downloadUrl)
        dest.writeString(cacheDir)
        dest.writeString(md5)
        dest.writeLong(size)
        dest.writeByte(if (isShowNotification) 1.toByte() else 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<DownloadEntity> = object : Parcelable.Creator<DownloadEntity> {
            override fun createFromParcel(source: Parcel): DownloadEntity = DownloadEntity(source)
            override fun newArray(size: Int): Array<DownloadEntity?> = arrayOfNulls(size)
        }
    }
}
