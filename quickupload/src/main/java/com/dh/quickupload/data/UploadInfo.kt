package com.dh.quickupload.data

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.ArrayList
import java.util.Date

@Parcelize
data class UploadInfo @JvmOverloads constructor(
    /**
     * 上传唯一ID
     */
    val uploadId: String,

    /**
     * 上传任务的开始时间戳 (以毫秒为单位)
     */
    val startTime: Long = 0,

    /**
     * 字节上传长度
     */
    val uploadedBytes: Long = 0,

    /**
     * 上传任务总字节数。
     */
    val totalBytes: Long = 0,

    /**
     * 在上传过程中进行的重试次数。
     * 如果未进行重试，则此值将为零。
     */
    val numberOfRetries: Int = 0,

    /**
     * 此上传中存在的所有文件的列表。
     */
    val files: ArrayList<UploadFile> = ArrayList()
) : Parcelable {

    /**
     * 获取上载任务的已用时间 (以毫秒为单位)。
     */
    @IgnoredOnParcel
    val elapsedTime: UploadElapsedTime
        get() {
            var seconds = ((Date().time - startTime) / 1000).toInt()
            val minutes = seconds / 60
            seconds -= 60 * minutes

            return UploadElapsedTime(minutes, seconds)
        }

    /**
     * 获取以Kb/s (每秒千位) 为单位的平均上传速率。
     */
    @IgnoredOnParcel
    val uploadRate: UploadRate
        get() {
            val elapsedSeconds = elapsedTime.totalSeconds

            // wait at least a second to stabilize the upload rate a little bit
            val kilobitPerSecond = if (elapsedSeconds < 1)
                0.0
            else
                uploadedBytes.toDouble() / 1000 * 8 / elapsedSeconds

            return when {
                kilobitPerSecond < 1 -> UploadRate(
                    value = (kilobitPerSecond * 1000).toInt(),
                    unit = UploadRate.UploadRateUnit.BitPerSecond
                )

                kilobitPerSecond >= 1000 -> UploadRate(
                    value = (kilobitPerSecond / 1000).toInt(),
                    unit = UploadRate.UploadRateUnit.MegabitPerSecond
                )

                else -> UploadRate(
                    value = kilobitPerSecond.toInt(),
                    unit = UploadRate.UploadRateUnit.KilobitPerSecond
                )
            }
        }

    /**
     * 获取以百分比表示的上载进度 (从0到100)。
     */
    @IgnoredOnParcel
    val progressPercent: Int
        get() = if (totalBytes == 0L) 0 else (uploadedBytes * 100 / totalBytes).toInt()

    @IgnoredOnParcel
    val successfullyUploadedFiles: Int
        get() = files.count { it.successfullyUploaded }
}
