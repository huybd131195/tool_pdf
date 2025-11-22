package com.huybd.tool_pdf.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

// VB: Type của ViewBinding, VM: Type của ViewModel
abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel> : Fragment() {

    private var _binding: VB? = null
    // Chỉ truy cập binding khi view còn tồn tại
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException("Binding is only valid between onCreateView and onDestroyView")

    // Lớp con bắt buộc phải cung cấp ViewModel
    protected abstract val viewModel: VM

    // Lớp con cung cấp cách inflate binding
    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
        observeBaseData() // Lắng nghe loading/error mặc định
        observeData()     // Lắng nghe dữ liệu riêng của màn hình
    }

    // Dọn dẹp binding để tránh memory leak (Rất quan trọng trong Fragment)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Các hàm abstract để lớp con implement
    abstract fun initView()
    abstract fun initData()
    abstract fun observeData()

    private fun observeBaseData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Lắng nghe Loading
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) showLoading() else hideLoading()
                    }
                }

                // Lắng nghe Error
                launch {
                    viewModel.errorMessage.collect { message ->
                        showErrorMessage(message)
                    }
                }
            }
        }
    }

    // Có thể override để hiển thị Dialog Loading thay vì ProgressBar
    open fun showLoading() {
        // Mặc định có thể để trống hoặc log, lớp con sẽ xử lý UI cụ thể
    }

    open fun hideLoading() {
        // Ẩn dialog loading
    }

    open fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}