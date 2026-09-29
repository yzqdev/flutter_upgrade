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

import com.xuexiang.xupdate.proxy.IUpdateHttpService
import com.xuexiang.xupdate.utils.UpdateLog
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 基于 OkHttp 实现的版本更新网络请求服务
 *
 * @author xuexiang
 * @since 2018/7/10 下午4:04
 */
class OKHttpUpdateHttpService(timeout: Int, private val mIsPostJson: Boolean) : IUpdateHttpService {

    /**
     * 下载请求，key为下载地址，用于取消下载
     */
    private val mDownloadCalls = ConcurrentHashMap<String, Call>()

    private val mClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
        .writeTimeout(5000L, TimeUnit.MILLISECONDS)
        .build()

    private val mMainHandler = Handler(Looper.getMainLooper())

    /**
     * 将结果回调切换至主线程, 保证上层(如MethodChannel)可以安全调用UI相关API
     */
    private fun postToMainThread(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            mMainHandler.post(runnable)
        }
    }

    init {
        UpdateLog.d("设置请求超时响应时间:" + timeout + "ms, 是否使用json:" + mIsPostJson)
    }

    override fun asyncGet(url: String, params: Map<String, Any>, callBack: IUpdateHttpService.Callback) {
        mClient.newCall(Request.Builder().url(buildGetUrl(url, params)).get().build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    postToMainThread { callBack.onError(e) }
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use { it.body?.string() ?: "" }
                    postToMainThread { callBack.onSuccess(result) }
                }
            })
    }

    override fun asyncPost(url: String, params: Map<String, Any>, callBack: IUpdateHttpService.Callback) {
        val body = if (mIsPostJson) {
            JSONObject(params).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        } else {
            FormBody.Builder().apply {
                for ((key, value) in params) {
                    add(key, value?.toString() ?: "")
                }
            }.build()
        }
        mClient.newCall(Request.Builder().url(url).post(body).build())
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    postToMainThread { callBack.onError(e) }
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use { it.body?.string() ?: "" }
                    postToMainThread { callBack.onSuccess(result) }
                }
            })
    }

    override fun download(url: String, path: String, fileName: String, callback: IUpdateHttpService.DownloadCallback) {
        val call = mClient.newCall(Request.Builder().url(url).get().build())
        mDownloadCalls[url] = call
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mDownloadCalls.remove(url)
                if (!call.isCanceled()) {
                    postToMainThread { callback.onError(e) }
                }
            }

            override fun onResponse(call: Call, response: Response) {
                mDownloadCalls.remove(url)
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    postToMainThread { callback.onError(IOException("下载失败, HTTP响应码:" + code)) }
                    return
                }
                postToMainThread { callback.onStart() }
                try {
                    val dir = File(path)
                    if (!dir.exists()) {
                        dir.mkdirs()
                    }
                    val file = saveFile(response, call, File(dir, fileName)) { rate, total ->
                        postToMainThread { callbackProgress(callback, rate, total) }
                    }
                    postToMainThread { callback.onSuccess(file) }
                } catch (e: Exception) {
                    postToMainThread { callback.onError(e) }
                }
            }
        })
    }

    override fun cancelDownload(url: String) {
        UpdateLog.d("取消下载, url:" + url)
        mDownloadCalls.remove(url)?.cancel()
    }

    /**
     * 构建get请求的url
     */
    private fun buildGetUrl(url: String, params: Map<String, Any>): String {
        if (params.isEmpty()) {
            return url
        }
        val sb = StringBuilder(url)
        sb.append(if (url.contains('?')) '&' else '?')
        var isFirst = true
        for ((key, value) in params) {
            if (!isFirst) {
                sb.append('&')
            }
            isFirst = false
            sb.append(URLEncoder.encode(key, "UTF-8"))
                .append('=')
                .append(URLEncoder.encode(value?.toString() ?: "", "UTF-8"))
        }
        return sb.toString()
    }

    /**
     * 将下载内容保存至文件
     */
    @Throws(IOException::class)
    private fun saveFile(
        response: Response,
        call: Call,
        file: File,
        onProgress: (Int, Long) -> Unit
    ): File {
        val body = response.body ?: throw IOException("下载失败, 响应内容为空")
        val contentLength = body.contentLength()
        body.byteStream().use { input ->
            FileOutputStream(file).use { output ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                var written: Long = 0
                var lastRate = 0
                while (true) {
                    if (call.isCanceled()) {
                        throw IOException("下载已取消")
                    }
                    val read = input.read(buf)
                    if (read == -1) {
                        break
                    }
                    output.write(buf, 0, read)
                    written += read
                    if (contentLength > 0) {
                        val rate = (written * 100 / contentLength).toInt()
                        if (rate != lastRate) {
                            lastRate = rate
                            onProgress(rate, contentLength)
                        }
                    }
                }
                output.flush()
                if (contentLength <= 0) {
                    //响应长度未知时, 下载完成补一次100%的进度回调
                    onProgress(100, 0)
                }
            }
        }
        return file
    }

    private fun callbackProgress(callback: IUpdateHttpService.DownloadCallback, rate: Int, total: Long) {
        UpdateLog.d("下载进度: $rate%")
        callback.onProgress(rate / 100.0f, if (total > 0) total else 0)
    }
}
