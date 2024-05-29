package com.dh.quickupload.network.okhttp

import com.dh.quickupload.data.NameValue
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.network.BodyWriter
import com.dh.quickupload.network.NetworkRequest
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URL
import java.util.UUID

/**
 * [NetworkRequest] 使用OkHttpClient实现。
 */
class OkHttpNetworkRequest(
    private val uploadId: String,
    private val httpClient: OkHttpClient,
    private val httpMethod: String,
    url: String
) : NetworkRequest {

    private val requestBuilder = Request.Builder().url(URL(url))
    private var bodyLength = 0L
    private var contentType: MediaType? = null
    private val uuid = UUID.randomUUID().toString()

    init {
        Logger.debug(javaClass.simpleName, uploadId) {
            "创建新的OkHttp连接 (uuid: $uuid)"
        }
    }

    @Throws(IOException::class)
    override fun setHeaders(requestHeaders: List<NameValue>): NetworkRequest {
        for (param in requestHeaders) {
            if ("content-type" == param.name.trim().lowercase())
                contentType = param.value.trim().toMediaTypeOrNull()

            requestBuilder.header(param.name.trim(), param.value.trim())
        }

        return this
    }

    override fun setTotalBodyBytes(
        totalBodyBytes: Long,
        isFixedLengthStreamingMode: Boolean
    ): NetworkRequest {
        bodyLength = if (isFixedLengthStreamingMode) totalBodyBytes else -1

        return this
    }

    private fun createBody(
        delegate: NetworkRequest.RequestBodyDelegate,
        listener: BodyWriter.OnStreamWriteListener
    ): RequestBody? {
        if (!httpMethod.hasBody()) return null

        return object : RequestBody() {
            override fun contentLength() = bodyLength

            override fun contentType() = contentType

            override fun writeTo(sink: BufferedSink) {
                OkHttpBodyWriter(sink, listener).use {
                    delegate.onWriteRequestBody(it)
                }
            }
        }
    }

    private fun request(
        delegate: NetworkRequest.RequestBodyDelegate,
        listener: BodyWriter.OnStreamWriteListener
    ) = requestBuilder
        .method(httpMethod, createBody(delegate, listener))
        .build()

    @Throws(IOException::class)
    override fun getResponse(
        delegate: NetworkRequest.RequestBodyDelegate,
        listener: BodyWriter.OnStreamWriteListener
    ) = use {
        httpClient.newCall(request(delegate, listener))
            .execute()
            .use { it.asServerResponse() }
    }

    override fun close() {
        // 资源在使用后自动释放。仅记录。
        Logger.debug(javaClass.simpleName, uploadId) {
            "正在关闭OkHttp连接 (uuid: $uuid)"
        }
    }
}
