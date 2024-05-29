package com.dh.quickupload.observer.request

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.dh.quickupload.UploadRequest
import com.dh.quickupload.UploadService
import com.dh.quickupload.data.UploadInfo

class RequestLiveData @JvmOverloads constructor(
    owner: LifecycleOwner,
    delegate: RequestObserverDelegate,
    shouldAcceptEventsFrom: (uploadInfo: UploadInfo) -> Boolean = { true }
) : BaseRequestLiveData(owner, delegate, shouldAcceptEventsFrom), LifecycleObserver {

    private val lifecycleOwner: LifecycleOwner by lazy { owner }


    private var subscribedUploadID: String? = null

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }


    /**
     * 注册此上传接收器以侦听事件。
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun register() {
        super.register()
        subscribedUploadID?.let {
            if (!UploadService.taskList.contains(it)) {
                delegate.onCompletedWhileNotObserving()
            }
        }
    }

    /**
     * 从侦听事件中取消注册此上载接收器。
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    override fun unregister() {
        super.unregister()
    }

    /**
     * 订阅以仅从给定的上传请求中获取事件。否则，它会听
     * 所有的上传请求。
     */
    fun subscribe(request: UploadRequest<*>) {
        subscribedUploadID = request.startUpload()
        shouldAcceptEventsFrom = { uploadInfo ->
            subscribedUploadID?.let { it == uploadInfo.uploadId } ?: false
        }
    }
}