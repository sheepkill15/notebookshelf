package com.sanddunes.notebookshelf.activities

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.sanddunes.notebookshelf.*
import com.sanddunes.notebookshelf.drive.DriveHelper
import com.sanddunes.notebookshelf.room.BookData
import smartdevelop.ir.eram.showcaseviewlib.GuideView
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

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
            DividerItemDecoration(bookshelf.context, viewManager.orientation)

        dividerItemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider)!!)

        bookshelf.addItemDecoration(dividerItemDecoration)

        bookshelf.apply {
            layoutManager = viewManager
        }
        loadBooksAndShelves()
        val prefs = getSharedPreferences("com.sanddunes.notebookshelf", Context.MODE_PRIVATE)

        val notFirstOpen = prefs.getBoolean("com.sanddunes.notebookshelf.opened", false)

        requestSignIn()
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

    private fun requestSignIn() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, 400)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            400 -> {
                if(resultCode == RESULT_OK) {
                    handleSignInIntent(data!!)
                }
            }
        }
    }

    private fun handleSignInIntent(intent: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(intent)
            .addOnSuccessListener {
                val credential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE_APPDATA))
                credential.selectedAccount = it.account
                val driveService = Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory(),
                    credential
                ).setApplicationName("Notebookshelf")
                    .build()
                DriveHelper.service = driveService
                val prefs = getSharedPreferences("com.sanddunes.notebookshelf", Context.MODE_PRIVATE)
                if(!prefs.getBoolean("com.sanddunes.notebookshelf.synced", false)) {
                    val backgroundExecutor = Executors.newSingleThreadExecutor()
                    backgroundExecutor.execute {
                        DriveHelper.getSaveFiles()
                        backgroundExecutor.shutdown()
                    }
                    prefs.edit().putBoolean("com.sanddunes.notebookshelf.synced", true).apply()
                }

            }
            .addOnFailureListener {

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
                adapter.notifyItemChanged(0);
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