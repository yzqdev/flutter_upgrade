/*
 * Copyright (C) 2018 xuexiangjys(xuexiangjys@163.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xuexiang.flutter_xupdate

import com.google.gson.Gson
import com.hjq.http.EasyConfig
import com.hjq.http.EasyHttp
import com.hjq.http.config.IRequestApi
import com.hjq.http.config.IRequestHandler
import com.hjq.http.config.IRequestServer
import com.hjq.http.lifecycle.ApplicationLifecycle
import com.hjq.http.listener.OnDownloadListener
import com.hjq.http.listener.OnHttpListener
import com.hjq.http.model.HttpMethod
import com.hjq.http.request.HttpRequest
import com.xuexiang.xupdate.logs.UpdateLog
import com.xuexiang.xupdate.proxy.IUpdateHttpService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.File
import java.lang.reflect.Type
import java.util.TreeMap
import java.util.concurrent.TimeUnit

class RequestHandler : IRequestHandler {
  override fun requestSuccess(httpRequest: HttpRequest<*>, response: Response, type: Type): Any {
    return response.body!!.string()
  }

  override fun requestFail(httpRequest: HttpRequest<*>, e: Throwable): Throwable {
    return e
  }

}
class FakeServer:IRequestServer{
  override fun getHost(): String {
    return  "https://www.wanandroid.com/"
  }

}
open class TestServer : IRequestServer {


  override fun getHost(): String {
    return ""
  }
}
class DefaultApi(private val url:String):IRequestApi,TestServer(){
  override fun getApi(): String {
    return url
  }

}

/**
 * 使用okhttp
 *
 * @author xuexiang
 * @since 2018/7/10 下午4:04
 */
class OKHttpUpdateHttpService(timeout: Int, private val mIsPostJson: Boolean) : IUpdateHttpService {
  constructor(isPostJson: Boolean) : this(20000, isPostJson)

  /**
   * 构造方法
   *
   * @param timeout    请求超时响应时间
   * @param isPostJson 是否使用json
   */


  init {
    val builder: OkHttpClient.Builder = OkHttpClient.Builder()
    builder.readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
    builder.writeTimeout(5000, TimeUnit.MILLISECONDS)
    builder.connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)

    EasyConfig.with(builder.build()).setServer(FakeServer()).setHandler(RequestHandler())
      .into()
    UpdateLog.d("设置请求超时响应时间:" + timeout + "ms, 是否使用json:" + mIsPostJson)
  }

  override fun asyncGet(
    url: String,
    params: Map<String, Any>,
    callBack: IUpdateHttpService.Callback
  ) {
    EasyHttp.get(ApplicationLifecycle.getInstance()).api(DefaultApi(url))
      .request(object : OnHttpListener<String> {
        override fun onHttpSuccess(result: String) {
          callBack.onSuccess(result)
        }

        override fun onHttpFail(e: Throwable) {
          callBack.onError(e)
        }
      })


  }

  override fun asyncPost(
    url: String,
    params: Map<String, Any>,
    callBack: IUpdateHttpService.Callback
  ) {
    val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    //这里默认post的是Form格式，使用json格式的请修改 post -> postString
    if (mIsPostJson) {

      val body: RequestBody = Gson().toJson(params).toRequestBody(JSON)
      EasyHttp.post(ApplicationLifecycle.getInstance()).api(DefaultApi(url)).body(body)
        .request(object : OnHttpListener<String> {
          override fun onHttpSuccess(result: String) {
            callBack.onSuccess(result)
          }

          override fun onHttpFail(e: Throwable) {
            callBack.onError(e)
          }

        })


    } else {

    }

  }

  override fun download(
    url: String,
    path: String,
    fileName: String,
    callback: IUpdateHttpService.DownloadCallback
  ) {


    EasyHttp.download(ApplicationLifecycle.getInstance()).method(HttpMethod.GET)
      .url(url).tag(url).listener(object : OnDownloadListener {
        override fun onDownloadStart(file: File) {
          super.onDownloadStart(file)
          callback.onStart()
        }

        override fun onDownloadProgressChange(file: File, progress: Int) {
          callback.onProgress((progress / 100).toFloat(), file.totalSpace)
        }

        override fun onDownloadSuccess(file: File) {
          callback.onSuccess(file)
        }

        override fun onDownloadFail(file: File, throwable: Throwable) {
          callback.onError(throwable)
        }
      }).start()

  }

  override fun cancelDownload(url: String) {
    EasyHttp.cancelByTag(url)

  }

  private fun transform(params: Map<String, Any>): Map<String, String> {
    val map: MutableMap<String, String> = TreeMap()
    for ((key, value) in params) {
      map[key] = value.toString()
    }
    return map
  }
}