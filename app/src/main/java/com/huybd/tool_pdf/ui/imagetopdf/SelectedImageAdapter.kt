package com.huybd.tool_pdf.ui.imagetopdf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.huybd.tool_pdf.data.model.ImageModel
import com.huybd.tool_pdf.databinding.ItemImageSelectedBinding

// Class này dùng chung ImageDiffCallback bên file MainImageAdapter nên không cần khai báo lại
class SelectedImageAdapter(
    private val onRemoveClick: (ImageModel) -> Unit
) : ListAdapter<ImageModel, SelectedImageAdapter.SelectedViewHolder>(ImageDiffCallback()) {

    inner class SelectedViewHolder(val binding: ItemImageSelectedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ImageModel) {
            Glide.with(binding.root).load(item.uri).into(binding.imgSelectedThumbnail)

            // Nút xóa ảnh
            binding.btnRemove.setOnClickListener { onRemoveClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedViewHolder {
        return SelectedViewHolder(
            ItemImageSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SelectedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}