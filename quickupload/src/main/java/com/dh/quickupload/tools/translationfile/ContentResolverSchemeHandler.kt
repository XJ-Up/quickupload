package com.dh.quickupload.tools.translationfile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.dh.quickupload.extensions.APPLICATION_OCTET_STREAM
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.tools.logger.Logger.NA
import java.io.File
import java.io.IOException

internal class ContentResolverSchemeHandler : SchemeHandler {

    private lateinit var uri: Uri

    override fun init(path: String) {
        uri = Uri.parse(path)
    }

    override fun size(context: Context): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val sizeColumn = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0) it.getLong(sizeColumn) else null
            } else {
                null
            }
        } ?: run {
            Logger.error(javaClass.simpleName, NA) { "没有 ${uri}的游标数据，返回大小为0" }
            0L
        }
    }

    override fun stream(context: Context) = context.contentResolver.openInputStream(uri)
        ?: throw IOException("无法打开 ${uri}的输入流")

    override fun contentType(context: Context): String {
        val type = context.contentResolver.getType(uri)

        return if (type.isNullOrBlank()) {
            APPLICATION_OCTET_STREAM
        } else {
            type
        }
    }

    override fun name(context: Context): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val displayNameColumn = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameColumn >= 0) it.getString(displayNameColumn) else null
            } else {
                null
            }
        } ?: uri.toString().split(File.separator).last()
    }

    override fun delete(context: Context) = try {
        context.contentResolver.delete(uri, null, null) > 0
    } catch (exc: Throwable) {
        Logger.error(javaClass.simpleName, NA, exc) { "文件删除错误" }
        false
    }
}
