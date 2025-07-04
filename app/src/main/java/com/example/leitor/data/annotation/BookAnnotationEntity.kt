package com.example.leitor.data.annotation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.leitor.data.book.BookEntity

@Entity(
    tableName = "BookAnnotation",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookAnnotationEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val bookId: Int,
)