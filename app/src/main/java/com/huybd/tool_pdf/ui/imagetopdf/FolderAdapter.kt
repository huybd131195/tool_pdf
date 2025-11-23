package com.huybd.tool_pdf.ui.imagetopdf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.huybd.tool_pdf.data.model.ImageFolder
import com.huybd.tool_pdf.databinding.ItemFolderDropdownBinding

class FolderAdapter(
    private var folders: List<ImageFolder>,
    private val onFolderClick: (ImageFolder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(val binding: ItemFolderDropdownBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: ImageFolder) {
            binding.tvFolderName.text = folder.name
            binding.tvFolderCount.text = "${folder.count}"
            Glide.with(binding.root).load(folder.firstImageUri).into(binding.imgFolderThumb)

            binding.root.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        return FolderViewHolder(
            ItemFolderDropdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    fun submitList(newFolders: List<ImageFolder>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}