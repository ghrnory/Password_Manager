package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owners")
data class Owner(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isMe: Boolean
)
