package com.huybd.tool_pdf.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.huybd.tool_pdf.R
import com.huybd.tool_pdf.base.BaseFragment
import com.huybd.tool_pdf.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>() {

    override val viewModel: HomeViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        // Bắt sự kiện click vào Card Image to PDF
        binding.cardImageToPdf.setOnClickListener {
            // Sử dụng ID của action đã định nghĩa trong nav_graph
            findNavController().navigate(R.id.action_homeFragment_to_imageToPDFFragment)
        }

        // Ví dụ cho các nút khác (chưa có màn hình thì hiện thông báo)
        binding.cardTextToPdf.setOnClickListener {
            Toast.makeText(context, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
        }
    }

    override fun initData() {
        // ...
    }

    override fun observeData() {
        // ...
    }
}