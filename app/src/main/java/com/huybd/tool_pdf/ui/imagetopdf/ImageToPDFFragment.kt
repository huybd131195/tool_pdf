package com.huybd.tool_pdf.ui.imagetopdf

import android.Manifest
import android.annotation.SuppressLint
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

        checkPermissionAndLoadData()
    }


    private fun setupAdapters() {
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
    }

    private fun setupBottomSheet() {
        val bottomSheet = binding.bottomSheetLayout.bottomSheetLayout
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        bottomSheetBehavior.isHideable = true

        // Nút Import
        binding.bottomSheetLayout.btnImport.setOnClickListener {
            val count = viewModel.selectedImages.value.size
            if (count > 0) {
                Toast.makeText(
                    requireContext(),
                    "Đã chọn $count ảnh -> Xử lý PDF",
                    Toast.LENGTH_SHORT
                ).show()
                // TODO: Gọi hàm tạo PDF ở đây
            }
        }

        binding.bottomSheetLayout.imgArrowSheet.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            } else if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
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

    @SuppressLint("InflateParams")
    private fun showFolderPopup(anchorView: View) {
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
                        requireActivity().onBackPressedDispatcher.onBackPressed()                    }
                }
            })
    }


    override fun observeData() {
        collectFlow(viewModel.uiImages) { images ->
            mainAdapter.submitList(images)
            previewAdapter.submitList(images)
        }

        collectFlow(viewModel.selectedImages) { selected ->
            selectedAdapter.submitList(selected) {
                if (selected.isNotEmpty()) {
                    binding.bottomSheetLayout.rvSelectedImg.smoothScrollToPosition(selected.size - 1)
                }
            }
            updateBottomSheetUI(selected.size)
        }

        // 3. Observe Folders (Cập nhật Menu Folder)
        collectFlow(viewModel.folders) { folders ->
            currentFolders = folders
        }

        collectFlow(viewModel.isPreviewMode) { isPreview ->
            binding.rvImg.isVisible = !isPreview
            binding.vpPreview.isVisible = isPreview
            binding.folderSelect.isVisible = !isPreview
        }

        // 5. Observe Vị trí Preview (Sync Slider khi click từ Grid)
        collectFlow(viewModel.previewPosition) { pos ->
            if (binding.vpPreview.currentItem != pos) {
                binding.vpPreview.setCurrentItem(pos, false)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateBottomSheetUI(count: Int) {
        binding.bottomSheetLayout.btnImport.text = "Import ($count)"

        if (count > 0) {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN ||
                bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {

                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
}

fun <T> Fragment.collectFlow(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}