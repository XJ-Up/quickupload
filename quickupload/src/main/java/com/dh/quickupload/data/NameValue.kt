package com.dh.quickupload.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.dh.quickupload.extensions.isASCII
import com.dh.quickupload.tools.datapreservation.Persistable
import com.dh.quickupload.tools.datapreservation.PersistableData

@Parcelize
data class NameValue(val name: String, val value: String) : Parcelable, Persistable {
    fun validateAsHeader(): NameValue {
        require(name.isASCII() && value.isASCII()) {
            "标头 ${name}及其值 ${value}必须仅为ASCII!"
        }

        return this
    }

    override fun toPersistableData() = PersistableData().apply {
        putString(CodingKeys.name, name)
        putString(CodingKeys.value, value)
    }

    companion object : Persistable.Creator<NameValue> {
        private object CodingKeys {
            const val name = "name"
            const val value = "value"
        }

        override fun createFromPersistableData(data: PersistableData) = NameValue(
            name = data.getString(CodingKeys.name),
            value = data.getString(CodingKeys.value)
        )
    }
}
