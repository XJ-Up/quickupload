package com.dh.updemo

data class FileItem(
    val fileName: String,
    val filePath: String,
    var uploadProgress: Int = 0,
    var uploadStatus: Int = 0   //0未开始，1等待中，2上传中,3取消上传 4上传完成
)