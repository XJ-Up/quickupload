package com.dh.quickupload.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.data.UploadLiveData
import com.dh.quickupload.data.UploadStatus

/**
 * 用来获取上传实时数据的仓库
 */
class UploadRepository {
    private var _uploadResultLiveData = MutableLiveData<UploadLiveData>()
    private val handler: Handler = Handler(Looper.getMainLooper())
    fun uploadData(): LiveData<UploadLiveData> {
        return _uploadResultLiveData
    }
    fun setUploadData(dataSource: UploadLiveData) {
        handler.post {
            this._uploadResultLiveData.value = dataSource
        }
    }
}