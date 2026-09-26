package com.mustime.core.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { item ->
            val trimmed = item.trim()
            if (trimmed.isEmpty()) null else trimmed
        }
    }
}
