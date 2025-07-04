package com.example.leitor.data.book

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.leitor.data.category.CategoryEntity
import com.example.leitor.data.crossRef.BookCategoryCrossRef

data class BookWithCategories(
    @Embedded val book: BookEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCategoryCrossRef::class,
            parentColumn = "bookId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity>
)