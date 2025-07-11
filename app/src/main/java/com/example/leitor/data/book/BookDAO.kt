package com.example.leitor.data.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.leitor.data.crossRef.BookCategoryCrossRef

@Dao
interface BookDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(book: BookEntity)

    @Update
    fun update(book: BookEntity)

    @Delete
    fun delete(book: BookEntity)

    @Query("SELECT * FROM Book")
    fun getAll(): List<BookEntity>

    @Query("SELECT * FROM Book ORDER BY bookTitle COLLATE NOCASE ASC")
    fun getAllByTitle(): List<BookEntity>

    @Query("SELECT * FROM Book ORDER BY createdAt ASC")
    fun getAllByCreatedAt(): List<BookEntity>

    @Query("SELECT * FROM Book WHERE id = :id")
    fun getById(id: Int): BookEntity?

    @Query("SELECT * FROM Book WHERE bookTitle LIKE '%' || :query || '%'")
    fun searchByTitle(query: String): List<BookEntity>

    @Transaction
    @Query("SELECT * FROM Book WHERE id = :bookId")
    fun getBookWithCategories(bookId: Int): BookWithCategories

    @Transaction
    @Query("""
    SELECT * FROM Book
    WHERE id IN (
        SELECT bookId FROM BookCategoryCrossRef
        WHERE categoryId IN (:categoryIds)
    )
    """)
    fun getBooksByCategories(categoryIds: List<Int>): List<BookEntity>

    @Transaction
    @Query("""
    SELECT * FROM Book
    WHERE id IN (
        SELECT bookId FROM BookCategoryCrossRef
        WHERE categoryId IN (:categoryIds)
    )
    ORDER BY bookTitle COLLATE NOCASE ASC
    """)
    fun getBooksByCategoriesByTitle(categoryIds: List<Int>): List<BookEntity>

    @Transaction
    @Query("""
    SELECT * FROM Book
    WHERE id IN (
        SELECT bookId FROM BookCategoryCrossRef
        WHERE categoryId IN (:categoryIds)
    )
    ORDER BY createdAt ASC
    """)
    fun getBooksByCategoriesByCreatedAt(categoryIds: List<Int>): List<BookEntity>

    @Query("DELETE FROM BookCategoryCrossRef WHERE bookId = :bookId")
    fun clearCategories(bookId: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertCrossRef(ref: BookCategoryCrossRef)
}