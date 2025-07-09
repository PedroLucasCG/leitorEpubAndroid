package com.example.leitor.data.book

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "Book")
data class BookEntity @RequiresApi(Build.VERSION_CODES.O) constructor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookUri: String,
    val bookTitle: String?,
    val bookAuthor: String?,
    val imageUri: String?,
    val createdAt: LocalDateTime = LocalDateTime.now()
)