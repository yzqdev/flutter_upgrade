/*
 * Copyright (C) 2019 xuexiangjys(xuexiangjys@163.com)
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
 *
 */
package com.xuexiang.flutter_xupdate

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import com.xuexiang.xupdate.XUpdate

/**
 * 版本更新重试提示弹窗
 *
 * @author xuexiang
 * @since 2019-06-15 00:06
 */
class RetryUpdateTipDialog : AppCompatActivity(), DialogInterface.OnDismissListener {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)


    var content = intent.getStringExtra(KEY_CONTENT)
    val url = intent.getStringExtra(KEY_URL)

    if (TextUtils.isEmpty(content)) {
      content = getString(R.string.xupdate_retry_tip_dialog_content)
    }

    val dialog = AlertDialog.Builder(this)
      .setMessage(content)
      .setPositiveButton(
        android.R.string.yes
      ) { dialog, which ->
        dialog.dismiss()
        goWeb(url)
      }
      .setNegativeButton(android.R.string.no, null)
      .setCancelable(false)
      .show()
    dialog.setOnDismissListener(this)
  }

  /**
   * 以系统API的方式请求浏览器
   *
   * @param url
   */
  fun goWeb(url: String?) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
      startActivity(intent)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    finish()
  }

  companion object {
    const val KEY_CONTENT: String = "com.xuexiang.flutter_xupdate.KEY_CONTENT"
    const val KEY_URL: String = "com.xuexiang.flutter_xupdate.KEY_URL"


    /**
     * 显示版本更新重试提示弹窗
     *
     * @param content
     * @param url
     */
    fun show(content: String?, url: String?) {
      val intent = Intent(
        XUpdate.getContext(),
        RetryUpdateTipDialog::class.java
      )
      intent.putExtra(KEY_CONTENT, content)
      intent.putExtra(KEY_URL, url)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      XUpdate.getContext().startActivity(intent)
    }
  }
}
