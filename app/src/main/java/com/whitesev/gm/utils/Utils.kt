package com.whitesev.gm.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream


object Utils {

    /**
     * 获取网页内容
     * @param url 需要请求的网页
     * **/
    fun getUrlPageContext(url: String): String {
        val request = Request.Builder().url(url).method("GET", null).build()
        val requestCall = OkHttpClient().newCall(request)
        return try {
            val response = requestCall.execute()
            response.body!!.string()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 设置请求头。userAgent和cookie
     * @param headersParams 请求头字典
     * @param webViewUserAgent 用户代理字符串
     * @param cookies
     * **/
    fun setHeaders(
        headersParams: Map<String, Any>, webViewUserAgent: String, cookies: String
    ): Headers {
        val headers: Headers?
        val headersBuilder = Headers.Builder()
        if (headersParams.isNotEmpty()) {
            val iterator = headersParams.keys.iterator()
            while (iterator.hasNext()) {
                val key = iterator.next()
                headersBuilder.add(key.lowercase(), headersParams[key].toString())
            }
            if (headersBuilder["user-agent"] == null) {
                LogUtils.info("User-Agent为空，设置webView的")
                LogUtils.info(webViewUserAgent)
                headersBuilder.add("User-Agent", webViewUserAgent)
            }
            if (headersBuilder["cookie"].isNullOrEmpty() && cookies.isNotEmpty()) {
                LogUtils.info("Cookie为空，设置Cookie")
                headersBuilder["cookie"] = cookies
                headersBuilder["cookie"] = handleCookieStr(headersBuilder["cookie"].toString())
            } else if (headersBuilder.equals("cookie") && cookies.isNotEmpty()) {
                LogUtils.info("Cookie不为空，添加cookie")
                headersBuilder["cookie"] += cookies
                headersBuilder["cookie"] = handleCookieStr(headersBuilder["cookie"].toString())
            }
        }
        headers = headersBuilder.build()

        return headers
    }

    /**
     * 将cookie列表，格式化（key=val;key=val....）为cookie字符串
     * @param cookies
     * **/
    fun cookieHeader(cookies: List<Cookie>): String {
        val cookieHeader = StringBuilder()
        cookies.forEach {
            LogUtils.info("添加cookie：" + it.name)
            cookieHeader.append("${it.name}=${it.value};")
        }
        return cookieHeader.toString()
    }

    /**
     * 将cookie字符串进行去重处理
     * @param cookies
     * **/
    private fun handleCookieStr(cookies: String): String {
        if (cookies == "" || cookies == "null") {
            return ""
        }
        LogUtils.info("进行cookie去重处理")
        val cookiesMap = mutableMapOf<String, String>()
        var cookiesText = ""
        cookies.split(";").forEach continuing@{ result ->
            if (result.trim() == "" || result.trim() == "null") {
                return@continuing
            }
            LogUtils.info(result)
            val keyPattern = "^(.+?)=".toRegex(RegexOption.IGNORE_CASE)
            val key = keyPattern.find((result.trim()))?.groupValues!!.toList()[1].trim()
            val value = result.trim().replace(keyPattern, "").trim()
            if (cookiesMap[key].isNullOrEmpty()) {
                cookiesMap[key] = value
                cookiesText += "$key=$value;"
            } else {
                LogUtils.info("该Cookie已存在")
            }
        }
        return cookiesText
    }

    /**
     * 把base64转为File对象，存在cache目录
     * @param context
     * @param fileName 文件名
     * @param base64Code base64格式的字符串
     * externalCacheDir    /storage/emulated/0/Android/data/com.whitesev.gm/cache
     * cacheDir            /data/user/0/com.whitesev.gm/cache
     * codeCacheDir        /data/user/0/com.whitesev.gm/code_cache
     * dataDir             /data/user/0/com.whitesev.gm
     * filesDir            /data/user/0/com.whitesev.gm/files
     * noBackupFilesDir    /data/user/0/com.whitesev.gm/no_backup
     * obbDir              /storage/emulated/0/Android/obb/com.whitesev.gm
     **/
    fun cacheBase64ToFile(context: Context, fileName: String, base64Code: String): File {
        LogUtils.info("externalCacheDir ${context.externalCacheDir?.path}")
        LogUtils.info("cacheDir ${context.cacheDir?.path}")
        LogUtils.info("codeCacheDir ${context.codeCacheDir?.path}")
        LogUtils.info("dataDir ${context.dataDir?.path}")
        LogUtils.info("filesDir ${context.filesDir?.path}")
        LogUtils.info("noBackupFilesDir ${context.noBackupFilesDir?.path}")
        LogUtils.info("obbDir ${context.obbDir?.path}")

        val dir = context.externalCacheDir?.path
        val savePath = "$dir/$fileName"
        val buffer: ByteArray = Base64.decode(base64Code, Base64.DEFAULT)
        val out = FileOutputStream(savePath)
        out.write(buffer)
        out.close()
        return File(savePath)
    }


    /**
     * 由于POST上传大文件会导致OOM，因此，要创建一个 BodyRequest 形成一个 Uri 避免任何 OutOfMemoryError 由于大文件创建它
     * @param context
     * @param intent
     * **/
    fun getFilePart(context: Context, intent: Intent): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType {
                return "multipart/form-data".toMediaType()
            }

            @SuppressLint("Recycle")
            override fun writeTo(sink: BufferedSink) {
                val mediaUri = Uri.parse(intent.dataString)
                context.contentResolver.openInputStream(mediaUri)!!.source().use { source ->
                    sink.writeAll(
                        source
                    )
                }
            }

        }
    }

    /**
     * 通配符匹配-贪心解法
     *
     * **/
    fun isMatch(s: String, p: String): Boolean {
        var sRight = s.length
        var pRight = p.length

        val charMatch = { u: Char, v: Char -> u == v || v == '?' }
        val allStars = { word: String, left: Int, right: Int ->
            word.subSequence(left, right).all {
                it == '*'
            }
        }

        while (sRight > 0 && pRight > 0 && p[pRight - 1] != '*') {
            if (charMatch(s[sRight - 1], p[pRight - 1])) {
                --sRight
                --pRight
            } else {
                return false
            }
        }
        if (pRight == 0) {
            return sRight == 0
        }
        var sIndex = 0
        var pIndex = 0
        var sRecord = -1
        var pRecord = -1
        while (sIndex < sRight && pIndex < pRight) {
            if (p[pIndex] == '*') {
                ++pIndex
                sRecord = sIndex
                pRecord = pIndex
            } else if (charMatch(s[sIndex], p[pIndex])) {
                ++sIndex
                ++pIndex
            } else if (sRecord != -1 && sRecord + 1 < sRight) {
                ++sRecord
                sIndex = sRecord
                pIndex = pRecord
            } else {
                return false
            }
        }
        return allStars(p, pIndex, pRight)
    }


}