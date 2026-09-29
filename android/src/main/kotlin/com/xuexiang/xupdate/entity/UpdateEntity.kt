package com.xuexiang.xupdate.entity

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.xuexiang.xupdate.proxy.IUpdateHttpService

/**
 * 版本更新信息实体
 *
 * @author xuexiang
 * @since 2018/6/29 下午9:33
 */
class UpdateEntity : Parcelable {

    //===========是否可以升级=============//
    /**
     * 是否有新版本
     */
    var isHasUpdate: Boolean = false

    /**
     * 是否强制安装：不安装无法使用app
     */
    var isForce: Boolean = false
        set(value) {
            if (value) {
                //强制更新，不可以忽略
                isIgnorable = false
            }
            field = value
        }

    /**
     * 是否可忽略该版本
     */
    var isIgnorable: Boolean = false
        set(value) {
            if (value) {
                //可忽略的，不能是强制更新
                isForce = false
            }
            field = value
        }

    //===========升级的信息=============//

    /**
     * 版本号
     */
    var versionCode: Int = 0

    /**
     * 版本名称
     */
    var versionName: String = "unknown_version"

    /**
     * 更新内容
     */
    var updateContent: String? = null

    /**
     * 下载信息实体
     */
    var downloadEntity: DownloadEntity = DownloadEntity()

    //============升级行为============//

    /**
     * 是否静默下载：有新版本时不提示直接下载
     */
    var isSilent: Boolean = false

    /**
     * 是否下载完成后自动安装[默认是true]
     */
    var isAutoInstall: Boolean = true

    //======内部变量，请勿设置=====//
    var iUpdateHttpService: IUpdateHttpService? = null

    //======代理属性【由downloadEntity提供】======//
    var downloadUrl: String?
        get() = downloadEntity.downloadUrl
        set(value) {
            downloadEntity.downloadUrl = value
        }

    var md5: String?
        get() = downloadEntity.md5
        set(value) {
            downloadEntity.md5 = value
        }

    var size: Long
        get() = downloadEntity.size
        set(value) {
            downloadEntity.size = value
        }

    val apkCacheDir: String?
        get() = downloadEntity.cacheDir

    constructor()

    private constructor(source: Parcel) {
        isHasUpdate = source.readByte().toInt() != 0
        isForce = source.readByte().toInt() != 0
        isIgnorable = source.readByte().toInt() != 0
        versionCode = source.readInt()
        versionName = source.readString() ?: "unknown_version"
        updateContent = source.readString()
        downloadEntity = source.readParcelable(DownloadEntity::class.java.classLoader) ?: DownloadEntity()
        isSilent = source.readByte().toInt() != 0
        isAutoInstall = source.readByte().toInt() != 0
    }

    /**
     * 设置apk的缓存地址，只支持设置一次
     */
    fun setApkCacheDir(apkCacheDir: String?): UpdateEntity {
        if (!TextUtils.isEmpty(apkCacheDir) && TextUtils.isEmpty(downloadEntity.cacheDir)) {
            downloadEntity.cacheDir = apkCacheDir
        }
        return this
    }

    /**
     * 设置是否是自动模式【自动静默下载，自动安装】
     */
    fun setIsAutoMode(isAutoMode: Boolean): UpdateEntity {
        if (isAutoMode) {
            //自动下载
            isSilent = true
            //自动安装
            isAutoInstall = true
            //自动模式下，默认下载进度条在通知栏显示
            downloadEntity.isShowNotification = true
        }
        return this
    }

    /**
     * 设置是否显示下载通知
     */
    fun setShowNotification(showNotification: Boolean): UpdateEntity {
        downloadEntity.isShowNotification = showNotification
        return this
    }

    override fun toString(): String {
        return "UpdateEntity{" +
                "mHasUpdate=" + isHasUpdate +
                ", mIsForce=" + isForce +
                ", mIsIgnorable=" + isIgnorable +
                ", mVersionCode=" + versionCode +
                ", mVersionName='" + versionName + '\'' +
                ", mUpdateContent='" + updateContent + '\'' +
                ", mDownloadEntity=" + downloadEntity +
                ", mIsSilent=" + isSilent +
                ", mIsAutoInstall=" + isAutoInstall +
                ", mIUpdateHttpService=" + iUpdateHttpService +
                '}'
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeByte(if (isHasUpdate) 1.toByte() else 0)
        dest.writeByte(if (isForce) 1.toByte() else 0)
        dest.writeByte(if (isIgnorable) 1.toByte() else 0)
        dest.writeInt(versionCode)
        dest.writeString(versionName)
        dest.writeString(updateContent)
        dest.writeParcelable(downloadEntity, flags)
        dest.writeByte(if (isSilent) 1.toByte() else 0)
        dest.writeByte(if (isAutoInstall) 1.toByte() else 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<UpdateEntity> = object : Parcelable.Creator<UpdateEntity> {
            override fun createFromParcel(source: Parcel): UpdateEntity = UpdateEntity(source)
            override fun newArray(size: Int): Array<UpdateEntity?> = arrayOfNulls(size)
        }
    }
}
