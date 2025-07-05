package com.example.leitor.data.annotation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.leitor.data.book.BookEntity
import java.time.LocalDateTime

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
data class BookAnnotationEntity @RequiresApi(Build.VERSION_CODES.O) constructor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val chapter: Int,
    val paragraph: Int,
    val startSelection: Int,
    val endSelection: Int,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val bookId: Int,
)