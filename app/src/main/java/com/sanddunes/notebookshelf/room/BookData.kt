package com.sanddunes.notebookshelf.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "books")
data class BookData(
    @PrimaryKey(autoGenerate = true)
    var id: Int?,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "last_edited")
    val lastEdited: String
) : Serializable