package com.dh.quickupload

import android.content.Context
import com.dh.quickupload.data.UploadFile
import com.dh.quickupload.data.UploadTaskParameters
import com.dh.quickupload.extensions.startNewUpload
import com.dh.quickupload.tools.datapreservation.Persistable
import com.dh.quickupload.tools.datapreservation.PersistableData
import java.util.ArrayList
import java.util.UUID

/**
 * 要扩展以创建上传请求的基类。如果您正在实现基于HTTP的上传，
 * 扩展 [GeneralUploadRequest] 代替。
 */
abstract class UploadRequest<B : UploadRequest<B>>
/**
 * 创建一个新的上传请求。
 *
 * @ param context应用程序上下文
 * @ param serverUrl处理请求的服务器端脚本的URL
 * @ 如果一个或多个参数无效，则抛出IllegalArgumentException
 */
@Throws(IllegalArgumentException::class)
constructor(protected val context: Context, protected var serverUrl: String) : Persistable {

    private var uploadId = UUID.randomUUID().toString()
    private var started: Boolean = false
    private var maxRetries = UploadConfiguration.retryPolicy.defaultMaxRetries
    private var autoDeleteSuccessfullyUploadedFiles = false
    protected val files = ArrayList<UploadFile>()
    private var resumedFileStart :Long= 0
    /**
     * 在子类中实现以指定将处理上传任务的类。
     * 类必须是 [UploadTask] 的子类。
     * @ return类
     */
    protected abstract val taskClass: Class<out UploadTask>

    init {
        require(serverUrl.isNotBlank()) { "服务器URL不能为空" }
    }

    private val uploadTaskParameters: UploadTaskParameters
        get() = UploadTaskParameters(
            taskClass = taskClass.name,
            id = uploadId,
            serverUrl = serverUrl,
            maxRetries = maxRetries,
            autoDeleteSuccessfullyUploadedFiles = autoDeleteSuccessfullyUploadedFiles,
            files = files,
            resumedFileStart=resumedFileStart,
            additionalParameters = getAdditionalParameters()
        )

    /**
     * 启动后台文件上传服务。
     * @ 返回uploadId字符串。如果您在构造函数中传递了自己的uploadId，则此
     * 方法将返回相同的uploadId，否则它将自动返回
     * 生成的uploadId
     */
    open fun startUpload(): String {
        check(!started) {
            "您已经在此上传请求实例上调用了一次startUpload()，并且您不能多次调用它。请检查您的代码。"
        }
        check(!UploadService.taskList.contains(uploadTaskParameters.id)) {
            "您已尝试使用相同的uploadID执行startUpload()已在运行任务。您正在尝试对多个上载使用相同的ID。"
        }
        started = true
        return context.startNewUpload(
            params = uploadTaskParameters
        )
    }

    protected abstract fun getAdditionalParameters(): PersistableData

    @Suppress("UNCHECKED_CAST")
    protected fun self(): B {
        return this as B
    }


    /**
     * 设置上传成功后自动删除文件。
     * @ param autoDeleteFiles为true以自动删除包含在
     * 请求时，上传成功完成。
     * 默认情况下，此设置设置为false，并且不会删除任何内容。
     * @ return self实例
     */
    fun setAutoDeleteFilesAfterSuccessfulUpload(autoDeleteFiles: Boolean): B {
        this.autoDeleteSuccessfullyUploadedFiles = autoDeleteFiles
        return self()
    }

    /**
     * 设置发生错误时库将尝试的最大重试次数，
     * 在返回错误之前。
     *
     * @ param maxRetries发生错误时的最大重试次数
     * @ return self实例
     */
    fun setMaxRetries(maxRetries: Int): B {
        this.maxRetries = maxRetries
        return self()
    }

    /**
     * 设置上传ID。
     *
     * @ param uploadID要分配给此上传请求的唯一ID。
     * 如果为null或空，将自动生成随机UUID。
     * 它在接收更新时用于广播接收器。
     */
    fun setUploadID(uploadID: String): B {
        this.uploadId = uploadID
        return self()
    }
    /**
     * 设置断点续传开始的地方
     */
    fun setResumedFileStart(index: Long): B {
        this.resumedFileStart = index
        return self()
    }
    /**
     * 获取表示此上传请求的 [PersistableData] 对象。
     * @ return [PersistableData] 表示此上传的对象
     */
    override fun toPersistableData() = uploadTaskParameters.toPersistableData()
}
