package com.example.stickynotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val x: Int,
    val y: Int,
    val color: Int = 0xFFFFF9C4.toInt() // Default yellowish
)
