package com.dh.quickupload

import android.app.Application
import android.os.Build
import com.dh.quickupload.data.RetryPolicyConfig
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.network.BaseNetwork
import com.dh.quickupload.network.okhttp.OkHttpNetwork
import com.dh.quickupload.observer.network.NetworkMonitor
import com.dh.quickupload.tools.translationfile.ContentResolverSchemeHandler
import com.dh.quickupload.tools.translationfile.FileSchemeHandler
import com.dh.quickupload.tools.translationfile.SchemeHandler
import kotlinx.coroutines.asCoroutineDispatcher
import java.lang.reflect.InvocationTargetException
import java.util.LinkedHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object UploadConfiguration {

    private const val fileScheme = "/"
    private const val contentScheme = "content://"

    /**
     * 默认Http堆栈构造函数使用的默认用户代理。
     */
    const val defaultUserAgent = "AndroidUploadService/1.0.0"

    private val schemeHandlers by lazy {
        LinkedHashMap<String, Class<out SchemeHandler>>().apply {
            this[fileScheme] = FileSchemeHandler::class.java
            this[contentScheme] = ContentResolverSchemeHandler::class.java
        }
    }

    /**
     * 使用命名空间和默认通知通道初始化上传服务。
     * 这必须在你的应用程序子类的onCreate方法之前做任何事情。
     * @ param context您的应用程序的上下文
     * @ param defaultNotificationChannel要使用的默认通知通道
     * @ param debug将其设置为您的BuildConfig.DEBUG
     */
    @JvmStatic
    fun initialize(context: Application, defaultNotificationChannel: String, debug: Boolean) {
        this.namespace = context.packageName
        this.defaultNotificationChannel = defaultNotificationChannel
        Logger.setDevelopmentMode(debug)
    }

    /**
     * 上传服务将要运行的命名空间。这必须在应用程序中设置
     * 子类的onCreate方法之前的任何东西。
     */
    @JvmStatic
    var namespace: String? = null
        private set
        get() = if (field == null)
            throw IllegalArgumentException("您必须在应用程序子类中将命名空间设置为您的应用程序包名称 (context.packageName)")
        else
            field

    /**
     * 要使用的默认通知通道。这必须在应用程序中设置
     * 子类的onCreate方法之前的任何东西。
     */
    @JvmStatic
    var defaultNotificationChannel: String? = null
        private set
        get() = if (field == null)
            throw IllegalArgumentException("您必须在应用程序子类中设置defaultNotificationChannel")
        else
            field

    /**
     * 创建自定义调度器
     */
    val dispatcher =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).apply {
            // 设置线程池的 keep-alive 时间
            (this as ThreadPoolExecutor).setKeepAliveTime(5, TimeUnit.SECONDS)
            allowCoreThreadTimeOut(true)  // 允许核心线程超时
        }.asCoroutineDispatcher()


    /**
     *  Semaphore 以限制最大并发任务数
     */
    @JvmStatic
    var maxConcurrentTasks = 2 // 设置最大并发任务数
        set(value) {
            require(value < 1) { "任务数必须大于1" }
            field = value
        }
    val semaphore = Semaphore(maxConcurrentTasks)

    /**
     *  创建网络状态监听
     *  当网络断开时立即停止全部上传
     */
    @JvmStatic
    var networkListening: (Application) -> NetworkMonitor = {
        NetworkMonitor(it)
    }
    /**
     * 在关闭服务之前要等待多少空闲时间。
     * 服务在运行时处于空闲状态，但没有任务正在运行。
     */
    @JvmStatic
    var idleTimeoutSeconds = 10
        set(value) {
            require(value >= 1) { "空闲超时最小允许值为1。不能是 $value" }
            field = value
        }
    /**
     * 上传任务用于数据传输的缓冲区大小 (以字节为单位)。
     */
    @JvmStatic
    var bufferSizeBytes = 4096
        set(value) {
            require(value >= 256) { "您不能将缓冲区大小设置为低于256字节" }
            field = value
        }
    /**
     * 配置上传网络
     */
    @JvmStatic
    var network: BaseNetwork = OkHttpNetwork()

    /**
     * 以毫秒为单位的进度通知之间的间隔。
     * 如果上传任务报告的频率超过此值，则上传服务将自动应用限制。
     * 默认为每秒3次更新
     */
    @JvmStatic
    var uploadProgressNotificationIntervalMillis: Long = 1000 / 3

    /**
     * 设置上传服务重试策略。有关详细信息，请参阅 [RetryPolicyConfig] 文档
     * 每个参数的解释。
     */
    @JvmStatic
    var retryPolicy = RetryPolicyConfig(
        initialWaitTimeSeconds = 1,
        maxWaitTimeSeconds = 100,
        multiplier = 2,
        defaultMaxRetries = 3
    )

    /**
     * 如果设置为true，服务将在做上传时在前台模式，
     * 降低被低内存系统杀死的概率。
     * 仅当您的上传具有通知配置时才使用此设置。
     * 这是不可能在没有通知的情况下在前台运行，根据Android政策
     * 约束，所以如果你设置为true，但你上传任务没有
     * 通知配置，该服务将简单地在后台模式下运行。
     *
     * 注意: 从Android Oreo (API 26) 开始，此设置被忽略，因为它始终必须为true，
     * 因为服务必须在前台运行并向用户公开通知。
     */
    @JvmStatic
    var isForegroundService = true
        get() = Build.VERSION.SDK_INT >= 26 || field



    /**
     * 注册一个自定义方案处理程序。
     * 您不能覆盖现有的文件和内容: // 方案。
     * @ param scheme要支持的方案 (例如content://，yourCustomScheme://)
     * @ param处理程序方案处理程序实现
     */
    @JvmStatic
    fun addSchemeHandler(scheme: String, handler: Class<out SchemeHandler>) {
        require(!(scheme == fileScheme || scheme == contentScheme)) { "无法覆盖: $scheme!" }
        schemeHandlers[scheme] = handler
    }

    @Throws(
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class,
        InstantiationException::class
    )
    @JvmStatic
    fun getSchemeHandler(path: String): SchemeHandler {
        val trimmedPath = path.trim()

        for ((scheme, handler) in schemeHandlers) {
            if (trimmedPath.startsWith(scheme, ignoreCase = true)) {
                return handler.newInstance().apply {
                    init(trimmedPath)
                }
            }
        }

        throw UnsupportedOperationException("$ path不支持的方案。当前支持的方案为 ${schemeHandlers.keys}")
    }

}
