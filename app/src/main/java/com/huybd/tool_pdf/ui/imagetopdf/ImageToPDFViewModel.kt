package com.huybd.tool_pdf.ui.imagetopdf

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import com.huybd.tool_pdf.base.BaseViewModel
import com.huybd.tool_pdf.data.model.ImageFolder
import com.huybd.tool_pdf.data.model.ImageModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageToPDFViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BaseViewModel() {

    private var originalAllImages = listOf<ImageModel>()

    private val _uiImages = MutableStateFlow<List<ImageModel>>(emptyList())
    val uiImages = _uiImages.asStateFlow()

    private val _folders = MutableStateFlow<List<ImageFolder>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<ImageModel>>(emptyList())
    val selectedImages = _selectedImages.asStateFlow()

    private val _currentFolderName = MutableStateFlow("All Images")
    val currentFolderName = _currentFolderName.asStateFlow()

    fun loadImages() {
        launchTask {
            val (images, folders) = fetchImagesFromGallery()
            originalAllImages = images
            _uiImages.value = images
            _folders.value = folders
        }
    }

    fun selectFolder(folder: ImageFolder) {
        _currentFolderName.value = folder.name
        // Filter lại từ danh sách gốc
        val filtered = if (folder.id == "ALL") {
            originalAllImages
        } else {
            originalAllImages.filter { it.folderName == folder.name }
        }
        _uiImages.value = filtered
    }

    // SENIOR FIX: Xử lý Immutable State & Threading
    fun toggleImageSelection(image: ImageModel) {
        // Đẩy việc tính toán list nặng ra khỏi Main Thread
        viewModelScope.launch(Dispatchers.Default) {
            val currentSelected = _selectedImages.value.toMutableList()
            val exists = currentSelected.any { it.id == image.id }

            // 1. Cập nhật danh sách Selected
            if (exists) {
                currentSelected.removeAll { it.id == image.id }
            } else {
                // Copy ra object mới với state mới
                currentSelected.add(image.copy(isSelected = true))
            }

            // Đánh lại số thứ tự (1, 2, 3...)
            val newSelectedList = currentSelected.mapIndexed { index, item ->
                item.copy(selectionIndex = index + 1, isSelected = true)
            }

            // 2. Map ngược trạng thái vào danh sách UI đang hiển thị
            // Lưu ý: StateFlow value update thread-safe, nhưng ta chuẩn bị data xong mới set
            val currentUiList = _uiImages.value
            val newUiList = currentUiList.map { uiItem ->
                val selectedItem = newSelectedList.find { it.id == uiItem.id }
                if (selectedItem != null) {
                    uiItem.copy(isSelected = true, selectionIndex = selectedItem.selectionIndex)
                } else {
                    uiItem.copy(isSelected = false, selectionIndex = -1)
                }
            }

            // 3. Cập nhật cả list gốc (để khi đổi folder không mất tích xanh)
            originalAllImages = originalAllImages.map { originItem ->
                val selectedItem = newSelectedList.find { it.id == originItem.id }
                if (selectedItem != null) {
                    originItem.copy(isSelected = true, selectionIndex = selectedItem.selectionIndex)
                } else {
                    originItem.copy(isSelected = false, selectionIndex = -1)
                }
            }

            // 4. Update StateFlow (Main Thread sẽ nhận được update)
            _selectedImages.value = newSelectedList
            _uiImages.value = newUiList
        }
    }

    // Logic fetch ảnh giữ nguyên (chỉ format lại xíu cho gọn)
    private suspend fun fetchImagesFromGallery(): Pair<List<ImageModel>, List<ImageFolder>> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<ImageModel>()
        val folderMap = mutableMapOf<String, ImageFolder>()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Unknown"
                val folderName = cursor.getString(bucketColumn) ?: "Other"
                val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                val imageModel = ImageModel(id, contentUri, name, folderName)
                imageList.add(imageModel)

                val currentFolder = folderMap[folderName]
                if (currentFolder == null) {
                    folderMap[folderName] = ImageFolder(folderName, folderName, contentUri, 1)
                } else {
                    folderMap[folderName] = currentFolder.copy(count = currentFolder.count + 1)
                }
            }
        }

        val folderList = mutableListOf<ImageFolder>()
        if (imageList.isNotEmpty()) {
            folderList.add(ImageFolder("ALL", "All Images", imageList[0].uri, imageList.size))
        }
        folderList.addAll(folderMap.values)

        return@withContext Pair(imageList, folderList)
    }
}