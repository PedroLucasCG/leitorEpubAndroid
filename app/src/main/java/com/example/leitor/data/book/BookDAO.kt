package com.example.leitor.data.book

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

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

    @Query("SELECT * FROM Book WHERE id = :id")
    fun getById(id: Int): BookEntity?

    @Query("SELECT * FROM Book WHERE bookTitle LIKE '%' || :query || '%'")
    fun searchByTitle(query: String): List<BookEntity>

    @Transaction
    @Query("SELECT * FROM Book WHERE id = :bookId")
    fun getBookWithCategories(bookId: Int): BookWithCategories
}