package com.xuexiang.xupdate.entity

import android.os.Parcel
import android.os.Parcelable

/**
 * 版本更新提示器参数信息
 *
 * @author xuexiang
 * @since 2018/11/19 上午9:44
 */
class PromptEntity : Parcelable {

    /**
     * 主题颜色
     */
    var themeColor: Int = -1

    /**
     * 顶部背景图片
     */
    var topResId: Int = -1

    /**
     * 顶部背景图片Drawable标识
     */
    var topDrawableTag: String = ""

    /**
     * 按钮文字颜色
     */
    var buttonTextColor: Int = 0

    /**
     * 是否支持后台更新
     */
    var isSupportBackgroundUpdate: Boolean = false

    /**
     * 版本更新提示器宽度占屏幕的比例
     */
    var widthRatio: Float = -1f

    /**
     * 版本更新提示器高度占屏幕的比例
     */
    var heightRatio: Float = -1f

    /**
     * 是否忽略下载异常【为true时，下载失败更新提示框不消失，默认是false】
     */
    var isIgnoreDownloadError: Boolean = false

    constructor()

    private constructor(source: Parcel) {
        themeColor = source.readInt()
        topResId = source.readInt()
        topDrawableTag = source.readString() ?: ""
        buttonTextColor = source.readInt()
        isSupportBackgroundUpdate = source.readByte().toInt() != 0
        widthRatio = source.readFloat()
        heightRatio = source.readFloat()
        isIgnoreDownloadError = source.readByte().toInt() != 0
    }

    override fun toString(): String {
        return "PromptEntity{" +
                "mThemeColor=" + themeColor +
                ", mTopResId=" + topResId +
                ", mTopDrawableTag=" + topDrawableTag +
                ", mButtonTextColor=" + buttonTextColor +
                ", mSupportBackgroundUpdate=" + isSupportBackgroundUpdate +
                ", mWidthRatio=" + widthRatio +
                ", mHeightRatio=" + heightRatio +
                ", mIgnoreDownloadError=" + isIgnoreDownloadError +
                '}'
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(themeColor)
        dest.writeInt(topResId)
        dest.writeString(topDrawableTag)
        dest.writeInt(buttonTextColor)
        dest.writeByte(if (isSupportBackgroundUpdate) 1.toByte() else 0)
        dest.writeFloat(widthRatio)
        dest.writeFloat(heightRatio)
        dest.writeByte(if (isIgnoreDownloadError) 1.toByte() else 0)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PromptEntity> = object : Parcelable.Creator<PromptEntity> {
            override fun createFromParcel(source: Parcel): PromptEntity = PromptEntity(source)
            override fun newArray(size: Int): Array<PromptEntity?> = arrayOfNulls(size)
        }
    }
}
