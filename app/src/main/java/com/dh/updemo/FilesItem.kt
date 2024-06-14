package com.dh.updemo

import com.dh.quickupload.observer.task.UploadObserverBase
/**
 * 多个地址文件上传示例
 * 使用：
 * 继承 UploadObserverBase()
 *
 */
data class FilesItem(
    val fileName: String,
    val filePath: MutableList<String>
): UploadObserverBase()
