package com.dh.quickupload.network.okhttp


import com.dh.quickupload.UploadConfiguration
import com.dh.quickupload.network.NetworkRequest
import com.dh.quickupload.network.BaseNetwork
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OkHttp网络的实现.
 */
class OkHttpNetwork(
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cache(null)
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", UploadConfiguration.defaultUserAgent)
                    .build()
                chain.proceed(request)
            })
            .build()
) : BaseNetwork {
    @Throws(IOException::class)
    override fun createRequest(uploadId: String, method: String, url: String): NetworkRequest {
        return OkHttpNetworkRequest(uploadId, client, method, url)
    }
}
