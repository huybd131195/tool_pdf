package com.huybd.tool_pdf.ui.imagetopdf

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.viewModelScope
import com.huybd.tool_pdf.base.BaseViewModel
import com.huybd.tool_pdf.data.model.ImageFolder
import com.huybd.tool_pdf.data.model.ImageModel
import com.huybd.tool_pdf.utils.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    private val _currentImageList = MutableStateFlow<List<ImageModel>>(emptyList())
    val currentImageList = _currentImageList.asStateFlow()

    val pdfFileResult = MutableStateFlow<File?>(null)

    val isGenerating = MutableStateFlow(false)

    private val _currentFolderName = MutableStateFlow("All Images")
    val currentFolderName = _currentFolderName.asStateFlow()

    private val _isPreviewMode = MutableStateFlow(false)
    val isPreviewMode = _isPreviewMode.asStateFlow()

    private val _previewPosition = MutableStateFlow(0)
    val previewPosition = _previewPosition.asStateFlow()

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
        val filtered = if (folder.id == "ALL") {
            originalAllImages
        } else {
            originalAllImages.filter { it.folderName == folder.name }
        }
        _uiImages.value = filtered
    }

    fun openPreview(position: Int) {
        _previewPosition.value = position
        _isPreviewMode.value = true
    }

    fun closePreview() {
        _isPreviewMode.value = false
    }

    fun toggleImageSelection(image: ImageModel) {
        // Đẩy việc tính toán list nặng ra khỏi Main Thread
        viewModelScope.launch(Dispatchers.Default) {
            val currentSelected = _selectedImages.value.toMutableList()
            val exists = currentSelected.any { it.id == image.id }

            if (exists) {
                currentSelected.removeAll { it.id == image.id }
            } else {
                // Copy ra object mới với state mới
                currentSelected.add(image.copy(isSelected = true))
            }

            val newSelectedList = currentSelected.mapIndexed { index, item ->
                item.copy(selectionIndex = index + 1, isSelected = true)
            }


            val currentUiList = _uiImages.value
            val newUiList = currentUiList.map { uiItem ->
                val selectedItem = newSelectedList.find { it.id == uiItem.id }
                if (selectedItem != null) {
                    uiItem.copy(isSelected = true, selectionIndex = selectedItem.selectionIndex)
                } else {
                    uiItem.copy(isSelected = false, selectionIndex = -1)
                }
            }

            originalAllImages = originalAllImages.map { originItem ->
                val selectedItem = newSelectedList.find { it.id == originItem.id }
                if (selectedItem != null) {
                    originItem.copy(isSelected = true, selectionIndex = selectedItem.selectionIndex)
                } else {
                    originItem.copy(isSelected = false, selectionIndex = -1)
                }
            }

            _selectedImages.value = newSelectedList
            _uiImages.value = newUiList
        }
    }

    fun createPdfFromSelection() {
        val selectedList = _selectedImages.value
        if (selectedList.isEmpty()) return

        isGenerating.value = true

        viewModelScope.launch {
            try {
                val uris = selectedList.map { it.uri }
                val fileName = "PDF_${System.currentTimeMillis()}"

                val file = PdfGenerator.generatePdf(context, uris, fileName)

                pdfFileResult.value = file

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGenerating.value = false
            }
        }
    }

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