package com.example.leitor.data.annotation

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AnnotationDAO {

    @Insert
    fun insert(annotation: BookAnnotationEntity)

    @Update
    fun update(annotation: BookAnnotationEntity)

    @Delete
    fun delete(annotation: BookAnnotationEntity)

    @Query("SELECT * FROM BookAnnotation WHERE bookId = :bookId")
    fun getAnnotationsByBookId(bookId: Int): List<BookAnnotationEntity>

    @Query("SELECT * FROM BookAnnotation WHERE id = :id")
    fun getById(id: Int): BookAnnotationEntity?

    @Query("SELECT * FROM BookAnnotation WHERE bookId = :bookId AND chapter = :chapter")
    fun getHighlightsForChapter(bookId: Int, chapter: Int): List<BookAnnotationEntity>

    @Query("SELECT * FROM BookAnnotation")
    fun getAll(): MutableList<BookAnnotationEntity>?
}