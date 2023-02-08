package com.whitesev.gm

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.webkit.ValueCallback
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebSettings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSONObject
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.whitesev.gm.databinding.ActivityMainBinding
import com.whitesev.gm.serviceImpl.GMServiceImpl
import com.whitesev.gm.utils.CrashHandler
import com.whitesev.gm.utils.LogUtils


class Main : AppCompatActivity() {
    init {
        // 保留cookies，而不是在随后的重新启动后清除
        CookieManager.getInstance().setAcceptCookie(true)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityMainBinding
        var menuData: MutableList<JSONObject> = mutableListOf(
            JSONObject.parseObject("""{'guid':'refresh_website','showText':'刷新网页'}"""),
            JSONObject.parseObject("""{'guid':'goto_log_view','showText':'进入日志界面'}"""),
            JSONObject.parseObject("""{'guid': 'request_permission','showText':'获取权限'}""")

        ) // 右上角按钮的数据菜单
        var uploadFiles: ValueCallback<Array<Uri>>? = null
        var wvChromeClient: WVChromeClient? = null

        @SuppressLint("StaticFieldLeak")
        var wvViewClient: WVViewClient? = null
        const val REQUEST_CODE: Int = 1024
        var userChooseFileMutableMap = mutableMapOf<String, Intent>()
    }

    @SuppressLint("JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // val demoURL = "file:///android_asset/GM.html" //测试用的
        val url = "https://bbs.binmt.cc/forum.php?mod=guide&view=newthread"
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        CrashHandler.getInstance(this)
        val webView = binding.webView
        handleWebViewSettings(webView.settings)
        val activityManager: ActivityManager =
            webView.context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        LogUtils.info("内存" + activityManager.memoryClass.toString())
        LogUtils.info("最大内存" + activityManager.largeMemoryClass.toString())
        wvChromeClient = WVChromeClient()
        wvViewClient = WVViewClient()

        webView.webChromeClient = wvChromeClient
        webView.webViewClient = wvViewClient as WVViewClient
        WebView.setWebContentsDebuggingEnabled(true) // 开启chrome调试
        webView.addJavascriptInterface(
            GMServiceImpl(), "_GM_"
        ) // 注入全局_GM_ API
        webView.loadUrl(url)
        requestPermission()
        LogUtils.info("打开URL:${webView.url}")
    }

    private fun requestPermission() {
        XXPermissions.with(this).permission(
            listOf(
                Permission.READ_MEDIA_AUDIO,
                Permission.READ_MEDIA_VIDEO,
                Permission.READ_MEDIA_IMAGES,
                Permission.CAMERA
            )
        ).request(object : OnPermissionCallback {
            override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                if (!allGranted) {
                    Toast.makeText(
                        applicationContext, "获取部分权限成功，但部分权限未正常授予", Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                //Toast.makeText(applicationContext, "成功获取权限", Toast.LENGTH_SHORT).show()

            }

            override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                if (doNotAskAgain) {
                    Toast.makeText(applicationContext, "被永久拒绝授权，请手动授予权限", Toast.LENGTH_SHORT).show()
                    // 如果是被永久拒绝就跳转到应用权限系统设置页面
                    XXPermissions.startPermissionActivity(applicationContext, permissions)
                } else {
                    Toast.makeText(applicationContext, "获取权限失败", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /*
    * 处理 WebView的所有设置
    * */
    @SuppressLint("SetJavaScriptEnabled")
    private fun handleWebViewSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true // 启用javascript支持
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW  // 允许https站点加载不安全http
        settings.blockNetworkImage = false // 解决图片不显示
        settings.loadsImagesAutomatically = true // 支持自动加载图片
        settings.useWideViewPort = true //将图片调整到适合webView的大小
        settings.loadWithOverviewMode = true // 缩放至屏幕的大小
        settings.builtInZoomControls = true //设置内置的缩放控件。若为false，则该WebView不可缩放
        settings.displayZoomControls = false //隐藏原生的缩放控件
        settings.javaScriptCanOpenWindowsAutomatically = true // 支持通过JS打开新窗口
        settings.allowFileAccess = true // 设置可以访问文件
        settings.allowContentAccess = true // 设置文件权限
        settings.defaultTextEncodingName = "utf-8" //设置编码格式
        settings.domStorageEnabled = true // 开启webView localStorage存储
        settings.databaseEnabled = true // 缓存相关
        settings.cacheMode = WebSettings.LOAD_DEFAULT // 缓存相关
        settings.mediaPlaybackRequiresUserGesture = false // 视频播放需要使用
        settings.setSupportZoom(true) // 允许缩放
        settings.setSupportMultipleWindows(true) // 设置webView支持多窗口
        settings.setGeolocationEnabled(true) // 是否使用地理位置
    }


    /*
    * 添加右上角菜单
    * */
    private fun addItem(menu: Menu?) {
        var index = 0
        LogUtils.info(JSONObject.toJSONString(menuData))
        menuData.forEach {
            LogUtils.info("添加菜单:${it}")
            LogUtils.info("添加菜单:${it["showText"]}")
            menu!!.add(Menu.NONE, index, index, it["showText"] as String)
            index++
        }
    }

    //设置返回键的监听
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return if (binding.webView.canGoBack()) {
                binding.webView.goBack()  //返回上一个页面
                true
            } else {
                finish()
                true
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        addItem(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        menu?.clear()
        addItem(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val guid = menuData[item.itemId]["guid"]
        val showText = menuData[item.itemId]["showText"]
        LogUtils.info("${item.itemId} $showText")
        if (guid == "refresh_website") {
            binding.webView.reload()
        } else if (guid == "goto_log_view") {
            // 跳转到日志显示界面
            val intent = Intent(applicationContext, LogViewerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra(CrashHandler.LOG_SUMMARY, "测试跳转到日志显示界面")
            applicationContext.startActivity(intent)
        } else if (guid == "request_permission") {
            requestPermission()
        } else {
            val menuCommandCallBack =
                """window._GM_.callback.GM_menuCommand["$guid"].changeEvent()""" // 回调js
            binding.webView.evaluateJavascript(
                menuCommandCallBack, null
            )
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == WVChromeClient.CHOOSER_REQUEST) { // 处理返回的文件
            wvChromeClient?.onActivityResultFileChooser(
                requestCode, resultCode, intent
            ) // 调用 WVChromeClient 类中的 回调方法
        }
    }

}