package com.dh.quickupload.tools.translationfile

import android.content.Context
import com.dh.quickupload.extensions.autoDetectMimeType
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.tools.logger.Logger.NA
import java.io.File
import java.io.FileInputStream
import java.io.IOException

internal class FileSchemeHandler : SchemeHandler {
    private lateinit var file: File

    override fun init(path: String) {
        file = File(path)
    }

    override fun size(context: Context) = file.length()

    override fun stream(context: Context) = FileInputStream(file)

    override fun contentType(context: Context) = file.absolutePath.autoDetectMimeType()

    override fun name(context: Context) = file.name
        ?: throw IOException("无法获取 ${file.absolutePath} 的文件名")

    override fun delete(context: Context) = try {
        file.delete()
    } catch (exc: Throwable) {
        Logger.error(javaClass.simpleName, NA, exc) { "文件删除错误" }
        false
    }
}
