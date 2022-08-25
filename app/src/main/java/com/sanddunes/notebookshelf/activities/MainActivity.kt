package com.sanddunes.notebookshelf.activities

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.sanddunes.notebookshelf.FileManager
import com.sanddunes.notebookshelf.MyViewModel
import com.sanddunes.notebookshelf.R
import com.sanddunes.notebookshelf.ShelfAdapter
import com.sanddunes.notebookshelf.room.BookData
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {

        lateinit var shelfLayout: ArrayList<ArrayList<Int>>

        lateinit var instance: MainActivity
        lateinit var viewModel: MyViewModel
        lateinit var formatter: SimpleDateFormat
        lateinit var filePath: String

        fun isViewModelInitialized(): Boolean {
            return this::viewModel.isInitialized
        }

        fun isMainActivityInitialized(): Boolean {
            return this::instance.isInitialized
        }
    }

    private lateinit var bookshelf: RecyclerView
    private lateinit var adapter: ShelfAdapter
    private lateinit var coordinatorLayout: CoordinatorLayout

    private var lastDeletedBook: BookData? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        instance = this
        formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        filePath = filesDir.path + "/data.dat"

        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        bookshelf = findViewById(R.id.bookshelf)
        coordinatorLayout = findViewById(R.id.coordinator)
        val viewManager = LinearLayoutManager(this)
        viewModel = MyViewModel(application)

        val dividerItemDecoration =
            DividerItemDecoration(bookshelf.context, viewManager.orientation).apply {
                setDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.divider)!!)
            }

        bookshelf.apply {
            layoutManager = viewManager
            addItemDecoration(dividerItemDecoration)
        }
        loadBooksAndShelves()

        val prefs = getSharedPreferences("com.sanddunes.notebookshelf", Context.MODE_PRIVATE)
        val notFirstOpen = prefs.getBoolean("com.sanddunes.notebookshelf.opened", false)
        if(!notFirstOpen) {
            GuideView.Builder(this)
                .setContentText(resources.getString(R.string.tutorial_addbook_content))
                .setTargetView(findViewById<FloatingActionButton>(R.id.addButton))
                .setDismissType(DismissType.anywhere)
                .setGuideListener {
                    GuideView.Builder(this)
                        .setContentText(resources.getString(R.string.tutorial_add_shelf_content))
                        .setTargetView(findViewById<FloatingActionButton>(R.id.addShelf))
                        .setDismissType(DismissType.anywhere)
                        .build()
                        .show()
                }
                .build()
                .show()
            prefs.edit().putBoolean("com.sanddunes.notebookshelf.opened", true).apply()
        }

    }


    fun removeBook(id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            viewModel.removeShortcutOf(id)
        }
        var whereWasI = 0
        var whereWasJ = 0
        for (i in shelfLayout.indices) {
            if (shelfLayout[i].contains(id)) {
                whereWasI = i
                whereWasJ = shelfLayout[i].indexOf(id)
                shelfLayout[i].remove(id)
                break
            }
        }
        adapter.notifyItemChanged(whereWasI)
//        adapter.notifyDataSetChanged()
        viewModel.delete(id) {
            lastDeletedBook = it
            val snackbar = Snackbar.make(
                coordinatorLayout,
                it.title + " " + resources.getString(R.string.deleted),
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction(R.string.undo) {
                val newBook = lastDeletedBook!!
                val handler = Handler(Looper.getMainLooper())
                viewModel.insert(newBook) {
                    handler.post {
                        shelfLayout[whereWasI].add(whereWasJ, it)
                        FileManager.saveToFile(filePath, shelfLayout)
//                        adapter.notifyDataSetChanged()
                        adapter.notifyItemChanged(whereWasI)
                    }
                }
            }
            snackbar.show()
        }
        FileManager.saveToFile(filePath, shelfLayout)
    }

    fun createBook(v: View) {

        val newBook = BookData(
            null,
            resources.getString(R.string.new_book),
            "",
            formatter.format(Calendar.getInstance().time)
        )
        val handler = Handler(Looper.getMainLooper())
        viewModel.insert(newBook) {
            handler.post {
                shelfLayout[0].add(it)
                FileManager.saveToFile(filePath, shelfLayout)

                /*       val newAdapter = ShelfAdapter(
                       this,
                       shelfLayout
                   )
                   bookshelf.adapter = newAdapter*/
//                adapter.notifyDataSetChanged()
                adapter.notifyItemChanged(0)
            }
        }

    }

    fun createShelf(v: View) {
        shelfLayout.add(arrayListOf())
        /*val newAdapter = ShelfAdapter(
            this,
            shelfLayout
        )
        bookshelf.adapter = newAdapter*/
//        adapter.notifyDataSetChanged()
        adapter.notifyItemInserted(shelfLayout.size - 1)
        FileManager.saveToFile(filePath, shelfLayout)
    }

    fun moveBook(oldIndex: Int, newIndex: Int, which: Int) {
        val whatWas: Int = shelfLayout[oldIndex][which]
        shelfLayout[oldIndex].removeAt(which)
        shelfLayout[newIndex].add(whatWas)

//        adapter.notifyDataSetChanged()
        adapter.notifyItemChanged(oldIndex)
        adapter.notifyItemChanged(newIndex)

        FileManager.saveToFile(filePath, shelfLayout)
    }

    private fun loadBooksAndShelves() {
        shelfLayout = FileManager.readFromFile(filePath)

        adapter = ShelfAdapter(
            this,
            shelfLayout
        )
        bookshelf.adapter = adapter
    }

    fun getBook(id: Int, callback: (BookData) -> Unit) {
        viewModel.getById(id, callback)
    }

    ///// SHORTCUTS
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun registerOpenedBook(id: Int) {
        viewModel.makeShortcutOf(id)
    }
}