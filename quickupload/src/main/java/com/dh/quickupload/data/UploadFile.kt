package com.dh.quickupload.data

import android.os.Parcelable
import java.util.LinkedHashMap
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import com.dh.quickupload.UploadConfiguration
import com.dh.quickupload.tools.datapreservation.Persistable
import com.dh.quickupload.tools.datapreservation.PersistableData
import com.dh.quickupload.tools.translationfile.SchemeHandler

@Parcelize
data class UploadFile @JvmOverloads constructor(
    val path: String,
    val properties: LinkedHashMap<String, String> = LinkedHashMap()
) : Parcelable, Persistable {

    companion object : Persistable.Creator<UploadFile> {
        private const val successfulUpload = "successful_upload"

        private object CodingKeys {
            const val path = "path"
            const val properties = "props"
        }

        override fun createFromPersistableData(data: PersistableData) = UploadFile(
            path = data.getString(CodingKeys.path),
            properties = LinkedHashMap<String, String>().apply {
                val bundle = data.getData(CodingKeys.properties).toBundle()
                bundle.keySet().forEach { propKey ->
                    put(propKey, bundle.getString(propKey)!!)
                }
            }
        )
    }

    @IgnoredOnParcel
    val handler: SchemeHandler by lazy {
        UploadConfiguration.getSchemeHandler(path)
    }

    @IgnoredOnParcel
    var successfullyUploaded: Boolean
        get() = properties[successfulUpload]?.toBoolean() ?: false
        set(value) {
            properties[successfulUpload] = value.toString()
        }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.path, path)
        putData(CodingKeys.properties, PersistableData().apply {
            properties.entries.forEach { (propKey, propVal) ->
                putString(propKey, propVal)
            }
        })
    }
}
