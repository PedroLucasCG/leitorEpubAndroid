package com.example.leitor.data.crossRef

import androidx.room.Entity

@Entity(
    tableName = "BookCategoryCrossRef",
    primaryKeys = ["bookId", "categoryId"]
)
data class BookCategoryCrossRef(
    val bookId: Int,
    val categoryId: Int
)