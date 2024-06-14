package com.dh.updemo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dh.quickupload.UploadService
import com.dh.quickupload.data.UploadStatus
import com.dh.quickupload.quick.QuickUploadRequest

/**
 * 同时上传多个文件活动
 * 简单来说就是多个文件在同一个上传请求中上传，它们具有相同的 UploadID
 * 无论多少个文件，上传请求只有一个
 * 注意：不支持断点续传
 * 此功能需要服务器支持单请求多文件同时上传
 */
class UploadMultipleFilesSimultaneouslyActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var uploadStart: Button
    private lateinit var endOfUpload: Button
    private lateinit var selectFile: Button
    private lateinit var uploadProgress: TextView
    private lateinit var uploadAddress: TextView

    companion object {
        private const val READ_REQUEST_CODE = 5
    }
    private var filesItem:FilesItem?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.upload_multiple_files_simultaneously_layout)
        progressBar = findViewById(R.id.progressBar)
        uploadStart = findViewById(R.id.uploadStart)
        endOfUpload = findViewById(R.id.endOfUpload)
        uploadProgress = findViewById(R.id.uploadProgress)
        uploadAddress = findViewById(R.id.uploadAddress)
        selectFile = findViewById(R.id.selectFile)
        uploadStart.setOnClickListener {
            if (filesItem==null){
                Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show()
            }else{
                filesItem?.let {
                    val  quickUploadRequest=   QuickUploadRequest(this, serverUrl = "http://192.168.30.137:8080/upload")
                        .setMethod("POST")
                        .apply {
                            it.filePath .forEachIndexed { index, s ->
                                addFileToUpload(
                                    filePath = s,
                                    parameterName = "files"
                                )
                            }
                        }
                    it.quickUploadRequest=quickUploadRequest
                    it.startUpload()
                }
            }
        }
        endOfUpload.setOnClickListener {
            filesItem?.stopUpload()
        }
        selectFile.setOnClickListener {
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
                    var file = arrayListOf<String>()

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
        filesItem = FilesItem("多文件",path)
        filesItem?.uploadId="多文件"
        filesItem?.refresh { uploadStatus, uploadInfo, throwable, serverResponse ->
            when(uploadStatus){
                UploadStatus.DEFAULT->{

                }
                UploadStatus.Wait->{

                }
                UploadStatus.InProgress->{
                    progressBar.progress = uploadInfo.progressPercent
                    uploadProgress.text = "已上传：${uploadInfo.progressPercent.toString()}%"
                }
                UploadStatus.Success->{
                    uploadProgress.text = "连接成功"
                }
                UploadStatus.Error->{
                    uploadProgress.text = "${throwable.toString()}"
                }
                UploadStatus.Completed->{
                    if (uploadInfo.progressPercent == 100) {
                        uploadProgress.text = "上传完成"
                    }
                }
                else->{}
            }
        }
        UploadService.observers.add(filesItem!!)
        uploadAddress.text = "本地文件地址：${path.joinToString()}"
    }
}