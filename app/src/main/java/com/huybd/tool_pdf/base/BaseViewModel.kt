package com.huybd.tool_pdf.base


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    // Quản lý trạng thái Loading (dùng StateFlow vì cần lưu giữ trạng thái)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Quản lý thông báo lỗi (dùng SharedFlow vì là sự kiện 1 lần - one-shot event)
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    // Xử lý lỗi chung cho Coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        hideLoading()
        viewModelScope.launch {
            _errorMessage.emit(throwable.message ?: "Unknown Error")
        }
        onError(throwable)
    }

    protected fun showLoading() {
        _isLoading.value = true
    }

    protected fun hideLoading() {
        _isLoading.value = false
    }

    // Hàm tiện ích để chạy coroutine an toàn
    protected fun launchTask(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            showLoading()
            block()
            hideLoading()
        }
    }

    // Cho phép lớp con override nếu muốn xử lý lỗi riêng
    open fun onError(throwable: Throwable) {}
}