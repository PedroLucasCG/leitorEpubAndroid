package com.example.leitor.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.leitor.data.annotation.AnnotationDAO
import com.example.leitor.data.annotation.BookAnnotationEntity
import com.example.leitor.data.annotation.DateTimeConverters
import com.example.leitor.data.book.BookDAO
import com.example.leitor.data.book.BookEntity
import com.example.leitor.data.category.CategoryDAO
import com.example.leitor.data.category.CategoryEntity
import com.example.leitor.data.crossRef.BookCategoryCrossRef

@Database(
    entities = [
        BookEntity::class,
        CategoryEntity::class,
        BookCategoryCrossRef::class,
        BookAnnotationEntity::class],
    version = 6
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDAO
    abstract fun categoryDao(): CategoryDAO
    abstract fun annotationDao(): AnnotationDAO

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}