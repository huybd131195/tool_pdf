package com.huybd.tool_pdf.ui.imagetopdf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.huybd.tool_pdf.R
import com.huybd.tool_pdf.data.model.ImageModel
import com.huybd.tool_pdf.databinding.ItemImageSliderBinding

class ImagePreviewAdapter(
    private val onSelectClick: (ImageModel) -> Unit
) : ListAdapter<ImageModel, ImagePreviewAdapter.PreviewViewHolder>(ImageDiffCallback()) {

    inner class PreviewViewHolder(val binding: ItemImageSliderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(image: ImageModel) {
            Glide.with(binding.root.context)
                .load(image.uri)
                .into(binding.imgFullPreview)

            // Update trạng thái nút Select
            if (image.isSelected) {
                binding.btnSelectInPreview.text = buildString {
                    append("Selected (")
                    append(image.selectionIndex)
                    append(")")
                }
                binding.btnSelectInPreview.setBackgroundResource(R.drawable.bg_button_selected) // Giả sử bạn có drawable này
                // Hoặc đổi màu icon/text tùy design
            } else {
                binding.btnSelectInPreview.text = buildString {
                    append("Select")
                }
                binding.btnSelectInPreview.setBackgroundResource(R.drawable.bg_button_unselected)
            }

            binding.btnSelectInPreview.setOnClickListener {
                onSelectClick(image)
                // Adapter sẽ tự refresh nhờ ListAdapter submitList lại
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        return PreviewViewHolder(
            ItemImageSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}