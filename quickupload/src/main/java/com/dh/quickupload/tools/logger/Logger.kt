package com.dh.quickupload.tools.logger

object Logger {
    private var logLevel = LogLevel.Off
    private val defaultLogger = DefaultExt()
    private var loggerDelegate: Ext = defaultLogger

    internal const val NA = "N/A"

    enum class LogLevel {
        Debug,
        Info,
        Error,
        Off
    }

    interface Ext {
        fun error(component: String, uploadId: String, message: String, exception: Throwable?)
        fun debug(component: String, uploadId: String, message: String)
        fun info(component: String, uploadId: String, message: String)
    }

    @Synchronized
    @JvmStatic
    fun setDelegate(delegate: Ext?) {
        loggerDelegate = delegate ?: defaultLogger
    }

    @Synchronized
    @JvmStatic
    fun setLogLevel(level: LogLevel) {
        logLevel = level
    }

    @Synchronized
    @JvmStatic
    fun setDevelopmentMode(devModeOn: Boolean) {
        logLevel = if (devModeOn) LogLevel.Debug else LogLevel.Off
    }

    private fun loggerWithLevel(minLevel: LogLevel) =
        if (logLevel > minLevel || logLevel == LogLevel.Off) null else loggerDelegate

    @JvmOverloads
    @JvmStatic
    fun error(component: String, uploadId: String, exception: Throwable? = null, message: () -> String) {
        loggerWithLevel(LogLevel.Error)?.error(component, uploadId, message(), exception)
    }

    @JvmStatic
    fun info(component: String, uploadId: String, message: () -> String) {
        loggerWithLevel(LogLevel.Info)?.info(component, uploadId, message())
    }

    @JvmStatic
    fun debug(component: String, uploadId: String, message: () -> String) {
        loggerWithLevel(LogLevel.Debug)?.debug(component, uploadId, message())
    }
}
