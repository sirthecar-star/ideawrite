package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTagList(tags: List<String>?): String {
        if (tags.isNullOrEmpty()) return ""
        return tags.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toTagList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
