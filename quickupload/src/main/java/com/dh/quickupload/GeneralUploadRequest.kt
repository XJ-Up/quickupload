package com.dh.quickupload

import android.content.Context
import android.util.Base64
import com.dh.quickupload.data.HttpUploadTaskParameters
import com.dh.quickupload.data.NameValue
import com.dh.quickupload.extensions.addHeader
import com.dh.quickupload.extensions.isValidHttpUrl

/**
 * 表示一般的HTTP上载请求。<br></br>
 * 子类创建您自己的自定义HTTP上传请求。
 * @ param context应用程序上下文
 * @ param serverUrl处理请求的服务器端脚本的URL
 *
 */
abstract class GeneralUploadRequest<B : GeneralUploadRequest<B>>(context: Context, serverUrl: String) :
    UploadRequest<B>(context, serverUrl) {

    private val httpParams = HttpUploadTaskParameters()
    init {
        require(serverUrl.isValidHttpUrl()) { "Specify either http:// or https:// as protocol" }
    }

    override fun getAdditionalParameters() = httpParams.toPersistableData()

    /**
     * 向此上传请求添加标头。
     *
     * @ param headerName标头名称
     * @ param headervvalue标头值
     * @ return self实例
     */
    fun addHeader(headerName: String, headerValue: String): B {
        httpParams.requestHeaders.addHeader(headerName, headerValue)
        return self()
    }

    /**
     * 设置HTTP基本身份验证标头。
     * @ param用户名HTTP基本身份验证用户名
     * @ param密码HTTP基本身份验证密码
     * @ return self实例
     */
    fun setBasicAuth(username: String, password: String): B {
        val auth = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
        return addHeader("Authorization", "Basic $auth")
    }

    /**
     * 使用令牌设置HTTP承载身份验证。
     * @ param bearerToken持有者授权令牌
     * @ return self实例
     */
    fun setBearerAuth(bearerToken: String): B {
        return addHeader("Authorization", "Bearer $bearerToken")
    }

    /**
     * 为该上传请求添加一个参数。
     *
     * @ param paramName参数名
     * @ Paramparamvalue参数值
     * @ return self实例
     */
    open fun addParameter(paramName: String, paramValue: String): B {
        httpParams.requestParameters.add(NameValue(paramName, paramValue))
        return self()
    }

    /**
     * 将具有多个值的参数添加到此上传请求中。
     *
     * @ param paramName参数名
     * @ param数组值
     * @ return self实例
     */
    open fun addArrayParameter(paramName: String, vararg array: String): B {
        for (value in array) {
            httpParams.requestParameters.add(NameValue(paramName, value))
        }
        return self()
    }

    /**
     * 将具有多个值的参数添加到此上传请求中。
     *
     * @ param paramName参数名
     * @ param列表值
     * @ return self实例
     */
    open fun addArrayParameter(paramName: String, list: List<String>): B {
        for (value in list) {
            httpParams.requestParameters.add(NameValue(paramName, value))
        }
        return self()
    }

    /**
     * 设置要使用的HTTP方法。默认情况下，它设置为POST。
     *
     * @ param方法使用新的HTTP方法
     * @ return self实例
     */
    fun setMethod(method: String): B {
        httpParams.method = method.uppercase()
        return self()
    }

    /**
     * 设置此上传请求是否使用固定长度流模式。
     * 默认设置为true。
     * 如果它使用固定长度流模式，则由返回的值
     * [GeneralUploadTask.getBodyLength] 将自动用于正确设置
     * 底层 [java.net.HttpURLConnection]，否则将使用chunk流模式。
     * @ param fixedLength true使用固定长度流模式 (这是默认设置) 或
     * false使用分块流模式。
     * @ return self实例
     */
    fun setUsesFixedLengthStreamingMode(fixedLength: Boolean): B {
        httpParams.usesFixedLengthStreamingMode = fixedLength
        return self()
    }
}
