package com.dh.quickupload.observer.task

import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.network.ServerResponse

interface UploadTaskObserver {
    fun onWait(info: UploadInfo)

    fun onProgress(
        info: UploadInfo
    )

    fun onSuccess(
        info: UploadInfo,
        response: ServerResponse
    )

    fun onError(
        info: UploadInfo,
        exception: Throwable
    )

    fun onCompleted(
        info: UploadInfo
    )
}
