package com.huybd.tool_pdf.ui.imagetopdf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.huybd.tool_pdf.data.model.ImageModel
import com.huybd.tool_pdf.databinding.ItemImageGridBinding

class MainImageAdapter(
    private val onImageClick: (ImageModel) -> Unit
) : ListAdapter<ImageModel, MainImageAdapter.ImageViewHolder>(ImageDiffCallback()) {

    inner class ImageViewHolder(val binding: ItemImageGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                // SỬA CHÍNH XÁC: Dùng bindingAdapterPosition (vì đã thêm thư viện mới)
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onImageClick(getItem(position))
                }
            }
        }

        fun bind(item: ImageModel) {
            Glide.with(binding.root)
                .load(item.uri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(binding.imgThumbnail)

            if (item.isSelected) {
                binding.overLay.visibility = View.VISIBLE
                binding.tvSelectionOrder.visibility = View.VISIBLE
                binding.tvSelectionOrder.text = item.selectionIndex.toString()
                binding.unselectedCircle.visibility = View.GONE
            } else {
                binding.overLay.visibility = View.GONE
                binding.tvSelectionOrder.visibility = View.GONE
                binding.unselectedCircle.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ItemImageGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ImageDiffCallback : DiffUtil.ItemCallback<ImageModel>() {
    override fun areItemsTheSame(oldItem: ImageModel, newItem: ImageModel) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ImageModel, newItem: ImageModel) = oldItem == newItem
}