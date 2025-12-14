package dev.yzq.flutter_xupdate.sample

import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    // 定义您的 Method Channel 名称，必须与 Dart 端使用的名称一致
    private val CHANNEL = "dev.yzq.flutter_xupdate/method"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        // 必须调用父类方法
        super.configureFlutterEngine(flutterEngine)

        // 实例化 MethodChannel
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            CHANNEL
        ).setMethodCallHandler { call, result ->

            // 检查调用的是哪个方法
            when (call.method) {
                "getDeviceName" -> {
                    // 假设我们有一个获取设备名称的逻辑
                    val deviceName = getDeviceName()

                    // 成功返回结果
                    result.success(deviceName)
                }

                "showAlert" -> {
                    // 示例：接收参数
                    val message = call.argument<String>("message")

                    // 执行原生操作，例如显示 Toast 或 Dialog
                    // ... showToast(message) ...

                    // 返回成功，如果不需要返回值，可以返回 null
                    result.success(null)
                }

                "update" -> {

                    UpdateChecker("eee92b8d90fc46c10c8f3934334154b7")
                        .check(
                            "5d3cf6afb2579de8cddf1eaa0e2b698d",
                            null ,
                            null,
                            null,
                            object : UpdateChecker.Callback {
                                override fun result(updateInfo: UpdateChecker.UpdateInfo?) {
                                   Log.i("----------------app",updateInfo.toString())
                                    result.success(updateInfo.toString())
                                }

                                override fun error(message: String?) {
                                    println(message)
                                    result.success("err")
                                }

                            }
                        );

                }

                else -> {
                    // 如果调用的方法未实现，返回未实现
                    result.notImplemented()
                }
            }
        }
    }

    // 示例：获取设备名称的私有方法
    private fun getDeviceName(): String {
        return android.os.Build.MODEL
    }
}