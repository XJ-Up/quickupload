package com.dh.quickupload.network

import android.util.Log
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import com.dh.quickupload.UploadConfiguration

abstract class BodyWriter(private val listener: OnStreamWriteListener) : Closeable {

    /**
     * 接收流写入进度并具有取消它的能力。
     */
    interface OnStreamWriteListener {
        /**
         * 指示是否应继续将流写入正文。
         * @ return true继续将流写入正文，false取消
         */
        fun shouldContinueWriting(): Boolean

        /**
         * 每次将一堆字节写入正文时调用
         * @ param字节写入的字节数
         */
        fun onBytesWritten(bytesWritten: Int)
    }

    /**
     * 将输入流写入请求正文。
     * 流将在成功写入或抛出异常后自动关闭。
     * @ param stream要从中读取的输入流
     * @ 如果发生I/O错误，则抛出IOException
     */
    @Throws(IOException::class)
    fun writeStream(stream: InputStream ,end :Long,start:Long) {
        val buffer = ByteArray(UploadConfiguration.bufferSizeBytes)
        stream.use {
            it.skip(start)
            var bytesRead=0
            var bytesRemaining = end - start
            while (listener.shouldContinueWriting() && bytesRemaining > 0 && it.read(buffer, 0, minOf(buffer.size.toLong(), bytesRemaining).toInt()).also { bytesRead = it }!= -1 ) {
                write(buffer, bytesRead)
                bytesRemaining -= bytesRead.toLong()
            }
        }
    }

    /**
     * 将一个字节数组写入请求正文。
     * @ param字节数组与字节写入
     * @ 如果在写入时出现错误，则抛出IOException
     */
    fun write(bytes: ByteArray) {
        internalWrite(bytes)
        flush()
        listener.onBytesWritten(bytes.size)
    }

    /**
     * 将字节数组的一部分写入请求正文。
     * @ param字节数组与字节写入
     * @ param lengthtowriefromstart写多少字节，从第一个开始
     * 数组
     * @ 如果在写入时出现错误，则抛出IOException
     */
    fun write(bytes: ByteArray, lengthToWriteFromStart: Int) {
        internalWrite(bytes, lengthToWriteFromStart)
        flush()
        listener.onBytesWritten(lengthToWriteFromStart)
    }

    @Throws(IOException::class)
    abstract fun internalWrite(bytes: ByteArray)

    @Throws(IOException::class)
    abstract fun internalWrite(bytes: ByteArray, lengthToWriteFromStart: Int)

    /**
     * 确保写入正文的字节全部传输到服务器并清除
     * 本地缓冲区。
     * @ 如果在刷新缓冲区时发生错误，则抛出IOException
     */
    @Throws(IOException::class)
    abstract fun flush()
}
