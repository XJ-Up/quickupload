package com.dh.updemo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dh.quickupload.UploadService
import com.dh.quickupload.data.UploadStatus
import com.dh.quickupload.quick.QuickUploadRequest
import java.io.File
import java.lang.ref.WeakReference

class MultipleSingleFileUploadsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var uploadAdapter: UploadAdapter
    private val fileList = mutableListOf<FileItem>()

    companion object {
        private const val READ_REQUEST_CODE = 7
    }

    private var filePath: MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multiple_single_files_layout)

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator=null

        uploadAdapter = UploadAdapter(fileList) {
            val uploadStatus = it.status

            if (uploadStatus == UploadStatus.InProgress) {
              it.stopUpload()
            } else if (uploadStatus == UploadStatus.DEFAULT || uploadStatus == UploadStatus.Error) {
                val filePath =it.filePath
                val uploadRequest =
                    QuickUploadRequest(this, serverUrl = "http://192.168.30.137:8080/upload")
                        .setMethod("POST")
                        .addFileToUpload(
                            filePath = filePath,
                            parameterName = "files"
                        )
                it.quickUploadRequest = uploadRequest
                it.startUpload()
            }
        }
        recyclerView.adapter = uploadAdapter
        findViewById<Button>(R.id.uploadStart).setOnClickListener {
            fileList.forEachIndexed { index, s ->
                if (UploadService.taskList.contains(s.uploadId)){
                    return@forEachIndexed
                }
                val uploadRequest =
                    QuickUploadRequest(this, serverUrl = "http://192.168.30.137:8080/upload")
                        .setMethod("POST")
                        .addFileToUpload(
                            filePath = s.filePath,
                            parameterName = "files"
                        )
                s.quickUploadRequest =uploadRequest
                s.startUpload()
            }

        }
        findViewById<Button>(R.id.endOfUpload).setOnClickListener {
            UploadService.stopAllUploads()
        }
        findViewById<Button>(R.id.selectFile).setOnClickListener {
            openFilePicker()
        }
    }

    fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            flags =
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.let { intent ->
                val clipData = intent.clipData
                if (clipData != null) {
                    // 多个文件被选中
                    val file = arrayListOf<String>()
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        file.add(uri.toString())
                    }
                    onPickedFiles(file)
                } else {
                    // 单个文件被选中
                    val uri = intent.data
                    uri?.let {
                        // 处理 URI
                        onPickedFiles(arrayListOf(it.toString()))
                    }
                }
            }
        }
    }

    private fun onPickedFiles(path: MutableList<String>) {
        filePath.clear()
        filePath.addAll(path)
        fileList.clear()
        filePath.forEach {
            val fileItem = FileItem(name(Uri.parse(it)), it, it)
            fileItem.refresh { _, _, _, _ ->
                val indexOf = fileList.indexOf(fileItem)
                uploadAdapter.notifyItemChanged(indexOf)
            }
                UploadService.addObserver(fileItem)
            fileList.add(fileItem)
        }
        uploadAdapter.notifyDataSetChanged()
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

    override fun onDestroy() {
        super.onDestroy()
        UploadService.removeAllObserver()
    }
}
