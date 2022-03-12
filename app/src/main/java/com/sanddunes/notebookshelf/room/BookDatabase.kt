package com.sanddunes.notebookshelf.room

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(entities = [BookData::class], version = 1, exportSchema = false)
abstract class BookDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        private lateinit var bookDatabaseInstance: BookDatabase
        fun getDatabase(context: Context): BookDatabase
        {
            if(!this::bookDatabaseInstance.isInitialized)
            {
                synchronized(BookDatabase::class) {
                    if(!this::bookDatabaseInstance.isInitialized)
                    {
                        bookDatabaseInstance = Room.databaseBuilder(context.applicationContext, BookDatabase::class.java, "book_database").build()
                    }
                }

            }
            return bookDatabaseInstance
        }
    }

    override fun createOpenHelper(config: DatabaseConfiguration?): SupportSQLiteOpenHelper {
        TODO("Not yet implemented")
    }

    override fun createInvalidationTracker(): InvalidationTracker {
        TODO("Not yet implemented")
    }

    override fun clearAllTables() {
        TODO("Not yet implemented")
    }
}