package com.dh.quickupload.tools.datapreservation

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import org.json.JSONObject
import java.lang.IllegalArgumentException

/**
 * 实用程序类用于能够轻松地序列化/反序列化复杂和嵌套的数据使用
 * 只有一个平面键值映射。
 * 它支持序列化和反序列化:
 * - Json
 * - PersistableData
 */
open class PersistableData() : Parcelable {
    protected val data = HashMap<String, Any>()

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is PersistableData) return false

        return data == other.data
    }

    override fun hashCode() = data.hashCode()

    @SuppressLint("ParcelClassLoader")
    private constructor(parcel: Parcel) : this() {
        parcel.readBundle()?.let { bundle ->
            bundle.keySet().forEach { key ->
                when (val value = bundle[key]) {
                    is Boolean, is Double, is Int, is Long, is String -> data[key] = value
                }
            }
        }
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        toBundle().writeToParcel(dest, flags)
    }

    companion object CREATOR : Parcelable.Creator<PersistableData> {
        private const val separator = "$"
        override fun createFromParcel(parcel: Parcel) = PersistableData(parcel)
        override fun newArray(size: Int): Array<PersistableData?> = arrayOfNulls(size)

        /**
         * 从PersistableData JSON表示创建 [PersistableData]。
         */
        @JvmStatic
        fun fromJson(rawJsonString: String): PersistableData {
            val json = JSONObject(rawJsonString)
            val data = PersistableData()

            json.keys().forEach { key ->
                when (val value = json.get(key)) {
                    is Boolean, is Double, is Int, is Long, is String -> data.data[key] = value
                }
            }

            return data
        }
    }

    private fun String.validated(checkExists: Boolean = false): String {
        if (contains(separator))
            throw IllegalArgumentException("key cannot contain $separator as it's a reserved character, used for nested data")
        if (checkExists && !data.containsKey(this))
            throw IllegalArgumentException("no data found for key \"$this\"")
        return this
    }

    fun putBoolean(key: String, value: Boolean) {
        data[key.validated()] = value
    }

    fun getBoolean(key: String) = data[key.validated(checkExists = true)] as Boolean

    fun putDouble(key: String, value: Double) {
        data[key.validated()] = value
    }

    fun getDouble(key: String) = data[key.validated(checkExists = true)] as Double

    fun putInt(key: String, value: Int) {
        data[key.validated()] = value
    }

    fun getInt(key: String) = data[key.validated(checkExists = true)] as Int

    fun putLong(key: String, value: Long) {
        data[key.validated()] = value
    }

    fun getLong(key: String) = data[key.validated(checkExists = true)] as Long

    fun putString(key: String, value: String) {
        data[key.validated()] = value
    }

    fun getString(key: String) = data[key.validated(checkExists = true)] as String

    fun putData(key: String, data: PersistableData) {
        data.data.forEach { (dataKey, value) ->
            this.data["$key$separator$dataKey"] = value
        }
    }

    fun getData(key: String): PersistableData {
        val entries = data.entries.filter { it.key.startsWith("$key$separator") }
        if (entries.isEmpty()) return PersistableData()

        return PersistableData().also { extractedData ->
            entries.forEach { (entryKey, entryValue) ->
                extractedData.data[entryKey.removePrefix("$key$separator")] = entryValue
            }
        }
    }

    fun putArrayData(key: String, data: List<PersistableData>) {
        data.forEachIndexed { index, persistableData ->
            persistableData.data.forEach { (dataKey, value) ->
                this.data["$key$separator$index$separator$dataKey"] = value
            }
        }
    }

    fun getArrayData(key: String): List<PersistableData> {
        val entries = ArrayList(data.entries.filter { it.key.startsWith("$key$separator") })
        if (entries.isEmpty()) return emptyList()

        var index = 0

        var elements = entries.filter { it.key.startsWith("$key$separator$index$separator") }

        val outList = ArrayList<PersistableData>()

        while (elements.isNotEmpty()) {
            outList.add(PersistableData().also { extractedData ->
                elements.forEach { (entryKey, entryValue) ->
                    extractedData.data[entryKey.removePrefix("$key$separator$index$separator")] =
                        entryValue
                }
                entries.removeAll(elements)
            })

            index += 1
            elements = entries.filter { it.key.startsWith("$key$separator$index$separator") }
        }

        return outList
    }

    /**
     * 创建一个新的包，其中包含此 [PersistableData] 中存在的所有字段。
     */
    fun toBundle() = Bundle().also { bundle ->
        data.keys.forEach { key ->
            when (val value = data[key]) {
                is Boolean -> bundle.putBoolean(key, value)
                is Double -> bundle.putDouble(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is String -> bundle.putString(key, value)
            }
        }
    }

    /**
     * 创建一个包含所有字段的JSON字符串表示
     * 在此 [PersistableData] 中。
     *
     * 这并不意味着人类可读，而是一种方便的方式来传递复杂的
     * 使用字符串的结构化数据。
     */
    fun toJson() = JSONObject().also { json ->
        data.keys.forEach { key ->
            when (val value = data[key]) {
                is Boolean, is Double, is Int, is Long, is String -> json.put(key, value)
            }
        }
    }.toString()
}
