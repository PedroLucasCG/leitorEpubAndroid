package com.example.leitor.data.category

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryName: String
)