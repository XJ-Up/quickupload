package com.dh.updemo

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dh.quickupload.UploadService
import com.dh.quickupload.data.UploadStatus
import com.dh.quickupload.quick.QuickUploadRequest
import java.io.File

/**
 * 单文件上传
 */
class SingleFileUploadActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var uploadStart: Button
    private lateinit var endOfUpload: Button
    private lateinit var selectFile: Button
    private lateinit var uploadProgress: TextView
    private lateinit var uploadAddress: TextView

    companion object {
        private const val READ_REQUEST_CODE = 4
    }

    private var fileItem: FileItem? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.single_file_upload_layout)
        progressBar = findViewById(R.id.progressBar)
        uploadStart = findViewById(R.id.uploadStart)
        endOfUpload = findViewById(R.id.endOfUpload)
        uploadProgress = findViewById(R.id.uploadProgress)
        uploadAddress = findViewById(R.id.uploadAddress)
        selectFile = findViewById(R.id.selectFile)
        uploadStart.setOnClickListener {
            if (fileItem == null) {
                Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show()
            } else {
                fileItem?.let {
                    it.quickUploadRequest= QuickUploadRequest(this, serverUrl = "http://192.168.30.137:8080/upload")
                        .setMethod("POST")
                        .addFileToUpload(
                            filePath = it.filePath,
                            parameterName = "files"
                        )
                        .setResumedFileStart(0)
                    it.startUpload()
                }
            }

        }
        endOfUpload.setOnClickListener {
            fileItem?.stopUpload()
        }
        selectFile.setOnClickListener {
            openFilePicker()
        }
    }

    fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            flags =
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            if (resultData != null) {
                val uri = resultData.data
                onPickedFiles(uri.toString())
            }
        } else {
            super.onActivityResult(requestCode, resultCode, resultData)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun onPickedFiles(path: String) {
        fileItem = FileItem(name(Uri.parse(path)), path, path)
        fileItem?.refresh { uploadStatus, uploadInfo, throwable, serverResponse ->
            when (uploadStatus) {
                UploadStatus.DEFAULT -> {

                }

                UploadStatus.Wait -> {

                }

                UploadStatus.InProgress -> {
                    progressBar.progress = uploadInfo.progressPercent
                    uploadProgress.text = "已上传：${uploadInfo.progressPercent}%"
                }

                UploadStatus.Success -> {
                    uploadProgress.text = "连接成功"
                }

                UploadStatus.Error -> {
                    uploadProgress.text = throwable.toString()
                }

                UploadStatus.Completed -> {
                    if (uploadInfo.progressPercent == 100) {
                        uploadProgress.text = "上传完成"
                    }
                }

                else -> {}
            }
        }
        UploadService.observers.add(fileItem!!)
        uploadAddress.text = "本地文件地址：$path"

    }

    fun name(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val displayNameColumn = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameColumn >= 0) it.getString(displayNameColumn) else null
            } else {
                null
            }
        } ?: uri.toString().split(File.separator).last()
    }
}