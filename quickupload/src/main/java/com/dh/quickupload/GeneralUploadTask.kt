package com.dh.quickupload

import android.annotation.SuppressLint
import com.dh.quickupload.data.HttpUploadTaskParameters
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.network.BodyWriter
import com.dh.quickupload.network.NetworkRequest
import com.dh.quickupload.network.BaseNetwork

/**
 * 通用上传任务。
 * 子类来创建您的自定义上传任务。
 */
abstract class GeneralUploadTask : UploadTask(), NetworkRequest.RequestBodyDelegate,
    BodyWriter.OnStreamWriteListener {

    protected val httpParams by lazy {
        HttpUploadTaskParameters.createFromPersistableData(params.additionalParameters)
    }

    /**
     * 在子类中实现，以在进度通知中提供预期的上传。
     * @ return http请求正文的预期大小。
     * @ 抛出UnsupportedEncodingException
     */
    abstract val bodyLength: Long

    /**
     * 上传逻辑的实现。<br></br>
     * 如果您想利用Android上传服务提供的自动化功能，
     * 不要在子类中覆盖或更改此方法的实现。如果你这么做了，
     * 您可以完全控制上传的方式，例如，您可以使用自定义
     * http堆栈，但你必须手动设置请求到服务器的一切你
     * 在你的 [GeneralUploadRequest] 子类中设置，并从服务器获取响应。
     *
     * @ 如果发生错误，则抛出异常
     */
    @SuppressLint("NewApi")
    @Throws(Exception::class)
    override fun upload(httpStack: BaseNetwork) {
        Logger.debug(javaClass.simpleName, params.id) { "正在启动上传任务" }

        setAllFilesHaveBeenSuccessfullyUploaded(false)
        totalBytes = bodyLength

        val response = httpStack.createRequest(params.id, httpParams.method, params.serverUrl)
            .setHeaders(httpParams.requestHeaders.map { it.validateAsHeader() })
            .setTotalBodyBytes(totalBytes, httpParams.usesFixedLengthStreamingMode)
            .getResponse(this, this)

        Logger.debug(javaClass.simpleName, params.id) {
            "服务器响应: code ${response.code}，body ${response.bodyString}"
        }

        // 仅在用户未取消操作时完成广播。
        if (job.isActive) {
            if (response.isSuccessful) {
                setAllFilesHaveBeenSuccessfullyUploaded()
            }
            onResponseReceived(response)
        }
    }

    override fun shouldContinueWriting() = job.isActive

    final override fun onBytesWritten(bytesWritten: Int) {
        onProgress(bytesWritten.toLong())
    }
}
