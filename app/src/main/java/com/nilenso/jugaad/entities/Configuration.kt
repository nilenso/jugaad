package com.nilenso.jugaad.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Configuration(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "value") val value: String?,
)