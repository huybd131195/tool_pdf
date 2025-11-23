package com.huybd.tool_pdf.data.model

import android.net.Uri

data class ImageModel(
    val id: Long,
    val uri: Uri,
    val name: String,
    val folderName: String,
    val isSelected: Boolean = false,
    val selectionIndex: Int = -1
)