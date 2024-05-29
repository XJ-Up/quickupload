package com.dh.quickupload.observer.task

import com.dh.quickupload.UploadConfiguration.uploadRepository
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.data.UploadLiveData
import com.dh.quickupload.data.UploadStatus
import com.dh.quickupload.network.ServerResponse


class UploadLiveDataObservation : UploadTaskObserver {

    override fun onWait(
        info: UploadInfo
    ) {
        uploadRepository.setUploadData(UploadLiveData(UploadStatus.Wait, info))
    }

    override fun onProgress(
        info: UploadInfo
    ) {
        uploadRepository.setUploadData(UploadLiveData(UploadStatus.InProgress, info))
    }

    override fun onSuccess(
        info: UploadInfo,
        response: ServerResponse
    ) {
        uploadRepository.setUploadData(UploadLiveData(UploadStatus.Success, info, response))
    }

    override fun onError(
        info: UploadInfo,
        exception: Throwable
    ) {
        uploadRepository.setUploadData(
            UploadLiveData(
                UploadStatus.Error,
                info,
                null,
                exception
            )
        )
    }

    override fun onCompleted(
        info: UploadInfo
    ) {
        uploadRepository.setUploadData(UploadLiveData(UploadStatus.Completed, info))
    }

}