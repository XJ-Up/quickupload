package com.dh.updemo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dh.quickupload.UploadService
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.network.ServerResponse
import com.dh.quickupload.observer.request.RequestLiveData
import com.dh.quickupload.observer.request.RequestObserverDelegate
import com.dh.quickupload.quick.QuickUploadRequest

class SingleFileUploadActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var uploadStart: Button
    private lateinit var endOfUpload: Button
    private lateinit var selectFile: Button
    private lateinit var uploadProgress: TextView
    private lateinit var uploadAddress: TextView
    private var mPath = ""

    companion object {
        private const val READ_REQUEST_CODE = 4
    }

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
            if (mPath == "") {
                Toast.makeText(this, "请选择文件", Toast.LENGTH_SHORT).show()
            } else {
                QuickUploadRequest(this, serverUrl = "http://192.168.30.137:8080/upload")
                    .setMethod("POST")
                    .addFileToUpload(
                        filePath = mPath,
                        parameterName = "files"
                    )
                    .setResumedFileStart(0)//如果需要断点续传调用此方法，默认情况下不需要调用
                    .setUploadID("1")
                    .startUpload()
            }
        }
        endOfUpload.setOnClickListener {
            UploadService.stopUpload("1")
        }
        selectFile.setOnClickListener {
            openFilePicker()
        }
        RequestLiveData(this, object : RequestObserverDelegate {
            override fun onWait(context: Context, uploadInfo: UploadInfo) {

            }

            override fun onProgress(context: Context, uploadInfo: UploadInfo) {
                progressBar.progress = uploadInfo.progressPercent
                uploadProgress.text = "已上传：${uploadInfo.progressPercent.toString()}%"
            }

            override fun onSuccess(
                context: Context,
                uploadInfo: UploadInfo,
                serverResponse: ServerResponse
            ) {
                uploadProgress.text = "连接成功"
            }

            override fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable) {
                uploadProgress.text = "${exception.toString()}"
            }

            override fun onCompleted(context: Context, uploadInfo: UploadInfo) {
                if (uploadInfo.progressPercent == 100) {
                    uploadProgress.text = "上传完成"
                }
            }

            override fun onCompletedWhileNotObserving() {
            }
        })
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

    private fun onPickedFiles(path: String) {
        uploadAddress.text = "本地文件地址：$path"
        mPath = path
    }
}