package com.dh.quickupload.quick

import com.dh.quickupload.GeneralUploadTask
import com.dh.quickupload.data.NameValue
import com.dh.quickupload.data.UploadFile
import com.dh.quickupload.extensions.addHeader
import com.dh.quickupload.extensions.asciiBytes
import com.dh.quickupload.extensions.utf8Bytes
import com.dh.quickupload.network.BodyWriter

/**
 * 实现HTTP分段上传任务。
 */
class QuickUploadTask : GeneralUploadTask() {

    companion object {
        private const val BOUNDARY_SIGNATURE = "-------UploadService1.0.0-"
        private const val NEW_LINE = "\r\n"
        private const val TWO_HYPHENS = "--"
    }

    private val boundary = BOUNDARY_SIGNATURE + System.nanoTime()
    private val boundaryBytes = (TWO_HYPHENS + boundary + NEW_LINE).asciiBytes
    private val trailerBytes = (TWO_HYPHENS + boundary + TWO_HYPHENS + NEW_LINE).asciiBytes
    private val newLineBytes = NEW_LINE.utf8Bytes

    private val NameValue.multipartHeader: ByteArray
        get() = boundaryBytes + ("Content-Disposition: form-data; " +
                "name=\"$name\"$NEW_LINE$NEW_LINE$value$NEW_LINE").utf8Bytes

    private val UploadFile.multipartHeader: ByteArray
        get() = boundaryBytes + ("Content-Disposition: form-data; " +
                "name=\"$parameterName\"; " +
                "filename=\"$remoteFileName\"$NEW_LINE" +
                "Content-Type: $contentType$NEW_LINE$NEW_LINE").utf8Bytes

    private val UploadFile.totalMultipartBytes: Long
        get() = multipartHeader.size.toLong() + handler.size(context) + newLineBytes.size.toLong()

    private fun BodyWriter.writeRequestParameters() {
        httpParams.requestParameters.forEach {
            write(it.multipartHeader)
        }
    }

    private fun BodyWriter.writeFiles() {

        for (file in params.files) {
            if (!job.isActive) break
            write(file.multipartHeader)
            writeStream(file.handler.stream(context),file.handler.size(context),params.resumedFileStart)
            write(newLineBytes)
        }
    }

    private val requestParametersLength: Long
        get() = httpParams.requestParameters.map { it.multipartHeader.size.toLong() }.sum()

    private val filesLength: Long
        get() = params.files.map { it.totalMultipartBytes }.sum()-params.resumedFileStart

    override val bodyLength: Long
        get() = requestParametersLength + filesLength + trailerBytes.size

    override fun performInitialization() {
        httpParams.requestHeaders.apply {
            addHeader("Content-Type", "multipart/form-data; boundary=$boundary")
            addHeader("Connection", if (params.files.size <= 1) "close" else "Keep-Alive")
        }
    }

    override fun onWriteRequestBody(bodyWriter: BodyWriter) {
        // 当正文准备写入时，重置上传的字节
        // 因为有时这会在网络更改时调用
        resetUploadedBytes()
        setAllFilesHaveBeenSuccessfullyUploaded(false)

        bodyWriter.apply {
            writeRequestParameters()
            writeFiles()
            write(trailerBytes)
        }
    }
}
