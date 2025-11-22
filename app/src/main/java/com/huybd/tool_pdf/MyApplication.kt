package com.huybd.tool_pdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo PDFBox (Bắt buộc để xử lý PDF)
        PDFBoxResourceLoader.init(applicationContext)
    }
}