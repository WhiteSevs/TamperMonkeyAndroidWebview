# TamperMonkeyAndroidWebview
在Android的Webview上使用油猴函数的一个项目，该项目示例油猴脚本为MT论坛

## 其中，如果页面中存在iframe
那么在Chrome中使用油猴可以正常加载脚本，那么在这里不行，因为我没写多个WebView管理，可以重写自行WebViewClient中的`shouldInterceptRequest`函数中可以设置webView
但是重写后，其它设置也需要重新加载，比如Main中的一些函数

蓝奏云地址: [https://baiqi.lanzoul.com/b06753gjc](https://baiqi.lanzoul.com/b06753gjc)
蓝奏云密码：chmn