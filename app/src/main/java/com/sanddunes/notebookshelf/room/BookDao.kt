package com.sanddunes.notebookshelf.room

import androidx.room.*

@Dao
interface BookDao {

    @Query("SELECT * FROM books")
    fun getAll(): List<BookData>

    @Query("SELECT * FROM books WHERE id IN (:bookIds)")
    fun loadAllByIds(bookIds: IntArray): List<BookData>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun findById(bookId: Int): BookData

    @Query("SELECT id FROM books")
    fun getAllIds(): List<Int>

 /*   @Query("SELECT * FROM user WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): BookData*/

    @Insert
    fun insertAll(vararg books: BookData)

    @Insert
    fun insert(book: BookData): Long

    @Delete
    fun delete(book: BookData)

    @Query("DELETE FROM books WHERE id = :id")
    fun deleteById(id: Int)

    @Update
    fun updateBooks(vararg books: BookData)
}