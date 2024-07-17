package com.dh.quickupload

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.dh.quickupload.extensions.acquirePartialWakeLock
import com.dh.quickupload.extensions.getUploadTask
import com.dh.quickupload.extensions.getUploadTaskCreationParameters
import com.dh.quickupload.extensions.safeRelease
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.observer.task.TaskCompletionNotifier
import com.dh.quickupload.observer.task.UploadTaskObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap

class UploadService : Service(), CoroutineScope by MainScope() {

    companion object {
        internal val TAG = UploadService::class.java.simpleName

        private const val UPLOAD_NOTIFICATION_BASE_ID = 10086


        private val uploadTasksMap = ConcurrentHashMap<String, UploadTask>()

        @Volatile
        private var foregroundUploadId: String? = null



        /**
         * 使用给定的uploadId停止上传任务。
         * @ param uploadId唯一的上传id
         */
        @Synchronized
        @JvmStatic
        fun stopUpload(uploadId: String) {
            uploadTasksMap[uploadId]?.cancel()
        }

        /**
         * 获取当前活动的上传任务的列表。
         * @ 如果当前没有任务正在运行，则返回uploadIDs列表或空列表
         */
        @JvmStatic
        val taskList: List<String>
            @Synchronized get() = if (uploadTasksMap.isEmpty()) {
                emptyList()
            } else {
                uploadTasksMap.keys().toList()
            }
        /**
         * 保存上传观察者对象集合,通过继承 UploadObserverBase 添加到observers即可
         */
        @JvmStatic
        val observers:MutableList<UploadTaskObserver> = mutableListOf()


        /**
         * 停止所有活动的上传。
         */
        @Synchronized
        @JvmStatic
        fun stopAllUploads() {
            val iterator = uploadTasksMap.keys.iterator()

            while (iterator.hasNext()) {
                uploadTasksMap[iterator.next()]?.cancel()
            }
        }

        @Synchronized
        @JvmStatic
        fun noNetworkStopAllUploads() {
            val iterator = uploadTasksMap.keys.iterator()

            while (iterator.hasNext()) {
                val uploadTask = uploadTasksMap[iterator.next()]
                uploadTask?.noNetwork = true
                uploadTask?.cancel()
            }
        }

        /**
         * 停止服务。
         * @ param context应用程序上下文
         * @ param forceStop如果为true，则无论某些任务是否正在运行，都会停止服务，否则
         * 停止只有当没有任何活动的任务
         * @ return如果服务停止，则返回true，否则返回false
         */
        @Synchronized
        @JvmOverloads
        @JvmStatic
        fun stop(context: Context, forceStop: Boolean = false) = if (forceStop) {
            stopAllUploads()
            context.stopService(Intent(context, UploadService::class.java))

        } else {
            uploadTasksMap.isEmpty() && context.stopService(
                Intent(
                    context,
                    UploadService::class.java
                )
            )
        }

    }
    private var wakeLock: PowerManager.WakeLock? = null
    private var idleTimer: Timer? = null
    private val taskObservers: Array<UploadTaskObserver>
        get() {
            if (!observers.contains(TaskCompletionNotifier(this))) {
                observers.add(TaskCompletionNotifier(this))
            }
            return observers.toTypedArray()
        }
    private val networkListening by lazy {
        UploadConfiguration.networkListening(this)
    }

    @Synchronized
    private fun clearIdleTimer() {
        idleTimer?.apply {
            Logger.info(TAG, Logger.NA) { "清除空闲计时器" }
            cancel()
        }
        idleTimer = null
    }

    @Synchronized
    private fun shutdownIfThereArentAnyActiveTasks(): Int {
        if (uploadTasksMap.isEmpty()) {
            clearIdleTimer()

            Logger.info(TAG, Logger.NA) {
                "服务将在 ${UploadConfiguration.idleTimeoutSeconds} 秒内关闭如果没有收到新任务"
            }

            idleTimer = Timer(TAG + "IdleTimer").apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        Logger.info(TAG, Logger.NA) {
                            "服务即将停止，因为空闲超时为已达到 ${UploadConfiguration.idleTimeoutSeconds}s"
                        }
                        stopSelf()
                    }
                }, (UploadConfiguration.idleTimeoutSeconds * 1000).toLong())
            }

            return START_NOT_STICKY
        }

        return START_STICKY
    }

    /**
     * 检查任务当前是否为前台通知中显示的任务。
     * @ param上传的ID
     * @ 如果当前上传任务持有前台通知，则返回true，否则为false
     */
    @Synchronized
    fun holdForegroundNotification(uploadId: String, notification: Notification): Boolean {
        if (!UploadConfiguration.isForegroundService) return false

        if (foregroundUploadId == null) {
            foregroundUploadId = uploadId
            Logger.debug(TAG, uploadId) { "现在保留前台通知" }
        }

        if (uploadId == foregroundUploadId) {
            startForeground(UPLOAD_NOTIFICATION_BASE_ID, notification)
            return true
        }

        return false
    }

    private fun stopServiceForeground() {
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    /**
     * 由每个任务完成时调用 (成功，错误或由于
     * 用户取消)。
     * @ param uploadId完成任务的uploadID
     */
    @Synchronized
    fun taskCompleted(uploadId: String) {
        val task = uploadTasksMap.remove(uploadId)

        // un-hold foreground upload ID if it's been hold
        if (UploadConfiguration.isForegroundService && task != null && task.params.id == foregroundUploadId) {
            Logger.debug(TAG, uploadId) { "现在未保留的前台通知" }
            foregroundUploadId = null
        }

        if (UploadConfiguration.isForegroundService && uploadTasksMap.isEmpty()) {
            Logger.debug(
                TAG,
                Logger.NA
            ) { "所有任务已完成，停止前台执行" }
            stopServiceForeground()
            shutdownIfThereArentAnyActiveTasks()
        }
    }

    override fun onCreate() {
        super.onCreate()

        wakeLock = acquirePartialWakeLock(wakeLock, TAG)
        networkListening.register()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.debug(TAG, Logger.NA) {
            "正在启动UploadService。调试信息: $UploadConfiguration"
        }

        val builder =
            NotificationCompat.Builder(this, UploadConfiguration.defaultNotificationChannel!!)
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .setOngoing(true)
                .setGroup(UploadConfiguration.namespace)

        if (Build.VERSION.SDK_INT >= 31) {
            builder.foregroundServiceBehavior = Notification.FOREGROUND_SERVICE_IMMEDIATE
        }

        val notification = builder.build()

        startForeground(UPLOAD_NOTIFICATION_BASE_ID, notification)

        val taskCreationParameters = intent.getUploadTaskCreationParameters()
            ?: return shutdownIfThereArentAnyActiveTasks()

        if (uploadTasksMap.containsKey(taskCreationParameters.params.id)) {
            Logger.error(TAG, taskCreationParameters.params.id) {
                "防止上传!具有相同ID的上载已在进行中。每次上传都必须有唯一的ID。请检查您的代码!"
            }
            return shutdownIfThereArentAnyActiveTasks()
        }



        val currentTask = getUploadTask(
            creationParameters = taskCreationParameters,
            scope = this,
            observers = taskObservers
        ) ?: return shutdownIfThereArentAnyActiveTasks()

        clearIdleTimer()
        uploadTasksMap[currentTask.params.id] = currentTask
        currentTask.start()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        networkListening.unregister()
        stopAllUploads()

        if (UploadConfiguration.isForegroundService) {
            Logger.debug(
                TAG,
                Logger.NA
            ) { "停止前台执行" }
            stopServiceForeground()
        }

        wakeLock.safeRelease()

        uploadTasksMap.clear()

        Logger.debug(TAG, Logger.NA) { "UploadService已销毁" }
    }
}