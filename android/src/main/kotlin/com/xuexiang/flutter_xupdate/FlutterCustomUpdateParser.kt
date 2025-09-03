package com.xuexiang.flutter_xupdate

import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.listener.IUpdateParseCallback
import com.xuexiang.xupdate.proxy.IUpdateParser
import io.flutter.plugin.common.MethodChannel
import java.lang.ref.WeakReference

/**
 * Flutter端自定义版本更新解析器
 *
 * @author xuexiang
 * @since 2020-02-15 15:21
 */
class FlutterCustomUpdateParser(channel: MethodChannel?) : IUpdateParser {
  private val mMethodChannel =
    WeakReference(channel)

  @Throws(Exception::class)
  override fun parseJson(json: String): UpdateEntity? {
    return null
  }

  @Throws(Exception::class)
  override fun parseJson(json: String, callback: IUpdateParseCallback) {
    val map: MutableMap<String, Any> = HashMap(3)
    map["update_json"] = json
    mMethodChannel.get()!!.invokeMethod("onCustomUpdateParse", map, object : MethodChannel.Result {
      override fun success(result: Any?) {
        handleCustomParseResult((result as HashMap<String, Any>?)!!, callback)
      }

      override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
      }

      override fun notImplemented() {
      }
    })
  }

  /**
   * 处理flutter端自定义处理的json解析
   *
   * @param result   结果
   * @param callback 回调
   */
  private fun handleCustomParseResult(
    result: HashMap<String, Any>,
    callback: IUpdateParseCallback?
  ) {
    if (callback == null) {
      return
    }
    callback.onParseResult(parseUpdateEntityMap(result))
  }

  override fun isAsyncParser(): Boolean {
    return true
  }

  companion object {
    /**
     * 解析Flutter传过来的UpdateEntity Map
     *
     * @param map
     * @return
     */
    fun parseUpdateEntityMap(map: HashMap<String, Any>): UpdateEntity {
      //必填项
      val hasUpdate = map["hasUpdate"] as Boolean
      val versionCode = map["versionCode"] as Int
      val versionName = map["versionName"] as String?
      val updateContent = map["updateContent"] as String?
      val downloadUrl = map["downloadUrl"] as String?

      val updateEntity = UpdateEntity()
      updateEntity.setHasUpdate(hasUpdate)
        .setVersionCode(versionCode)
        .setVersionName(versionName)
        .setUpdateContent(updateContent)
        .setDownloadUrl(downloadUrl)

      val isForce = map["isForce"]
      val isIgnorable = map["isIgnorable"]
      val apkSize = map["apkSize"]
      val apkMd5 = map["apkMd5"]

      if (isForce != null) {
        updateEntity.setForce(isForce as Boolean)
      }
      if (isIgnorable != null) {
        updateEntity.setIsIgnorable(isIgnorable as Boolean)
      }
      if (apkSize != null) {
        updateEntity.setSize((apkSize as Int).toLong())
      }
      if (apkMd5 != null) {
        updateEntity.setMd5(apkMd5 as String)
      }

      return updateEntity
    }
  }
}
