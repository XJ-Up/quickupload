package com.dh.updemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UploadAdapter(
    private val fileList: List<FileItem>,
    private val onUploadButtonClick: (Int) -> Unit
) :
    RecyclerView.Adapter<UploadAdapter.UploadViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UploadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_upload, parent, false)
        return UploadViewHolder(view)
    }

    override fun onBindViewHolder(holder: UploadViewHolder, position: Int) {
        holder.bind(fileList[position], position)
    }

    override fun getItemCount(): Int = fileList.size

    inner class UploadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fileName: TextView = itemView.findViewById(R.id.file_name)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_bar)
        private val uploadButton: Button = itemView.findViewById(R.id.upload_button)
        fun bind(fileItem: FileItem, position: Int) {
            fileName.text = fileItem.fileName
            progressBar.progress = fileItem.uploadProgress
            when (fileItem.uploadStatus) {
                0 -> {
                    uploadButton.text = "上传"
                }

                1 -> {
                    uploadButton.text = "等待"
                }

                2 -> {
                    uploadButton.text = "取消"
                }

                3 -> {
                    uploadButton.text = "重新上传"
                }

                4 -> {
                    uploadButton.text = "上传完成"
                }
            }
            uploadButton.setOnClickListener {
                onUploadButtonClick(position)
            }
        }
    }

}