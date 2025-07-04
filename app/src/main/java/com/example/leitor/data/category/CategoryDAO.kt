package com.example.leitor.data.category

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface CategoryDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(category: CategoryEntity)

    @Update
    fun update(category: CategoryEntity)

    @Delete
    fun delete(category: CategoryEntity)

    @Query("SELECT * FROM Category ORDER BY categoryName ASC")
    fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM Category WHERE id = :id")
    fun getById(id: Int): CategoryEntity?

    @Transaction
    @Query("SELECT * FROM Category WHERE id = :categoryId")
    fun getCategoryWithBooks(categoryId: Int): CategoryWithBooks
}