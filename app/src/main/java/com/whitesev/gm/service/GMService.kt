package com.whitesev.gm.service

abstract class GMService {

    /**
     * 初始化js全局GM函数
     */
    abstract fun initGMApi()

    /**
     * 加载网络油猴脚本
     * @param url 油猴的网络地址 https://greasyfork.org/scripts/....user.js
     * */
    abstract fun loadNetWorkScript(url: String)

    /**
     * 获取所有本地脚本内容
     * @param fileList 数组形式
     * @return {String}
     * */
    abstract fun getLocalScriptContent(fileList: List<String>): String

    /**
     * 解析油猴脚本
     * @param scriptText 脚本的文本
     * */
    abstract fun parseScript(scriptText: String)

    /**
     * 跨域请求网络函数
     * @params 请求的配置信息，JSON格式的字符串
     */
    abstract fun GM_xmlhttpRequest(options: String)

    /**
     * 注册菜单函数
     * @params 请求的配置信息，JSON格式的字符串
     */
    abstract fun GM_registerMenuCommand(options: String)

    /**
     * 取消注册菜单函数
     * @params 需要删除的GUID
     */
    abstract fun GM_unregisterMenuCommand(guid: String)

    /**
     * 获取需要注入的js
     */
    abstract fun getScriptText(): String

    /**
     * 获取需要注入的iframe的js
     * **/
    abstract fun getIframeScriptText(): String

    /**
     * 获取需要注入内的api
     */
    abstract fun getGMApi(): String

    /**
     * 获取需要注入iframe内的api
     */
    abstract fun getGMIframeApi(): String


    /**
     * @description 复制文字到剪贴板
     */
    abstract fun GM_setClipboard(text: String)
}