package com.dh.quickupload.tools.datapreservation

interface Persistable {
    fun toPersistableData(): PersistableData

    interface Creator<T> {
        fun createFromPersistableData(data: PersistableData): T
    }
}
