package com.whitesev.gm

import android.graphics.Bitmap
import android.webkit.WebViewClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.whitesev.gm.Main.Companion.binding
import com.whitesev.gm.serviceImpl.GMServiceImpl
import com.whitesev.gm.utils.LogUtils
import com.whitesev.gm.utils.Utils
import okhttp3.Cookie
import okhttp3.Headers.Companion.toHeaders
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.IOException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection

class WVViewClient : WebViewClient() {
    private var cookieJar: PersistentCookieJar
    private var isStart = false
    private var scriptsText = ""
    private var scriptsList = listOf(
        "js/jquery3.4.1.js",
        "js/any-touch.umd.js",
        "js/Viewer.js",
        "js/NZMsgBox.js",
        "js/Xtiper.js",
        "js/js-watermark.js",
        "js/GM_html2canvas.js",
        "js/Utils.js",
        "MT论坛.js"
    )
    private val webView: WebView = binding.webView

    init {
        scriptsText = GMServiceImpl().getLocalScriptContent(scriptsList)
        cookieJar =
            PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(webView.context))
        cookieJar.clear()
    }

    // document-start
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        LogUtils.info("document-start")
        Main.userChooseFileMutableMap.clear()
        if (!isStart && Utils.isMatch(webView.url.toString(), "http*://bbs.binmt.cc/*")) {
            GMServiceImpl().initGMApi()
            view?.evaluateJavascript(
                """
            (() => {
                document.addEventListener(
                    "DOMContentLoaded",
                    function () {
                        document.removeEventListener("DOMContentLoaded", arguments.callee, false);
                        (() => {
                            $scriptsText;
                        })();
                    },
                    false
                );
            })();  
            """.trimIndent(), null
            )
        }
        isStart = true
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        LogUtils.info("document-ready")
        isStart = false

    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?, request: WebResourceRequest?
    ): Boolean {
        return false
    }

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        if ((request != null) && (request.url != null) && (request.method.lowercase() == "get")) {
            // GET请求
            val scheme = request.url.scheme!!.trim()
            if (scheme.equals("http", ignoreCase = true) || scheme.equals(
                    "https", ignoreCase = true
                )
            ) {
                if (!request.requestHeaders.toHeaders()["Accept"].isNullOrEmpty() && request.requestHeaders.toHeaders()["Accept"]?.startsWith(
                        "text/html"
                    ) == true
                ) {
                    LogUtils.info("${request.url}   ${request.isForMainFrame}")
                    // 暂不实现多个webView管理，麻烦不想写
//                    view?.post {
//                        view.webViewClient = WVViewClient()
//                    }
                }
                return executeRequest(request.url.toString())
            }
        }
        return null
    }

    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        if (url != null) {
            return executeRequest(url)
        }
        return null
    }

    /***
     * 处理请求，保存Cookie
     *
     * */
    private fun executeRequest(url: String): WebResourceResponse? {
        //LogUtils.info("处理GET请求：${url}")
        try {
            val connection: URLConnection = URL(url).openConnection()
            val cookie = connection.headerFields["Set-Cookie"]
            if (!cookie.isNullOrEmpty()) {
                val cookieList = mutableListOf<Cookie?>()
                cookie.toList().forEach {
                    cookieList.add(Cookie.parse(url.toHttpUrl(), it))
                    Cookie.parse(url.toHttpUrl(), it)
                }
                LogUtils.info(cookieList.toString())
                cookieJar.saveFromResponse(url.toHttpUrl(), cookieList.toList() as List<Cookie>)
            }
            return null
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}