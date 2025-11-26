package com.huybd.tool_pdf.ui.imagetopdf

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.huybd.tool_pdf.R
import com.huybd.tool_pdf.base.BaseFragment
import com.huybd.tool_pdf.data.model.ImageFolder
import com.huybd.tool_pdf.databinding.FragmentImageToPDFBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.core.graphics.drawable.toDrawable
import java.io.File

@AndroidEntryPoint
class ImageToPDFFragment : BaseFragment<FragmentImageToPDFBinding, ImageToPDFViewModel>() {

    override val viewModel: ImageToPDFViewModel by viewModels()

    private lateinit var mainAdapter: MainImageAdapter
    private lateinit var selectedAdapter: SelectedImageAdapter
    private lateinit var previewAdapter: ImagePreviewAdapter

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
        setupBackPressHandler()

        binding.btnBack.setOnClickListener {
            if (viewModel.isPreviewMode.value) {
                viewModel.closePreview()
            } else {
                findNavController().popBackStack()
            }
        }

        binding.bottomSheetLayout.btnImport.setOnClickListener {
            val count = viewModel.selectedImages.value.size
            if (count > 0) {
                Toast.makeText(requireContext(), "Đang tạo PDF...", Toast.LENGTH_SHORT).show()
                viewModel.createPdfFromSelection()
            }
        }

        checkPermissionAndLoadData()
    }

    private fun setupAdapters() {
        try {
            mainAdapter = MainImageAdapter(
                onImageClick = { image -> viewModel.toggleImageSelection(image) },
                onExpandClick = { position -> viewModel.openPreview(position) }
            )
            binding.rvImg.apply {
                layoutManager = SquareGridLayoutManager(requireContext(), 3)
                adapter = mainAdapter
                itemAnimator = null
            }

            previewAdapter = ImagePreviewAdapter { image ->
                viewModel.toggleImageSelection(image)
            }
            binding.vpPreview.apply {
                adapter = previewAdapter
                offscreenPageLimit = 1
            }

            selectedAdapter = SelectedImageAdapter { image ->
                viewModel.toggleImageSelection(image)
            }
            binding.bottomSheetLayout.rvSelectedImg.apply {
                layoutManager = GridLayoutManager(context, 3)
                adapter = selectedAdapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Lỗi khởi tạo adapter: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupBottomSheet() {
        try {
            val bottomSheet = binding.bottomSheetLayout.bottomSheetLayout
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

            // Cấu hình ban đầu
            bottomSheetBehavior.isHideable = true
            bottomSheetBehavior.peekHeight = 0
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

            // Click arrow để toggle collapse/expand
            binding.bottomSheetLayout.imgArrowSheet.setOnClickListener {
                try {
                    bottomSheetBehavior.state = when (bottomSheetBehavior.state) {
                        BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
                        BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
                        BottomSheetBehavior.STATE_HIDDEN -> BottomSheetBehavior.STATE_COLLAPSED
                        else -> BottomSheetBehavior.STATE_COLLAPSED
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Callback để update icon arrow
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    try {
                        when (newState) {
                            BottomSheetBehavior.STATE_EXPANDED -> {
                                binding.bottomSheetLayout.imgArrowSheet.setImageResource(R.drawable.ic_arrow_drop_down)
                            }

                            BottomSheetBehavior.STATE_COLLAPSED -> {
                                binding.bottomSheetLayout.imgArrowSheet.setImageResource(R.drawable.ic_arrow_drop_up)
                            }

                            BottomSheetBehavior.STATE_HIDDEN -> {
                                binding.bottomSheetLayout.imgArrowSheet.setImageResource(R.drawable.ic_arrow_drop_up)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    // Optional: Thêm animation cho arrow khi slide
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Lỗi setup bottom sheet: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupFolderMenu() {
        binding.folderSelect.setOnClickListener { view ->
            if (currentFolders.isNotEmpty()) {
                showFolderPopup(view)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showFolderPopup(anchorView: View) {
        try {
            val popupView =
                LayoutInflater.from(requireContext()).inflate(R.layout.layout_folder_popup, null)
            val rvFolder = popupView.findViewById<RecyclerView>(R.id.rvFolderList)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            popupWindow.elevation = 20f

            val folderAdapter = FolderAdapter(currentFolders) { selectedFolder ->
                viewModel.selectFolder(selectedFolder)
                binding.folderName.text = selectedFolder.name
                popupWindow.dismiss()
            }

            rvFolder.layoutManager = LinearLayoutManager(requireContext())
            rvFolder.adapter = folderAdapter
            popupWindow.showAsDropDown(anchorView, 0, 10)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.isPreviewMode.value) {
                        viewModel.closePreview()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
    }

    override fun observeData() {
        // 1. Observe Images (Cập nhật Grid và Preview)
        collectFlow(viewModel.uiImages) { images ->
            try {
                if (::mainAdapter.isInitialized) {
                    mainAdapter.submitList(images)
                }
                if (::previewAdapter.isInitialized) {
                    previewAdapter.submitList(images)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Observe Selected Images (Cập nhật Bottom Sheet)
        collectFlow(viewModel.selectedImages) { selected ->
            try {
                if (::selectedAdapter.isInitialized) {
                    selectedAdapter.submitList(selected) {
                        if (selected.isNotEmpty()) {
                            try {
                                binding.bottomSheetLayout.rvSelectedImg.smoothScrollToPosition(
                                    selected.size - 1
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                updateBottomSheetUI(selected.size)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Observe Folders (Cập nhật Menu Folder)
        collectFlow(viewModel.folders) { folders ->
            currentFolders = folders
        }

        // 4. Observe Preview Mode
        collectFlow(viewModel.isPreviewMode) { isPreview ->
            binding.rvImg.isVisible = !isPreview
            binding.vpPreview.isVisible = isPreview
            binding.folderSelect.isVisible = !isPreview
        }

        // 5. Observe Vị trí Preview (Sync Slider khi click từ Grid)
        collectFlow(viewModel.previewPosition) { pos ->
            try {
                if (binding.vpPreview.currentItem != pos) {
                    binding.vpPreview.setCurrentItem(pos, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 6. Observe PDF File Result
        collectFlow(viewModel.pdfFileResult) { file ->
            if (file != null) {
                Toast.makeText(requireContext(), "Đã tạo xong!", Toast.LENGTH_SHORT).show()
                openPdfFile(file)
                viewModel.pdfFileResult.value = null
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBottomSheetUI(count: Int) {
        try {
            binding.bottomSheetLayout.btnImport.text = "Import ($count)"

            if (count > 0) {
                if (::bottomSheetBehavior.isInitialized) {
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        // Sử dụng giá trị dp được convert sang pixel
                        val peekHeightInPx = (60 * resources.displayMetrics.density).toInt()
                        bottomSheetBehavior.peekHeight = peekHeightInPx
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
            } else {
                if (::bottomSheetBehavior.isInitialized) {
                    bottomSheetBehavior.peekHeight = 0
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                requireContext(),
                "Lỗi cập nhật bottom sheet: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkPermissionAndLoadData() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.loadImages()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    override fun initData() {}

    private fun openPdfFile(file: File) {
        try {
            // 1. Lấy Uri an toàn từ FileProvider
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            // 2. Tạo Intent mở file
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY

            // 3. Mở App đọc PDF (Google Drive, PDF Viewer, etc.)
            val chooser = Intent.createChooser(intent, "Mở file PDF bằng")
            startActivity(chooser)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không tìm thấy ứng dụng đọc PDF!", Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }
}

fun <T> Fragment.collectFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}