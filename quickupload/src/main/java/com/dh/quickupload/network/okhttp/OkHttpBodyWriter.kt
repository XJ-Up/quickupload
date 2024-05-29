package com.dh.quickupload.network.okhttp

import com.dh.quickupload.network.BodyWriter
import okio.BufferedSink
import java.io.IOException

class OkHttpBodyWriter(private val sink: BufferedSink, listener: OnStreamWriteListener) :
    BodyWriter(listener) {
    @Throws(IOException::class)
    override fun internalWrite(bytes: ByteArray) {
        sink.write(bytes)
    }

    @Throws(IOException::class)
    override fun internalWrite(bytes: ByteArray, lengthToWriteFromStart: Int) {
        sink.write(bytes, 0, lengthToWriteFromStart)
    }

    @Throws(IOException::class)
    override fun flush() {
        sink.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        sink.close()
    }
}
