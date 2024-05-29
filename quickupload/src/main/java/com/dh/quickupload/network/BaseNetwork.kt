package com.dh.quickupload.network

import java.io.IOException

interface BaseNetwork {
    /**
     * 为给定的URL和HTTP方法创建一个新的连接。
     * @ param uploadId请求此连接的上载的ID
     * @ param方法HTTP方法
     * @ param url要连接到的URL
     * @ return新连接对象
     * @ 如果在创建连接对象时发生错误，则抛出IOException
     */
    @Throws(IOException::class)
    fun createRequest(uploadId: String, method: String, url: String): NetworkRequest
}
