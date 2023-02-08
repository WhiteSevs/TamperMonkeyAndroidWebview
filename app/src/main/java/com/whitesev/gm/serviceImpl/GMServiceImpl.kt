package com.whitesev.gm.serviceImpl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import com.alibaba.fastjson.JSONObject
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.whitesev.gm.Main.Companion.binding
import com.whitesev.gm.Main.Companion.menuData
import com.whitesev.gm.Main.Companion.userChooseFileMutableMap
import com.whitesev.gm.okhttp3.MultipartBody
import com.whitesev.gm.service.GMService
import com.whitesev.gm.utils.AssetsUtils
import com.whitesev.gm.utils.LogUtils
import com.whitesev.gm.utils.Utils
import okhttp3.Response
import okhttp3.RequestBody
import okhttp3.Headers
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class GMServiceImpl : GMService() {
    private val webView: WebView = binding.webView
    private var webViewUserAgent = "okhttp/4.10.0"
    private val client = OkHttpClient()
    val cookieJar =
        PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(webView.context))
    private val clientBuilder = client.newBuilder()
    private val requestBuilder = Request.Builder()

    init {
        webViewUserAgent = webView.settings.userAgentString

    }

    /**
     * 初始化页面GM API
     * */
    override fun initGMApi() {
        webView.evaluateJavascript(getGMApi(), null)
    }

    /**
     * 加载油猴脚本-从网络
     * */
    override fun loadNetWorkScript(url: String) {
        val request = Request.Builder().url(url).method("GET", null).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Toast.makeText(webView.context, "获取油猴脚本:${url}失败", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                this@GMServiceImpl.parseScript(response.body!!.string())
            }

        })
    }

    override fun getLocalScriptContent(fileList: List<String>): String {
        var scriptsText = ""
        fileList.forEach {
            LogUtils.info("加载js $it")
            scriptsText += AssetsUtils.getFileText(webView.context, it)
            scriptsText += "\n"
        }
        return scriptsText
    }

    /**
     * 解析油猴脚本-从内容
     * */
    override fun parseScript(scriptText: String) {
        try {
            val userScriptPattern =
                "//\\s*==UserScript==\\s*([\\s\\S]+?)//\\s*==/UserScript==".toRegex(RegexOption.IGNORE_CASE)
            if (userScriptPattern.containsMatchIn(scriptText)) {
                var userScript = userScriptPattern.find(scriptText)!!.value
                userScript = userScriptPattern.matchEntire(userScript)!!.groups[1]!!.value
                var gmScriptString = "" // 油猴脚本的所有字符串
                "\n".toRegex().split(userScript).forEach {
                    val keyMatch = "//\\s+@(.+?)\\s+".toRegex(RegexOption.IGNORE_CASE)
                        .find(it, 0)?.groupValues // key正则
                    val valueMatch = "//\\s+@.+\\s+(.+)".toRegex(RegexOption.IGNORE_CASE)
                        .find(it, 0)?.groupValues // value正则

                    if ((keyMatch != null && keyMatch.toList().size > 1) && (valueMatch != null && valueMatch.toList().size > 1)) {
                        val key = keyMatch.toList()[1]
                        val value = valueMatch.toList()[1]
                        if (key.lowercase() == "require") {
                            LogUtils.info("require请求的网址：$value")
                            gmScriptString += Utils.getUrlPageContext(value)
                            gmScriptString += "\n"
                        }
                    }
                }
                if (gmScriptString != "") {
                    gmScriptString += scriptText
                    webView.post {
                        webView.evaluateJavascript(
                            """
                        (()=>{
                            $gmScriptString
                        })()""".trimMargin(), null
                        )
                    }

                }
            } else {
                LogUtils.info("油猴UserScript解析失败")
                Thread {
                    Looper.prepare()
                    Toast.makeText(
                        webView.context.applicationContext, "油猴UserScript解析失败", Toast.LENGTH_SHORT
                    ).show()
                    Looper.loop()
                }.start()
            }


        } catch (e: java.lang.Exception) {
            LogUtils.info(e.toString())
        }

    }

    /**
     * GM API GM_xmlhttpRequest
     * 注意，当以POST传输很大的base64，如80MB的，那么fastjson直接使用parseObject就会OOM，所以使用JSONReader按顺序读取
     * */
    @JavascriptInterface
    override fun GM_xmlhttpRequest(options: String) {
        LogUtils.info("JS调用 GM_xmlhttpRequest")
        LogUtils.info(options, false)
        val optionsJSON = JSONObject.parse(options) as JSONObject
        var url = optionsJSON["url"].toString() // 请求的URL
        var data = optionsJSON["data"] // 请求携带的数据包 get的param或post的json
        val timeout = optionsJSON["timeout"].toString().toLong() // 设置超时时间
        val method = optionsJSON["method"].toString()// 设置请求方法
        val headers = optionsJSON["headers"] as JSONObject // 请求头
        val responseType = optionsJSON["responseType"].toString()// 设置响应文件类型
        val guid = optionsJSON["guid"].toString()// 回调id
        val from = optionsJSON["from"].toString() // 来自top或者iframe
        val cookie = optionsJSON["cookie"].toString().trim() // 传递的cookie
        LogUtils.info("超时时间：$timeout")
        val onLoadCallBack =
            if (from.lowercase() == "top") "window._GM_.callback.GM_xmlhttpRequest.onload['${guid}'](参数)" else "window._GM_.callback.GM_xmlhttpRequest_iframe.onload['${guid}'](参数)"
        val onErrorCallBack =
            if (from.lowercase() == "top") "window._GM_.callback.GM_xmlhttpRequest.onerror['${guid}'](参数)" else "window._GM_.callback.GM_xmlhttpRequest_iframe.onerror['${guid}'](参数)"
        val onTimeoutCallBack =
            if (from.lowercase() == "top") "window._GM_.callback.GM_xmlhttpRequest.ontimeout['${guid}'](参数)" else "window._GM_.callback.GM_xmlhttpRequest_iframe.ontimeout['${guid}'](参数)"
        val loadCookie = cookieJar.loadForRequest(url.toHttpUrl())
        var needSetCookie = Utils.cookieHeader(loadCookie)
        needSetCookie += CookieManager.getInstance().getCookie(url)
        if (cookie != "") {
            needSetCookie += if (needSetCookie.trim().endsWith(";")) {
                cookie
            } else {
                ";$cookie"
            }
        }

        LogUtils.info("needSetCookie：$needSetCookie")
        val requestHeaders: Headers = Utils.setHeaders(headers, webViewUserAgent, needSetCookie)

        if ("get" == method.lowercase()) {
            // GET请求
            data = data.toString()
            if (!((data.isEmpty()) || (data != ""))) {
                url += "?$data"
            }
            xmlHttpRequestGet(
                url,
                timeout,
                responseType,
                requestHeaders,
                onLoadCallBack,
                onTimeoutCallBack,
                onErrorCallBack
            )


        } else if ("post" == method.lowercase()) {
            // POST请求

            xmlHttpRequestPost(
                url,
                timeout,
                responseType,
                requestHeaders,
                data as JSONObject,
                optionsJSON["files"] as JSONObject,
                onLoadCallBack,
                onTimeoutCallBack,
                onErrorCallBack
            )

        }
    }

    /**
     * GM API GM_xmlhttpRequest的 GET请求
     * */
    private fun xmlHttpRequestGet(
        url: String,
        timeout: Long,
        responseType: String,
        headers: Headers,
        onLoadCallBack: String,
        onTimeoutCallBack: String,
        onErrorCallBack: String
    ) {
        LogUtils.info("请求的headers：$headers", true)
        val request = requestBuilder.url(url).method("GET", null).headers(headers).build()
        clientBuilder.cookieJar(cookieJar)
        clientBuilder.readTimeout(timeout, TimeUnit.MILLISECONDS)
        clientBuilder.connectTimeout(timeout, TimeUnit.MILLISECONDS)
        //创建call并调用enqueue()方法实现网络请求
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogUtils.info("错误：" + e.printStackTrace().toString())
                val gmCallback: String =
                    if (e.toString().startsWith("java.net.UnknownHostException") || e.toString()
                            .startsWith("java.net.ConnectException")
                    ) {
                        // 连接超时
                        onTimeoutCallBack.replace(
                            "参数", ""
                        )
                    } else {
                        onErrorCallBack.replace(
                            "参数", ""
                        )
                    }
                webView.post {
                    LogUtils.info("webView调用${gmCallback}")
                    webView.evaluateJavascript(
                        gmCallback, null
                    )
                    LogUtils.info("失败响应")
                    LogUtils.info(e.toString())
                }


            }

            override fun onResponse(call: Call, response: Response) {
                val httpUrl = url.toHttpUrl()
                val cookieList =
                    Cookie.parseAll(httpUrl, response.headers) // 解析cookie保存到SharedPreferences
                if (response.priorResponse != null) {
                    // 请求重定向
                    LogUtils.info( "发生重定向"+response.priorResponse!!.code.toString())
                    val networkResponse = response.priorResponse!!.networkResponse
                    val redirectHeaders = networkResponse!!.headers
                    cookieJar.saveFromResponse(httpUrl, Cookie.parseAll(httpUrl, redirectHeaders))
                }
                LogUtils.info("onResponse")
                // body字符串
                val resultBytes = response.body!!.bytes()
                val result = String(resultBytes)
                LogUtils.info(result)
                // 动态添加headers集合
                val headersJSON = mutableMapOf<String, String>()
                response.headers.forEach {
                    headersJSON[it.first] = it.second
                }
                cookieJar.saveFromResponse(httpUrl, cookieList)
                webView.post {
                    val paramJSON = mutableMapOf(
                        "finalUrl" to response.request.url.toString(),
                        "readyState" to 4,
                        "status" to response.code,
                        "statusText" to "OK", // 状态码对应的解释？
                        "responseHeaders" to response.headers.toString(),
                        "response" to "", // js将responseText转换成Node类型
                        "responseXML" to "", // js将responseText转换成XML类型
                        "responseText" to result,
                    )

                    if (responseType.lowercase() == "arraybuffer") {
                        paramJSON["responseBase64"] =
                            Base64.encodeToString(resultBytes, Base64.NO_WRAP)
                    }
                    val paramString = JSONObject.toJSONString(paramJSON)
                    val gmCallback = onLoadCallBack.replace(
                        "参数", paramString
                    )
                    webView.evaluateJavascript(
                        gmCallback, null
                    )
                }
            }
        })
    }

    /**
    * GM API GM_xmlhttpRequest 的POST请求
    * */
    private fun xmlHttpRequestPost(
        url: String,
        timeout: Long,
        responseType: String,
        headers: Headers,
        data: JSONObject,
        files: JSONObject,
        onLoadCallBack: String,
        onTimeoutCallBack: String,
        onErrorCallBack: String
    ) {
        LogUtils.info("请求headers：$headers", true)
        clientBuilder.readTimeout(timeout, TimeUnit.MILLISECONDS)
        clientBuilder.connectTimeout(timeout, TimeUnit.MILLISECONDS)
        var requestBody: RequestBody? = null
        if (!headers["content-type"].isNullOrEmpty()) {
            // 如果存在这个
            val contentType = headers["content-type"]
            if (contentType?.lowercase()?.indexOf("application/x-www-form-urlencoded") != -1) {
                // 表单类型
                // 循环form表单，将表单内容添加到form builder中
                LogUtils.info("当前POST类型为 表单")
                val builder: FormBody.Builder = FormBody.Builder()
                data.forEach { s, any ->
                    LogUtils.info("添加表单参数key：$s")
                    LogUtils.info("添加表单参数value：$any")
                    builder.add(s, any.toString())
                }
                requestBody = builder.build()

            } else if (contentType.lowercase().indexOf("application/json") != -1) {
                // json类型
                LogUtils.info("当前POST类型为 json类型")
                requestBody = data.toJSONString().toRequestBody()
            } else {
                LogUtils.info("其它的Content-Type：$contentType")
            }
        } else {
            // 默认 multipart/form-data ，注意，宝塔有个设置里不带Content-length
            LogUtils.info("当前POST类型为 multipart/form-data类型")
            val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            data.forEach { key, value ->
                if (value.toString().startsWith("isFileInOptionsFiles_")) {
                    // 判断该值是否是js的File对象
                    val fileJSON = files[value.toString()] as JSONObject
                    val fileName = fileJSON["fileName"].toString() // 文件名
                    // val fileType = fileJSON["fileType"].toString() // 文件类型
                    // val fileSize = fileJSON["fileSize"] // 文件大小
                    val fileIntent = userChooseFileMutableMap[fileName] as Intent

                    val fileBody = Utils.getFilePart(webView.context, fileIntent)
                    multipartBody.addFormDataPart(
                        key, fileName, fileBody
                    )
                } else {
                    LogUtils.info("multipart/form-data 添加键：$key")
                    LogUtils.info("multipart/form-data 添加值：$value")
                    multipartBody.addFormDataPart(key, value.toString())
                }
            }

            requestBody = multipartBody.build()
        }

        val request =
            requestBuilder.url(url).method("POST", body = requestBody).headers(headers).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                LogUtils.info("onFailure " + e.printStackTrace().toString())
                val gmCallback: String =
                    if (e.toString().startsWith("java.net.UnknownHostException") || e.toString()
                            .startsWith("java.net.ConnectException")
                    ) {
                        // 连接超时
                        onTimeoutCallBack.replace(
                            "参数", ""
                        )
                    } else {
                        onErrorCallBack.replace(
                            "参数", ""
                        )
                    }
                webView.post {
                    LogUtils.info("webView调用${gmCallback}")
                    webView.evaluateJavascript(
                        gmCallback, null
                    )
                    LogUtils.info("onFailure 失败响应")
                    LogUtils.info("onFailure $e")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val httpUrl = url.toHttpUrl()
                val cookieList =
                    Cookie.parseAll(httpUrl, response.headers) // 解析cookie保存到SharedPreferences
                if (response.priorResponse != null) {
                    // 请求重定向
                    LogUtils.info(
                        "该请求发生重定向,code：" + response.priorResponse!!.code.toString()
                    )
                    val networkResponse = response.priorResponse!!.networkResponse
                    val redirectHeaders = networkResponse!!.headers
                    cookieJar.saveFromResponse(httpUrl, Cookie.parseAll(httpUrl, redirectHeaders))
                }

                cookieJar.saveFromResponse(httpUrl, cookieList)
                LogUtils.info("onResponse 成功响应")
                // body字符串
                val resultBytes = response.body!!.bytes()
                val result = String(resultBytes)
                LogUtils.info(result)
                val headersJSON = mutableMapOf<String, String>()
                response.headers.forEach {
                    headersJSON[it.first] = it.second
                }

                webView.post {
                    val paramJSON = mutableMapOf(
                        "finalUrl" to response.request.url.toString(),
                        "readyState" to 4,
                        "status" to response.code,
                        "statusText" to "OK", // 状态码对应的解释？
                        "responseHeaders" to response.headers.toString(),
                        "response" to "", // js将responseText转换成Node类型
                        "responseXML" to "", // js将responseText转换成XML类型
                        "responseText" to result

                    )

                    if (responseType.lowercase() == "arraybuffer") {
                        paramJSON["responseBase64"] =
                            Base64.encodeToString(resultBytes, Base64.NO_WRAP)
                    }
                    val paramString = JSONObject.toJSONString(paramJSON)
                    val gmCallback = onLoadCallBack.replace(
                        "参数", paramString
                    )
                    webView.evaluateJavascript(
                        gmCallback, null
                    )
                }
            }

        })
        //创建call并调用enqueue()方法实现网络请求
    }


    @JavascriptInterface
    override fun GM_registerMenuCommand(options: String) {
        val optionsJSON = JSONObject.parseObject(options)
        val guid = optionsJSON["guid"] as String
        val showText = optionsJSON["showText"]
        menuData.add(JSONObject.parseObject("""{'guid':"$guid",'showText':"$showText"}"""))
    }

    @JavascriptInterface
    override fun GM_unregisterMenuCommand(guid: String) {
        LogUtils.info(guid)
        menuData.forEach {
            if (it["guid"]?.equals(guid) == true) {
                menuData.remove(it)
                LogUtils.info("删除${it.toJSONString()}")
                return
            }
        }
    }

    @JavascriptInterface
    override fun getScriptText(): String {
        var scriptText = getLocalScriptContent(
            listOf(
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
        )
        scriptText = """
                document.addEventListener(
                    "DOMContentLoaded",
                    function () {
                        document.removeEventListener("DOMContentLoaded", arguments.callee, false);
                        (() => {
                            $scriptText;
                        })();
                    },
                    false
                );
            """.trimIndent()
        return scriptText
    }

    @JavascriptInterface
    override fun getIframeScriptText(): String {
        return """
            (()=>{
            ${
            getLocalScriptContent(
                listOf(
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
            )
        }
            })()
        """.trimIndent()
    }

    @JavascriptInterface
    override fun getGMApi(): String {
        return AssetsUtils.getFileText(webView.context, "GM.js")
    }

    @JavascriptInterface
    override fun getGMIframeApi(): String {
        return AssetsUtils.getFileText(webView.context, "GMIframe.js")
    }

    @JavascriptInterface
    override fun GM_setClipboard(text: String) {
        val clipData = ClipData.newPlainText("newPlainTextLabel", text)
        val clipboardManager =
            webView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(clipData)
    }
}