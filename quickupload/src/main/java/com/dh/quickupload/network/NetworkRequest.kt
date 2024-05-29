package com.dh.quickupload.network

import java.io.Closeable
import java.io.IOException
import com.dh.quickupload.data.NameValue

interface NetworkRequest : Closeable {

    /**
     * 当Body准备好被写时，委托被调用。
     */
    interface RequestBodyDelegate {

        /**
         * 处理请求正文的写入。
         * @ param bodyWriter用于在正文上写入的对象
         * @ 如果在写入正文时发生错误，则抛出IOException
         */
        @Throws(IOException::class)
        fun onWriteRequestBody(bodyWriter: BodyWriter)
    }

    /**
     * 设置请求标头。
     * @ param requestHeaders要设置的请求标头
     * @ 如果在设置请求头时发生错误，则抛出IOException
     * @ return实例
     */
    @Throws(IOException::class)
    fun setHeaders(requestHeaders: List<NameValue>): NetworkRequest

    /**
     * 设置总body字节数。
     * @ param totalBodyBytes总字节数
     * @ param isFixedLengthStreamingMode如果必须使用固定长度流模式，则为true。如果
     * 这是假的，必须使用chunked流模式。
     * @ return实例
     */
    fun setTotalBodyBytes(totalBodyBytes: Long, isFixedLengthStreamingMode: Boolean): NetworkRequest

    /**
     * 获取服务器响应。
     * @ return对象，包含服务器响应状态、标头和正文。
     * @ param委托处理请求正文的写入
     * @ param监听器，它在写入字节时得到通知，并控制是否
     * @ 如果在获取服务器响应时发生错误，则抛出IOException
     */
    @Throws(IOException::class)
    fun getResponse(
        delegate: RequestBodyDelegate,
        listener: BodyWriter.OnStreamWriteListener
    ): ServerResponse
}
