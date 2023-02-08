package com.whitesev.gm.utils

import android.util.Log.e
import android.util.Log.i

object LogUtils {
    private const val debug = true
    private const val showLength = 3999

    /**
     * 分段打印出较长log文本
     *
     * @param logContent 打印文本
     * @param showLong   显示长文本，默认显示
     */
    fun info(logContent: String, showLong: Boolean = false) {
        if (!debug) {
            return
        }
        val callerStackTrace = Exception().stackTrace[2] // 调用堆栈
        val callerClassName = callerStackTrace.className
        val callerMethodName = callerStackTrace.methodName
        val tag = "$callerClassName.$callerMethodName"
        if (logContent.length > showLength && showLong) {
            val show = logContent.substring(0, showLength)
            i(tag, show)
            /*剩余的字符串如果大于规定显示的长度，截取剩余字符串进行递归，否则打印结果*/
            if (logContent.length - showLength > showLength) {
                val partLog = logContent.substring(showLength, logContent.length)
                info(partLog)
            } else {
                val printLog = logContent.substring(showLength, logContent.length)
                i(tag, printLog)
            }
        } else {
            e(tag, logContent)
        }
    }

}