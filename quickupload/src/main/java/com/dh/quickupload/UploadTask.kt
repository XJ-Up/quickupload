package com.dh.quickupload

import android.content.Context
import com.dh.quickupload.UploadConfiguration.semaphore
import com.dh.quickupload.data.UploadFile
import com.dh.quickupload.data.UploadInfo
import com.dh.quickupload.data.UploadTaskParameters
import com.dh.quickupload.exceptions.NoNetworkException
import com.dh.quickupload.exceptions.UploadError
import com.dh.quickupload.exceptions.UserCancelledUploadException
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.network.BaseNetwork
import com.dh.quickupload.network.ServerResponse
import com.dh.quickupload.observer.task.UploadTaskObserver
import kotlinx.coroutines.*
import java.io.IOException
import java.util.ArrayList
import java.util.Date

abstract class UploadTask {
    companion object {
        private val TAG = UploadTask::class.java.simpleName
    }

    private var lastProgressNotificationTime: Long = 0

    protected lateinit var context: Context
    private lateinit var scope: CoroutineScope
    lateinit var params: UploadTaskParameters

    private val observers = ArrayList<UploadTaskObserver>()

    /**
     * 要传输的总字节数。您应该在
     * [UploadTask.upload] 子类的方法，在开始上传数据之前
     * 转让。
     */
    var totalBytes: Long = 0

    /**
     * 总传输字节。上传时，您应该在子类中更新此值
     * 一些数据，并在调用 [UploadTask.onProgress] 之前
     */
    private var uploadedBytes: Long = 0
    lateinit var job: Job

    /**
     * 本次上传任务的开始时间戳。
     */
    private val startTime: Long = Date().time

    /**
     * 已进行的上传尝试的计数器；
     */
    private var attempts: Int = 0

    var noNetwork: Boolean = false
    private var errorDelay = UploadConfiguration.retryPolicy.initialWaitTimeSeconds.toLong()

    private val uploadInfo: UploadInfo
        get() = UploadInfo(
            uploadId = params.id,
            startTime = startTime,
            uploadedBytes = uploadedBytes,
            totalBytes = totalBytes,
            numberOfRetries = attempts,
            files = params.files
        )

    /**
     * 上传逻辑的实现。
     *
     * @ 如果发生错误，则抛出异常
     */
    @Throws(Exception::class)
    protected abstract fun upload(httpStack: BaseNetwork)

    private inline fun theObserver(action: UploadTaskObserver.() -> Unit) {
        observers.forEach {
            try {
                action(it)
            } catch (exc: Throwable) {
                Logger.error(TAG, params.id, exc) {
                    "将事件调度到观察者时出错"
                }
            }
        }
    }
    suspend fun <T> withSemaphore(action: suspend () -> T): T {
        semaphore.acquire() // 获取许可，减少可用许可数
        return try {
            action()
        } finally {
            semaphore.release() // 释放许可，增加可用许可数
        }
    }

    /**
     * 初始化 [UploadTask]
     * 在子类中覆盖此方法以执行自定义任务初始化并获取
     * 在 [UploadRequest.initializeIntent] 方法中设置的自定义参数。
     * @ Param上下文上传服务实例。您应该使用此引用作为您的上下文。
     * @ Param intent intent发送到上下文开始上传
     * @ 如果在初始化时发生I/O异常，则抛出IOException
     */
    @Throws(IOException::class)
    fun init(
        context: Context,
        taskParams: UploadTaskParameters,
        scope: CoroutineScope,
        vararg taskObservers: UploadTaskObserver
    ) {
        this.context = context
        this.params = taskParams
        this.scope = scope
        taskObservers.forEach { observers.add(it) }
        performInitialization()
    }

    open fun performInitialization() {}

    private fun resetAttempts() {
        attempts = 0
        errorDelay = UploadConfiguration.retryPolicy.initialWaitTimeSeconds.toLong()
    }

    fun start() {
        job = scope.launch(UploadConfiguration.dispatcher) {
            theObserver {
                launch(Dispatchers.Main) {
                    onWait(
                        uploadInfo
                    )
                }
            }
            withSemaphore {
                try {

                    resetAttempts()
                    while (attempts <= params.maxRetries && isActive) {
                        try {
                            resetUploadedBytes()
                            upload(UploadConfiguration.network)
                            break
                        } catch (exc: Throwable) {
                            if (attempts >= params.maxRetries) {
                                onError(exc)
                            } else {
                                Logger.error(
                                    TAG,
                                    params.id,
                                    exc
                                ) { "尝试 ${attempts + 1} 时出错。在下一次尝试之前正在等待 ${errorDelay}s。" }

                                val sleepDeadline = System.currentTimeMillis() + errorDelay * 1000

                                sleepWhile { System.currentTimeMillis() < sleepDeadline }

                                errorDelay *= UploadConfiguration.retryPolicy.multiplier.toLong()

                                if (errorDelay > UploadConfiguration.retryPolicy.maxWaitTimeSeconds) {
                                    errorDelay =
                                        UploadConfiguration.retryPolicy.maxWaitTimeSeconds.toLong()
                                }
                            }
                        }
                        attempts++
                    }
                } finally {
                    if (!job.isActive) {
                        onUserCancelledUpload()
                    }
                }
            }
        }


    }

    private inline fun sleepWhile(millis: Long = 1000, condition: () -> Boolean) {
        while (condition()) {
            try {
                Thread.sleep(millis)
            } catch (_: Throwable) {
            }
        }
    }

    protected fun resetUploadedBytes() {
        uploadedBytes = 0
    }

    /**
     * 广播进度更新。
     *
     * @ param uploadedBytes已经上传到服务器的字节数
     * @ param totalBytes请求的总字节数
     */
    protected fun onProgress(bytesSent: Long) {
        uploadedBytes += bytesSent
        if (shouldThrottle(uploadedBytes, totalBytes)) return
        Logger.debug(
            TAG,
            params.id
        ) { "已上传 ${uploadedBytes * 100 / totalBytes}%, $uploadedBytes of $totalBytes 字节" }
        theObserver {
           scope. launch(Dispatchers.Main) {
                onProgress(uploadInfo)
            }
        }
    }

    /**
     * 广播完成状态更新，并通知 [UploadService] 该任务
     * 执行成功。
     * 当任务完成上传请求并收到响应时调用此
     * 从服务器。
     *
     * @ param响应从服务器得到的响应
     */
    protected fun onResponseReceived(response: ServerResponse) {
        Logger.debug(
            TAG,
            params.id
        ) { "上传 ${if (response.isSuccessful) "完成" else "错误"}" }
        if (response.isSuccessful) {
            if (params.autoDeleteSuccessfullyUploadedFiles) {
                for (file in successfullyUploadedFiles) {
                    if (file.handler.delete(context)) {
                        Logger.info(
                            TAG,
                            params.id
                        ) { "成功删除: ${file.path}" }
                    } else {
                        Logger.error(
                            TAG,
                            params.id
                        ) { "删除时出错: ${file.path}" }
                    }
                }
            }

            theObserver {
                scope.launch(Dispatchers.Main) {
                    onSuccess(
                        uploadInfo,
                        response
                    )
                }

            }
        } else {
            theObserver {
                scope.launch(Dispatchers.Main) {
                    onError(
                        uploadInfo,
                        UploadError(response)
                    )
                }

            }
        }

        theObserver {
            scope.launch(Dispatchers.Main) {
                onCompleted(uploadInfo)
            }}
    }

    /**
     * 广播已取消状态。
     * 当用户取消请求时，[UploadTask] 自动调用，
     * 和 [UploadTask.upload] 的具体实现
     * 返回或抛出异常。你不应该在你的显式调用这个方法
     * 实施。
     */
    private fun onUserCancelledUpload() {
        Logger.debug(TAG, params.id) { "上传已取消" }
        if (noNetwork) {

            onError(NoNetworkException())
        } else {
            onError(UserCancelledUploadException())
        }

    }

    /**
     * 广播错误。
     * 具体实现时由 [UploadTask] 自动调用
     * [UploadTask.upload] 抛出异常，没有任何剩余的重试。
     * 您不应该在实现中显式调用此方法。
     * @ param异常广播异常。是具体实现抛出的那个[UploadTask.upload] 的
     */
    private fun onError(exception: Throwable) {
        Logger.error(TAG, params.id, exception) { "错误" }
        uploadInfo.let {
            theObserver {
                scope.launch(Dispatchers.Main) {
                    onError(it, exception) }
                }

            theObserver {
                scope.launch(Dispatchers.Main) {
                    onCompleted(it)
                }
            }
        }
    }

    /**
     * 将所有文件添加到成功上传的文件列表中。
     * 这将自动从params.getFiles() 列表中删除它们。
     */
    protected fun setAllFilesHaveBeenSuccessfullyUploaded(value: Boolean = true) {
        params.files.forEach { it.successfullyUploaded = value }
    }

    /**
     * 获取所有成功上传文件的列表。
     * 你不能在你的子类中修改这个列表!您只能阅读其内容。
     * 如果你想添加一个元素，
     * 使用 [UploadTask.addSuccessfullyUploadedFile]
     * @ 返回字符串列表
     */
    protected val successfullyUploadedFiles: List<UploadFile>
        get() = params.files.filter { it.successfullyUploaded }

    fun cancel() {
        job.cancel()
    }

    private fun shouldThrottle(uploadedBytes: Long, totalBytes: Long): Boolean {
        val currentTime = System.currentTimeMillis()

        if (uploadedBytes < totalBytes && currentTime < lastProgressNotificationTime + UploadConfiguration.uploadProgressNotificationIntervalMillis) {
            return true
        }

        lastProgressNotificationTime = currentTime
        return false
    }
}
