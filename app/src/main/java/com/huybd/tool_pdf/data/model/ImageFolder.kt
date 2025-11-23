package com.huybd.tool_pdf.data.model

import android.net.Uri

data class ImageFolder(
    val id: String,          // ID thư mục (dùng bucketId hoặc tên)
    val name: String,        // Tên thư mục (Camera, Download...)
    val firstImageUri: Uri,  // Ảnh đại diện cho thư mục
    val count: Int           // Số lượng ảnh trong thư mục
)