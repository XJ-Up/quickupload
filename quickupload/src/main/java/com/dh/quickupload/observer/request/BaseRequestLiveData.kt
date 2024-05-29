package com.dh.quickupload.observer.request

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.dh.quickupload.UploadConfiguration.uploadRepository
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.data.UploadLiveData
import com.dh.quickupload.data.UploadStatus
import com.dh.quickupload.viewmodel.UploadViewModel
import com.dh.quickupload.viewmodel.UploadViewModelFactory

open class BaseRequestLiveData
    (
    private val owner: LifecycleOwner,
    internal val delegate: RequestObserverDelegate,
    internal var shouldAcceptEventsFrom: (uploadInfo: UploadInfo) -> Boolean
) {
    protected val context: Context by lazy {
        (owner as? Context) ?: throw IllegalArgumentException("Owner must be a Context")
    }

    private val viewModelStoreOwner: ViewModelStoreOwner = owner as ViewModelStoreOwner
    private val viewModel = ViewModelProvider(
        viewModelStoreOwner,
        UploadViewModelFactory(uploadRepository)
    )[UploadViewModel::class.java]
    private val observe = Observer<UploadLiveData> {
        val uploadInfo = it.uploadInfo
        if (!shouldAcceptEventsFrom(uploadInfo)) {
            return@Observer
        }
        when (it.status) {
            UploadStatus.Wait -> delegate.onWait(context, uploadInfo)
            UploadStatus.InProgress -> delegate.onProgress(context, uploadInfo)
            UploadStatus.Error -> delegate.onError(context, uploadInfo, it.exception!!)
            UploadStatus.Success -> delegate.onSuccess(context, uploadInfo, it.serverResponse!!)
            UploadStatus.Completed -> delegate.onCompleted(context, uploadInfo)
        }

    }

    open fun register() {
        viewModel.dataLiveData.observeForever(observe)
    }

    open fun unregister() {
        viewModel.dataLiveData.removeObserver(observe)
    }
}