package com.sanddunes.notebookshelf

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanddunes.notebookshelf.activities.EditBookActivity
import com.sanddunes.notebookshelf.room.BookDao
import com.sanddunes.notebookshelf.room.BookData
import com.sanddunes.notebookshelf.room.BookDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MyViewModel(private val myApplication: Application) : AndroidViewModel(myApplication) {

    private val bookDatabase: BookDatabase = BookDatabase.getDatabase(myApplication)
    private val bookDao: BookDao = bookDatabase.bookDao()

    fun insert(book: BookData, callback: ((id:Int) -> Unit)?) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = bookDao.insert(book).toInt()
            callback?.let { it(id) }
//            DriveHelper.setSaveFiles()
        }
    }

    fun delete(book: BookData) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.delete(book)
        }
    }

    fun delete(id: Int, callback: ((BookData) -> Unit)? = null)
    {
        viewModelScope.launch(Dispatchers.IO)
        {
            getById(id) { data ->
                bookDao.deleteById(id)
                callback?.let {
                    it(data)
                }
            }
//            DriveHelper.setSaveFiles()
        }
    }

    fun save(books: ArrayList<BookData>) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateBooks(*books.toTypedArray())
        }
    }

    fun getAll(callback: (ArrayList<BookData>) -> Unit) {
        var allBooks: ArrayList<BookData>
        runBlocking {
            viewModelScope.launch(Dispatchers.IO) {
                allBooks = bookDao.getAll() as ArrayList<BookData>
                callback(allBooks)
            }
        }
    }

    fun getAllIds(callback: ((List<Int>) -> Unit)) {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = bookDao.getAllIds()
            callback(ids)
        }
    }

    fun getById(id: Int, callback: (BookData) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val book = bookDao.findById(id)
            callback(book)
        }
    }

    fun updateBook(book: BookData, callback: (() -> Unit)? = null)
    {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateBooks(book)
            if (callback != null) {
                callback()
            }
//            DriveHelper.setSaveFiles()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun makeShortcutOf(id: Int)
    {
        getById(id) {data -> finishShortcutOf(data)}
    }
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun finishShortcutOf(data: BookData)
    {
        viewModelScope.launch(Dispatchers.IO) {

            val shortcutManager = getSystemService(
                myApplication,
                ShortcutManager::class.java
            )

            val shortcut = ShortcutInfo.Builder(myApplication, data.id.toString())
                .setShortLabel(data.title)
                .setLongLabel(data.title)
                .setIcon(Icon.createWithResource(myApplication, R.drawable.ic_bookmark))
                .setIntent(Intent(Intent.ACTION_MAIN, Uri.EMPTY, myApplication,
                        EditBookActivity::class.java
                    ).putExtra(EditBookActivity.INTENT_BOOKID, data.id)).build()

            val oldShortcuts = shortcutManager!!.dynamicShortcuts
            val existsNewInOld = oldShortcuts.indexOfFirst { x -> x.id == shortcut.id }
            if (existsNewInOld != -1) {
                oldShortcuts.removeAt(existsNewInOld)
            }
            oldShortcuts.add(shortcut)
            shortcutManager.dynamicShortcuts = oldShortcuts
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun removeShortcutOf(id: Int)
    {
        viewModelScope.launch {
            val shortcutManager = getSystemService(myApplication ,ShortcutManager::class.java)

            val oldShorcuts = shortcutManager!!.dynamicShortcuts

            val indexOfShortcut = oldShorcuts.indexOfFirst { x -> x.id == id.toString() }

            if(indexOfShortcut != -1)
            {
                oldShorcuts.removeAt(indexOfShortcut)
            }
            shortcutManager.dynamicShortcuts = oldShorcuts
        }
    }

}