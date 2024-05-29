package com.dh.quickupload.extensions

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Parcelable
import android.os.PowerManager
import android.webkit.MimeTypeMap
import com.dh.quickupload.UploadService
import com.dh.quickupload.UploadTask
import com.dh.quickupload.data.NameValue
import com.dh.quickupload.data.UploadTaskParameters
import com.dh.quickupload.tools.logger.Logger
import com.dh.quickupload.observer.task.UploadTaskObserver
import kotlinx.coroutines.CoroutineScope
import java.lang.IllegalStateException
import java.net.URL

fun ArrayList<NameValue>.addHeader(name: String, value: String) {
    add(NameValue(name, value).validateAsHeader())
}

fun LinkedHashMap<String, String>.setOrRemove(key: String, value: String?) {
    if (value == null) {
        remove(key)
    } else {
        this[key] = value
    }
}


/**
 * Context
 */
private const val taskParametersKey = "taskParameters"

fun Context.startNewUpload(
    params: UploadTaskParameters
): String {
    val intent = Intent(this, UploadService::class.java).apply {
        putExtra(taskParametersKey, params)
    }

    try {
        /*
      尝试在API 26上启动服务
        当应用程序在后台时，将触发IllegalStateException
         */
        startService(intent)
    } catch (exc: Throwable) {
        if (Build.VERSION.SDK_INT >= 26 && exc is IllegalStateException) {
            startForegroundService(intent)
        } else {
            Logger.error(
                component = "UploadService",
                uploadId = params.id,
                exception = exc,
                message = {
                    "Error starting"
                }
            )
        }
    }

    return params.id
}

data class UploadTaskCreationParameters(
    val params: UploadTaskParameters
)

fun Intent?.getUploadTaskCreationParameters(): UploadTaskCreationParameters? {
    if (this == null ) {
        Logger.error(
            component = UploadService.TAG,
            uploadId = Logger.NA,
            message = {
                "实例化新任务时出错。收到无效Intent"
            }
        )
        return null
    }

    val params: UploadTaskParameters = parcelableCompat(taskParametersKey) ?: run {
        Logger.error(
            component = UploadService.TAG,
            uploadId = Logger.NA,
            message = {
                "实例化新任务时出错。缺少任务参数"
            }
        )
        return null
    }

    val taskClass = try {
        Class.forName(params.taskClass)
    } catch (exc: Throwable) {
        Logger.error(
            component = UploadService.TAG,
            uploadId = Logger.NA,
            exception = exc,
            message = {
                "实例化新任务时出错。${params.taskClass} 不存在。"
            }
        )
        null
    } ?: return null

    if (!UploadTask::class.java.isAssignableFrom(taskClass)) {
        Logger.error(
            component = UploadService.TAG,
            uploadId = Logger.NA,
            message = {
                "实例化新任务时出错。${params.taskClass} 不扩展 UploadTask。"
            }
        )
        return null
    }

    return UploadTaskCreationParameters(
        params = params,
    )
}

/**
* 根据intent中请求的task类创建一个新的task实例。
* @ return task实例，如果task类不支持或无效，则返回null
*/
@Suppress("UNCHECKED_CAST")
fun Context.getUploadTask(
    creationParameters: UploadTaskCreationParameters,
    scope: CoroutineScope,
    vararg observers: UploadTaskObserver
): UploadTask? {
    return try {
        val taskClass = Class.forName(creationParameters.params.taskClass) as Class<out UploadTask>
        val uploadTask = taskClass.newInstance().apply {
            init(
                context = this@getUploadTask,
                taskParams = creationParameters.params,
                scope = scope,
                taskObservers = observers
            )
        }

        Logger.debug(
            component = UploadService.TAG,
            uploadId = Logger.NA,
            message = {
                "已成功创建具有类的新任务: ${taskClass.name}"
            }
        )
        uploadTask
    } catch (exc: Throwable) {
        Logger.error(
            component = UploadService.TAG,
            uploadId = Logger.NA,
            exception = exc,
            message = {
                "实例化新任务时出错"
            }
        )
        null
    }
}

// 调整Android 12的标志
fun flagsCompat(flags: Int): Int {
    if (Build.VERSION.SDK_INT > 30) {
        return flags or PendingIntent.FLAG_IMMUTABLE
    }

    return flags
}

inline fun <reified T : Parcelable> Intent.parcelableCompat(key: String): T? = when {
    Build.VERSION.SDK_INT >= 34 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= 34) {
        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(receiver, filter)
    }
}

/**
 * String
 */
internal const val APPLICATION_OCTET_STREAM = "application/octet-stream"
internal const val VIDEO_MP4 = "video/mp4"

/**
 * 尝试自动检测特定文件的内容类型 (MIME类型)。
 * @ param absolutePath文件的绝对路径
 * @ return文件的内容类型 (MIME类型)，如果没有内容，则为application/octet-stream
 * 类型可以自动确定
 */
fun String.autoDetectMimeType(): String {
    val index = lastIndexOf(".")

    return if (index in 0 until lastIndex) {
        val extension = substring(index + 1).lowercase()

        if (extension == "mp4") {
            VIDEO_MP4
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: APPLICATION_OCTET_STREAM
        }
    } else {
        APPLICATION_OCTET_STREAM
    }
}

fun String?.isASCII(): Boolean {
    if (this.isNullOrBlank())
        return false

    for (element in this) {
        if (element.code > 127) {
            return false
        }
    }

    return true
}

fun String.isValidHttpUrl(): Boolean {
    if (!startsWith("http://") && !startsWith("https://")) return false

    return try {
        URL(this)
        true
    } catch (exc: Throwable) {
        false
    }
}

val String.asciiBytes: ByteArray
    get() = toByteArray(Charsets.US_ASCII)

val String.utf8Bytes: ByteArray
    get() = toByteArray(Charsets.UTF_8)


/**
 * WakeLock
 */
fun PowerManager.WakeLock?.safeRelease() {
    this?.apply { if (isHeld) release() }
}

fun Context.acquirePartialWakeLock(
    currentWakeLock: PowerManager.WakeLock?,
    tag: String
): PowerManager.WakeLock {
    if (currentWakeLock?.isHeld == true) {
        return currentWakeLock
    }

    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

    return powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag).apply {
        setReferenceCounted(false)
        if (!isHeld) acquire()
    }
}
