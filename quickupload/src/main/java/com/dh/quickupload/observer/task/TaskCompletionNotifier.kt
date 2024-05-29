package com.dh.quickupload.observer.task

import com.dh.quickupload.UploadService
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.network.ServerResponse

class TaskCompletionNotifier(private val service: UploadService) : UploadTaskObserver {
    override fun onWait(
        info: UploadInfo
    ) {
    }

    override fun onProgress(
        info: UploadInfo
    ) {
    }

    override fun onSuccess(
        info: UploadInfo,

        response: ServerResponse
    ) {
    }

    override fun onError(
        info: UploadInfo,
        exception: Throwable
    ) {
    }

    override fun onCompleted(
        info: UploadInfo
    ) {
        service.taskCompleted(info.uploadId)
    }
}
