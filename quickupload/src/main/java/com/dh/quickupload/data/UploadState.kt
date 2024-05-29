package com.dh.quickupload.data

import com.dh.quickupload.network.ServerResponse

data class UploadLiveData(
    val status: UploadStatus,
    val uploadInfo: UploadInfo,
    val serverResponse: ServerResponse? = null,
    val exception: Throwable? = null
)