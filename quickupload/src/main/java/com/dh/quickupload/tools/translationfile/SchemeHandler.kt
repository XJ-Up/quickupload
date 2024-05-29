package com.dh.quickupload.tools.translationfile

import android.content.Context
import java.io.InputStream

interface SchemeHandler {
    /**
     * 使用文件路径初始化实例。
     */
    fun init(path: String)

    /**
     * 获取文件大小 (以字节为单位)。
     */
    fun size(context: Context): Long

    /**
     *获取文件输入流以读取它
     */
    fun stream(context: Context): InputStream

    /**
     * 获取文件内容类型
     */
    fun contentType(context: Context): String

    /**
     * 获取文件名
     */
    fun name(context: Context): String

    /**
     * 删除文件
     */
    fun delete(context: Context): Boolean
}
