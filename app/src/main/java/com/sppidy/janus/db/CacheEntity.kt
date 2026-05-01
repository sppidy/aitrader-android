package com.sppidy.janus.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_cache")
data class CacheEntity(
    @PrimaryKey val key: String,
    val jsonPayload: String
)
