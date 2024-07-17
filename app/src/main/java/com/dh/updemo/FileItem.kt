package com.dh.updemo

import com.dh.quickupload.observer.task.UploadObserverBase

/**
 * 单个地址文件上传示例
 * 使用：
 * 继承 UploadObserverBase()
 *
 */
data class FileItem(
    val fileName: String,
    val filePath: String,
    override val uploadId: String,
) : UploadObserverBase(uploadId)