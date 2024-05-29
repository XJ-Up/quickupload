package com.dh.quickupload.observer.request

import android.content.Context
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.network.ServerResponse

interface RequestObserverDelegate {
    /**
     * 调用startUpload后调用，无论是否开始上传
     *
     * @ param context context
     * @ param uploadInfo上传状态信息
     */
    fun onWait(context: Context, uploadInfo: UploadInfo)
    /**
     * 当上传进度更改时调用。
     *
     * @ param context context
     * @ param uploadInfo上传状态信息
     */
    fun onProgress(context: Context, uploadInfo: UploadInfo)

    /** 当上传成功完成时调用。
     **
     * @ Param context context
     * @ Param uploadInfo上传状态信息
     * @ Param服务器
     */
    fun onSuccess(context: Context, uploadInfo: UploadInfo, serverResponse: ServerResponse)

    /** 在上传过程中发生错误时调用。
     **
     * @ Param context context
     * @ Param uploadInfo上传状态信息
     * @ Param导致错误的异常
     */
    fun onError(context: Context, uploadInfo: UploadInfo, exception: Throwable)

    /**
     * 当上传完成时调用，成功或错误。
     *
     * @ param context context
     * @ param uploadInfo上传状态信息
     */
    fun onCompleted(context: Context, uploadInfo: UploadInfo)

    /**
     * 仅在监听单个上传ID并注册请求观察者时调用，
     * 但上传ID不存在于UploadService的任务列表中，这意味着它已完成。
     * 在这种情况下，您无法知道它以哪种状态完成 (成功或错误)。
     */
    fun onCompletedWhileNotObserving()
}
