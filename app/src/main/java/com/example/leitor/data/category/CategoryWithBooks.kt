package com.example.leitor.data.category

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.leitor.data.book.BookEntity
import com.example.leitor.data.crossRef.BookCategoryCrossRef

data class CategoryWithBooks(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = BookCategoryCrossRef::class,
            parentColumn = "categoryId",
            entityColumn = "bookId"
        )
    )
    val books: List<BookEntity>
)