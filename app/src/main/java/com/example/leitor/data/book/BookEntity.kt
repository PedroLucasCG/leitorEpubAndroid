package com.example.leitor.data.book

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Book")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookUri: String,
    val bookTitle: String?,
    val bookAuthor: String?,
    val imageUri: String?
)