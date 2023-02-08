package com.whitesev.gm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.whitesev.gm.Main.Companion.uploadFiles
import com.whitesev.gm.Main.Companion.userChooseFileMutableMap
import com.whitesev.gm.utils.FileUtils
import java.io.File


class WVChromeClient : WebChromeClient() {
    companion object {
        @JvmStatic
        val CHOOSER_REQUEST = 0x33
    }

    override fun onJsAlert(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        return false
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        uploadFiles = filePathCallback
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*" // 设置文件类型

        val mimeTypes = arrayOf("image/*,audio/*,video/*,*/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes) // 设置多种类型

        intent.addCategory(Intent.CATEGORY_OPENABLE)
        Main().startActivityForResult(Intent.createChooser(intent, "File Chooser"), CHOOSER_REQUEST)
        return true
    }

    // 文件选择回调（在 Main.kt 的 onActivityResult中调用此方法）
    fun onActivityResultFileChooser(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != CHOOSER_REQUEST || uploadFiles == null) return
        var results: Array<Uri> = arrayOf()
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        results[i] = item.uri
                    }

                }
                if (dataString != null) {
                    val realFilePath = FileUtils.getFilePathByUri(
                        Main.binding.webView.context, Uri.parse(dataString)
                    )
                    val fileName = File(realFilePath).name
                    userChooseFileMutableMap[fileName] = intent
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        uploadFiles!!.onReceiveValue(results)
        uploadFiles = null
    }
}