package com.whitesev.gm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.whitesev.gm.databinding.ActivityLogViewerBinding
import com.whitesev.gm.utils.CrashHandler
import com.whitesev.gm.utils.LogUtils

class LogViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LogUtils.info("已跳转到日志界面")
        val binding = ActivityLogViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val logSummary = intent.getStringExtra(CrashHandler.LOG_SUMMARY)
        if (!logSummary.isNullOrEmpty()) {
            binding.textLog.text = logSummary
        }
    }
}