package com.dh.quickupload.network

import android.os.Parcelable
import java.io.Serializable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerResponse(
    /**
     * 服务器响应响应代码。如果您正在实现非HTTP
     * 协议，将此设置为200以通知任务已完成
     * 成功。小于200或大于299表示的整数值
     * 来自服务器的错误响应。
     */
    val code: Int,

    /**
     * 服务器响应正文。
     * 如果你的服务器响应一个字符串，你可以得到它
     * 如果字符串是JSON，则可以使用org.json等库对其进行解析
     * 如果你的服务器没有返回任何东西，设置为空数组。
     */
    val body: ByteArray,
    /**
     * 服务器响应标头
     */
    val headers: LinkedHashMap<String, String>
) : Parcelable, Serializable {

    /**
     * 获取服务器响应正文作为字符串。
     * 如果字符串是JSON，则可以使用org.json等库对其进行解析
     * @ 返回字符串
     */
    @IgnoredOnParcel
    val bodyString: String
        get() = String(body)

    @IgnoredOnParcel
    val isSuccessful: Boolean
        get() = code in 200..399

    companion object {
        fun successfulEmpty(): ServerResponse {
            return ServerResponse(
                code = 200, body = ByteArray(1), headers = LinkedHashMap()
            )
        }
    }
}
