package com.dh.quickupload.data

/**
 * 上传时间
 */
data class UploadElapsedTime(val minutes: Int, val seconds: Int) {
    val totalSeconds: Int
        get() = minutes * 60 + seconds
}
