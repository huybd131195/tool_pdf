package com.huybd.tool_pdf.ui.imagetopdf

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.huybd.tool_pdf.R
import com.huybd.tool_pdf.base.BaseFragment
import com.huybd.tool_pdf.data.model.ImageFolder
import com.huybd.tool_pdf.databinding.FragmentImageToPDFBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageToPDFFragment : BaseFragment<FragmentImageToPDFBinding, ImageToPDFViewModel>() {

    override val viewModel: ImageToPDFViewModel by viewModels()

    private lateinit var mainAdapter: MainImageAdapter
    private lateinit var selectedAdapter: SelectedImageAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var currentFolders = listOf<ImageFolder>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadImages()
        } else {
            Toast.makeText(requireContext(), "Cần cấp quyền để tải ảnh!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentImageToPDFBinding {
        return FragmentImageToPDFBinding.inflate(inflater, container, false)
    }

    override fun initView() {
        setupAdapters()
        setupBottomSheet()
        setupFolderMenu()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        checkPermissionAndLoadData()
    }

    private fun setupAdapters() {
        mainAdapter = MainImageAdapter { image ->
            viewModel.toggleImageSelection(image)
        }
        binding.rvImg.apply {
            layoutManager = SquareGridLayoutManager(requireContext(), 3)
            adapter = mainAdapter
            itemAnimator = null // Tắt animation mặc định để tránh nháy khi update
        }

        selectedAdapter = SelectedImageAdapter { image ->
            viewModel.toggleImageSelection(image)
        }
        binding.bottomSheetLayout.rvSelectedImg.adapter = selectedAdapter
    }

    private fun setupBottomSheet() {
        val bottomSheet = binding.bottomSheetLayout.bottomSheetLayout
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        binding.bottomSheetLayout.btnImport.setOnClickListener {
            val count = viewModel.selectedImages.value.size
            if (count > 0) {
                Toast.makeText(requireContext(), "Đã chọn $count ảnh -> Xử lý PDF", Toast.LENGTH_SHORT).show()
            }
        }

        binding.bottomSheetLayout.imgArrowSheet.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    private fun setupFolderMenu() {
        binding.folderSelect.setOnClickListener { view ->
            if (currentFolders.isNotEmpty()) {
                showFolderPopup(view)
            }
        }
    }

    private fun showFolderPopup(anchorView: View) {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_folder_popup, null)
        val rvFolder = popupView.findViewById<RecyclerView>(R.id.rvFolderList)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 20f

        val folderAdapter = FolderAdapter(currentFolders) { selectedFolder ->
            viewModel.selectFolder(selectedFolder)
            binding.folderName.text = selectedFolder.name
            popupWindow.dismiss()
        }

        rvFolder.layoutManager = LinearLayoutManager(requireContext())
        rvFolder.adapter = folderAdapter
        popupWindow.showAsDropDown(anchorView, 0, 10)
    }

    private fun checkPermissionAndLoadData() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadImages()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // SENIOR FIX: Chỉ collect flow khi view ở trạng thái STARTED
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiImages.collect { images ->
                        mainAdapter.submitList(images)
                    }
                }

                launch {
                    viewModel.selectedImages.collect { selected ->
                        selectedAdapter.submitList(selected)
                        updateBottomSheetUI(selected.size)
                    }
                }

                launch {
                    viewModel.folders.collect { folders ->
                        currentFolders = folders
                    }
                }
            }
        }
    }

    private fun updateBottomSheetUI(count: Int) {
        if (count > 0) {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
            binding.bottomSheetLayout.btnImport.text = "Import ($count)"
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun initData() { }
}