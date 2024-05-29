package com.dh.quickupload.exceptions

import com.dh.quickupload.network.ServerResponse

class UserCancelledUploadException : Throwable("用户已取消上传")
class UploadError(val serverResponse: ServerResponse) : Throwable("上传错误")
class NoNetworkException: Throwable("网络连接断开")
