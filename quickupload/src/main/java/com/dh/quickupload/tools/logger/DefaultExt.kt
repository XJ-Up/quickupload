package com.dh.quickupload.tools.logger

import android.util.Log

class DefaultExt : Logger.Ext {

    companion object {
        private const val TAG = "QuickUpload"
    }

    override fun error(component: String, uploadId: String, message: String, exception: Throwable?) {
        Log.e(TAG, "$component - (uploadId: $uploadId) - $message", exception)
    }

    override fun debug(component: String, uploadId: String, message: String) {
        Log.i(TAG, "$component - (uploadId: $uploadId) - $message")
    }

    override fun info(component: String, uploadId: String, message: String) {
        Log.i(TAG, "$component - (uploadId: $uploadId) - $message")
    }
}
