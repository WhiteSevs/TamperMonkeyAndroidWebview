package com.whitesev.gm.utils

import android.content.Context
import java.io.*


object AssetsUtils {

    /**
     * 获取文件内容
     * @param context
     * @param fileName 文件名
     * @param charsetName 编码，默认utf8
     * @return {String}
     */
    fun getFileText(context: Context, fileName: String, charsetName: String = "utf8"): String {
        val fd = context.applicationContext.assets.open(fileName)
        val isReader = InputStreamReader(fd, charsetName)
        val bufReader = BufferedReader(isReader).use {
            it.readText()
        }
        isReader.close()
        fd.close()
        return bufReader
    }
}