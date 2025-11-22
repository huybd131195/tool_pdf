package com.huybd.tool_pdf.ui.imagetopdf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.huybd.tool_pdf.base.BaseFragment
import com.huybd.tool_pdf.databinding.FragmentImageToPDFBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImageToPDFFragment : BaseFragment<FragmentImageToPDFBinding, ImageToPDFViewModel>() {

    override val viewModel: ImageToPDFViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentImageToPDFBinding {
        // Lưu ý: ViewBinding sẽ tự sinh ra tên class dựa trên tên file xml
        // fragment_image_to_p_d_f.xml -> FragmentImageToPDFBinding
        return FragmentImageToPDFBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        // Xử lý nút Back trên Toolbar custom
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun initData() {
        // Load danh sách ảnh...
    }

    override fun observeData() {
        // Lắng nghe dữ liệu...
    }
}