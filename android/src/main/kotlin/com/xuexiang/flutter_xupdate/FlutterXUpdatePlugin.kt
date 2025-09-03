package com.xuexiang.flutter_xupdate

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import com.xuexiang.xupdate.UpdateManager
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.entity.UpdateEntity
import com.xuexiang.xupdate.entity.UpdateError
import com.xuexiang.xupdate.listener.OnUpdateFailureListener
import com.xuexiang.xupdate.utils.UpdateUtils
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

import java.lang.ref.WeakReference

/**
 * FlutterXUpdatePlugin
 *
 * @author xuexiang
 * @since 2020-02-04 16:33
 */
class FlutterXUpdatePlugin : FlutterPlugin, ActivityAware,
  MethodChannel.MethodCallHandler {
  private lateinit var mMethodChannel: MethodChannel
  private lateinit var mApplication: Application
  private   var mActivity: Activity? =null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    mMethodChannel = MethodChannel(flutterPluginBinding.getBinaryMessenger(), PLUGIN_NAME)
    mApplication = flutterPluginBinding.applicationContext as Application
    mMethodChannel.setMethodCallHandler(this)
  }

  override fun onDetachedFromEngine(binding:  FlutterPlugin.FlutterPluginBinding) {
    mMethodChannel.setMethodCallHandler(null)

  }



  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      "getPlatformVersion" -> result.success("Android " + Build.VERSION.RELEASE)
      "initXUpdate" -> initXUpdate(call, result)
      "checkUpdate" -> checkUpdate(call, result)
      "updateByInfo" -> updateByInfo(call, result)
      "showRetryUpdateTipDialog" -> showRetryUpdateTipDialog(call, result)
      else -> result.notImplemented()
    }
  }

  /**
   * 初始化
   *
   * @param call
   * @param result
   */
  private fun initXUpdate(call: MethodCall, result: MethodChannel.Result) {
    val map = call.arguments as Map<String, Any?>
    val debug = map["debug"] as Boolean
    val isGet = map["isGet"] as Boolean
    val timeout = map["timeout"] as Int
    val isPostJson = map["isPostJson"] as Boolean
    val isWifiOnly = map["isWifiOnly"] as Boolean
    val isAutoMode = map["isAutoMode"] as Boolean
    val supportSilentInstall = map["supportSilentInstall"] as Boolean
    val enableRetry = map["enableRetry"] as Boolean
    val retryContent = map["retryContent"] as String
    val retryUrl = map["retryUrl"] as String?

    XUpdate.get()
      .debug(debug) //默认设置使用get请求检查版本
      .isGet(isGet) //默认设置只在wifi下检查版本更新
      .isWifiOnly(isWifiOnly) //默认设置非自动模式，可根据具体使用配置
      .isAutoMode(isAutoMode) //是否支持静默安装
      .supportSilentInstall(supportSilentInstall)
      .setOnUpdateFailureListener(object : OnUpdateFailureListener {
        override fun onFailure(error: UpdateError) {
          val errorMap: MutableMap<String, Any> = HashMap()
          errorMap["code"] = error.code
          errorMap["message"] = error.message!!
          errorMap["detailMsg"] = error.detailMsg
          if (mMethodChannel != null) {
            mMethodChannel.invokeMethod("onUpdateError", errorMap)
          }
        }
      }) //设置默认公共请求参数
      .param("versionCode", UpdateUtils.getVersionCode(mApplication))
      .param("appKey", mApplication!!.packageName)
      .setIUpdateDownLoader(
        RetryUpdateDownloader(
          enableRetry!!,
          retryContent,
          retryUrl
        )
      ) //这个必须设置！实现网络请求功能。
      .setIUpdateHttpService(OKHttpUpdateHttpService(timeout, isPostJson!!))
    if (map["params"] != null) {
      XUpdate.get().params(map["params"] as Map<String, Any>)
    }
    XUpdate.get().init(mApplication)

    result.success(map)
  }

  /**
   * 版本更新
   *
   * @param call
   * @param result
   */
  private fun checkUpdate(call: MethodCall, result: MethodChannel.Result) {
    if (mActivity == null || mActivity!!  == null) {
      result.error("1001", "Not attach a Activity", null)
    }

    val url = call.argument<String>("url")
    val supportBackgroundUpdate = call.argument<Boolean>("supportBackgroundUpdate")!!
    val isAutoMode = call.argument<Boolean>("isAutoMode")!!
    val isCustomParse = call.argument<Boolean>("isCustomParse")!!
    val themeColor = call.argument<String>("themeColor")
    val topImageRes = call.argument<String>("topImageRes")
    val buttonTextColor = call.argument<String>("buttonTextColor")

    val widthRatio = call.argument<Double>("widthRatio")
    val heightRatio = call.argument<Double>("heightRatio")

    val overrideGlobalRetryStrategy = call.argument<Boolean>("overrideGlobalRetryStrategy")!!
    val enableRetry = call.argument<Boolean>("enableRetry")!!
    val retryContent = call.argument<String>("retryContent")
    val retryUrl = call.argument<String>("retryUrl")

    val builder: UpdateManager.Builder = XUpdate.newBuild(mActivity!! )
      .updateUrl(url!!)
      .isAutoMode(isAutoMode)
      .supportBackgroundUpdate(supportBackgroundUpdate)
    if (call.argument<Any?>("params") != null) {
      builder.params(call.argument<Any>("params") as Map<String, Any>)
    }
    if (isCustomParse) {
      builder.updateParser(FlutterCustomUpdateParser(mMethodChannel))
    }

    updatePromptStyle(
      builder,
      themeColor,
      topImageRes,
      buttonTextColor,
      widthRatio,
      heightRatio,
      overrideGlobalRetryStrategy,
      enableRetry,
      retryContent,
      retryUrl
    )

    builder.update()
  }

  /**
   * 直接传入UpdateEntity进行版本更新
   *
   * @param call
   * @param result
   */
  private fun updateByInfo(call: MethodCall, result: MethodChannel.Result) {
    if (mActivity == null || mActivity!! == null) {
      result.error("1001", "Not attach a Activity", null)
    }

    val map = call.argument<HashMap<String, Any>>("updateEntity")
    val updateEntity: UpdateEntity = FlutterCustomUpdateParser.Companion.parseUpdateEntityMap(
      map!!
    )

    val supportBackgroundUpdate = call.argument<Boolean>("supportBackgroundUpdate")!!
    val isAutoMode = call.argument<Boolean>("isAutoMode")!!
    val themeColor = call.argument<String>("themeColor")
    val topImageRes = call.argument<String>("topImageRes")
    val buttonTextColor = call.argument<String>("buttonTextColor")

    val widthRatio = call.argument<Double>("widthRatio")
    val heightRatio = call.argument<Double>("heightRatio")

    val overrideGlobalRetryStrategy = call.argument<Boolean>("overrideGlobalRetryStrategy")!!
    val enableRetry = call.argument<Boolean>("enableRetry")!!
    val retryContent = call.argument<String>("retryContent")
    val retryUrl = call.argument<String>("retryUrl")


    val builder: UpdateManager.Builder = XUpdate.newBuild(mActivity!! )
      .isAutoMode(isAutoMode)
      .supportBackgroundUpdate(supportBackgroundUpdate)

    updatePromptStyle(
      builder,
      themeColor,
      topImageRes,
      buttonTextColor,
      widthRatio,
      heightRatio,
      overrideGlobalRetryStrategy,
      enableRetry,
      retryContent,
      retryUrl
    )

    builder.build().update(updateEntity)
  }

  /**
   * 更新弹窗的样式
   *
   * @param builder
   * @param themeColor                  主题颜色
   * @param topImageRes                 弹窗顶部的图片
   * @param buttonTextColor             按钮文字的颜色
   * @param widthRatio                  版本更新提示器宽度占屏幕的比例
   * @param heightRatio                 版本更新提示器高度占屏幕的比例
   * @param overrideGlobalRetryStrategy 是否覆盖全局的重试策略
   * @param enableRetry                 在下载过程中，如果点击了取消的话，是否弹出切换下载方式的重试提示弹窗
   * @param retryContent                重试提示弹窗的提示内容
   * @param retryUrl                    重试提示弹窗点击后跳转的url
   */
  private fun updatePromptStyle(
    builder: UpdateManager.Builder,
    themeColor: String?,
    topImageRes: String?,
    buttonTextColor: String?,
    widthRatio: Double?,
    heightRatio: Double?,
    overrideGlobalRetryStrategy: Boolean,
    enableRetry: Boolean,
    retryContent: String?,
    retryUrl: String?
  ) {
    if (!TextUtils.isEmpty(themeColor)) {
      builder.promptThemeColor(Color.parseColor(themeColor))
    }
    if (!TextUtils.isEmpty(topImageRes)) {
      val topImageResId = mActivity!!
        .resources.getIdentifier(topImageRes, "drawable", mActivity!!.packageName)
      builder.promptTopResId(topImageResId)
    }
    if (!TextUtils.isEmpty(buttonTextColor)) {
      builder.promptButtonTextColor(Color.parseColor(buttonTextColor))
    }
    if (widthRatio != null) {
      builder.promptWidthRatio(widthRatio.toFloat())
    }
    if (heightRatio != null) {
      builder.promptHeightRatio(heightRatio.toFloat())
    }
    if (overrideGlobalRetryStrategy) {
      builder.updateDownLoader(RetryUpdateDownloader(enableRetry, retryContent, retryUrl))
    }
  }


  /**
   * 显示重试提示弹窗
   *
   * @param call
   * @param result
   */
  private fun showRetryUpdateTipDialog(call: MethodCall, result: MethodChannel.Result) {
    val retryContent = call.argument<String>("retryContent")
    val retryUrl = call.argument<String>("retryUrl")

    RetryUpdateTipDialog.Companion.show(retryContent, retryUrl)
  }


  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    mActivity =binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
  }

  override fun onDetachedFromActivity() {
    mActivity = null
  }

  companion object {
    private const val PLUGIN_NAME = "com.xuexiang/flutter_xupdate"


  }
}
