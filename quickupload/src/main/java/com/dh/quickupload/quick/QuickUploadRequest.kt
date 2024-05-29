package com.dh.quickupload.quick

import android.content.Context
import java.io.FileNotFoundException
import com.dh.quickupload.GeneralUploadRequest
import com.dh.quickupload.UploadTask
import com.dh.quickupload.data.UploadFile

/**
 * 上传请求。
 * @ param context应用程序上下文
 * @ param serverUrl将处理多部分表单上传的服务器端脚本的URL。
 */
class QuickUploadRequest(context: Context, serverUrl: String) :
    GeneralUploadRequest<QuickUploadRequest>(context, serverUrl) {

    override val taskClass: Class<out UploadTask>
        get() = QuickUploadTask::class.java

    /**
     * 将文件添加到此上传请求。
     *
     * @ param filePath要上传的文件的路径
     * @ param parameterName将包含文件数据的表单参数的名称
     * @ param fileName服务器端脚本看到的文件名。如果为null，则为原始文件名
     * 将使用
     * @ param contentType文件的内容类型。如果为null或empty，则mime类型将为
     * 自动检测。如果由于某些原因自动检测失败，
     * 默认情况下将使用 “application/octet-stream”
     * @ return [QuickUploadRequest]
     */
    @Throws(FileNotFoundException::class)
    @JvmOverloads
    fun addFileToUpload(
        filePath: String,
        parameterName: String,
        fileName: String? = null,
        contentType: String? = null
    ): QuickUploadRequest {
        require(filePath.isNotBlank() && parameterName.isNotBlank()) {
            "请指定有效的文件路径和参数名称。它们不能是空白的。"
        }

        files.add(UploadFile(filePath).apply {
            this.parameterName = parameterName

            this.contentType = if (contentType.isNullOrBlank()) {
                handler.contentType(context)
            } else {
                contentType
            }

            remoteFileName = if (fileName.isNullOrBlank()) {
                handler.name(context)
            } else {
                fileName
            }
        })

        return this
    }
}
