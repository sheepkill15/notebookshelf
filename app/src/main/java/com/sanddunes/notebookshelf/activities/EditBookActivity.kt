package com.sanddunes.notebookshelf.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.sanddunes.notebookshelf.AddImageActivityContract
import com.sanddunes.notebookshelf.ConfirmDialogFragment
import com.sanddunes.notebookshelf.MyViewModel
import com.sanddunes.notebookshelf.R
import com.sanddunes.notebookshelf.room.BookData
import com.sanddunes.notebookshelf.views.LineTextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EditBookActivity : AppCompatActivity() {


    companion object {
        const val ADD_IMAGE_CONTENT: Int = 1
        const val DATE_FORMAT = "dd MMM yyyy, HH:mm"
        const val INTENT_BOOKID = "com.sanddunes.notebookshelf.BOOK_ID"
        const val INSTANCE_NOTEDATA = "com.sanddunes.notebookshelf.NOTE_DATA"
        const val INSTANCE_NOTEEDITED = "com.sanddunes.notebookshelf.NOTE_EDITED"

        lateinit var formatter: SimpleDateFormat

        lateinit var instance: EditBookActivity
    }

    private lateinit var titleInput: TextInputEditText
    private lateinit var contentInput: LineTextInputEditText

    private lateinit var saveButton: MenuItem

    private lateinit var handler: Handler

    private var edited: Boolean = false
    private var initialized: Boolean = false

    private var bookId: Int = 0

    private lateinit var checkedVector: Drawable
    private lateinit var disabledCheckVector: Drawable

    private val requestPermissionManager = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if(it) {
            makeImagePicker()
        }
    }
    private val openAddImageCustom = registerForActivityResult(AddImageActivityContract()) {
        if(it != null)
        {
            contentInput.insertImageToCursor(LineTextInputEditText.getBitmap(it), null, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_book)
        instance = this
        formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())

        val toolbar: Toolbar = findViewById(R.id.noteEditToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        checkedVector = ContextCompat.getDrawable(this, R.drawable.ic_check)!!
        disabledCheckVector = ContextCompat.getDrawable(this, R.drawable.ic_check_disabled)!!

        toolbar.setNavigationOnClickListener { exit() }
        saveButton = toolbar.menu.findItem(R.id.save_button)

        bookId = intent.getIntExtra(INTENT_BOOKID, 0)

        titleInput = findViewById(R.id.titleInput)
        contentInput = findViewById(R.id.contentInput)
        handler = Handler(Looper.getMainLooper())

        if(savedInstanceState != null) {
            val data = savedInstanceState.getSerializable(INSTANCE_NOTEDATA) as BookData
            setUpActivity(data)

            edited = savedInstanceState.getBoolean(INSTANCE_NOTEEDITED, false)
        }
        else {
            if (!MainActivity.isViewModelInitialized())
                MainActivity.viewModel = MyViewModel(application)
            MainActivity.viewModel.getById(bookId) { data -> handler.post { setUpActivity(data); } }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(INSTANCE_NOTEEDITED, edited)
        outState.putSerializable(INSTANCE_NOTEDATA, BookData(null, titleInput.text.toString(), contentInput.text.toString(), ""))
    }

    override fun onBackPressed() {
        exit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.note_edit_appbar, menu)
        saveButton = menu.findItem(R.id.save_button)
        updateSaveButton()
        initialized = true
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        R.id.save_button -> {
            save()
            true
        }
        R.id.add_image -> {

            if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
            {
                requestPermissionManager.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else makeImagePicker()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun makeImagePicker()
    {
        openAddImageCustom.launch(ADD_IMAGE_CONTENT)
    }

    private fun setUpActivity(data: BookData) {

        val images = arrayListOf<Bitmap>()
        val startIndices = arrayListOf<Int>()
        val uris = arrayListOf<Uri>()

        var content: String = data.content

        for(i in content.indices step LineTextInputEditText.TOKEN.length ) {
            if(i >= content.length) break
            if (content[i] in LineTextInputEditText.TOKEN) {
                val start = i - LineTextInputEditText.TOKEN.indexOf(content[i])

                val end = content.indexOfAny(
                    charArrayOf(' ', '\n', '\r', '\t'),
                    start + LineTextInputEditText.TOKEN.length
                )
                try {
                    val uri = Uri.parse(content.substring(start + LineTextInputEditText.TOKEN.length, end))
                    val loaded = LineTextInputEditText.getBitmap(uri)
                    if (loaded != null) {
                        images.add(loaded)
                        startIndices.add(start)
                        uris.add(uri)
                    }
                    else {
                        content = content.removeRange(start, end + 2)
                    }
                } catch (ex: Exception) {
                    ex.message?.let { Log.e("URI", it) }
                }
            }
        }

        titleInput.text = SpannableStringBuilder(data.title)
        contentInput.text = SpannableStringBuilder(content)

        for(i in images.indices)
        {
            contentInput.insertImageToCursor(images[i], startIndices[i], uris[i], false)
        }

        titleInput.doAfterTextChanged {
            if (!edited && initialized) {
                edited = true
                updateSaveButton()
            }
        }
        contentInput.doAfterTextChanged {
            if (!edited && initialized) {
                edited = true
                updateSaveButton()
            }
        }
    }

    private fun save() {
        val newBook = BookData(
            bookId,
            titleInput.text.toString(),
            contentInput.text.toString(),
            formatter.format(Calendar.getInstance().time)
        )
        if (newBook.title.isEmpty()) {
            Toast.makeText(
                applicationContext,
                resources.getText(R.string.title_cant_be_blank),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        MainActivity.viewModel.updateBook(newBook) {
            handler.post {
                Toast.makeText(
                    applicationContext,
                    resources.getText(R.string.successful_save),
                    Toast.LENGTH_SHORT
                ).show()
                edited = false
                updateSaveButton()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    if(MainActivity.isMainActivityInitialized())
                        MainActivity.instance.registerOpenedBook(bookId)
                    else MainActivity.viewModel.makeShortcutOf(bookId)
                }
            }
        }

    }

    private fun exit() {

        if(edited)
        {
            createConfirmDialog()
        }
        else {
            if (!MainActivity.isMainActivityInitialized()) {
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }
            finish()
        }
    }

    private fun createConfirmDialog() {
        val dialog = ConfirmDialogFragment()
        dialog.callingActivity = this
        dialog.show(supportFragmentManager, "")
    }

    private fun updateSaveButton() {
        saveButton.isEnabled = edited
        saveButton.icon = if (edited) checkedVector else disabledCheckVector
    }

    fun saveAndExit()
    {
        val newBook = BookData(
            bookId,
            titleInput.text.toString(),
            contentInput.text.toString(),
            formatter.format(Calendar.getInstance().time)
        )
        if (newBook.title.isEmpty()) {
            Toast.makeText(
                applicationContext,
                resources.getText(R.string.title_cant_be_blank),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        MainActivity.viewModel.updateBook(newBook) {
            handler.post {
                Toast.makeText(
                    applicationContext,
                    resources.getText(R.string.successful_save),
                    Toast.LENGTH_SHORT
                ).show()
                edited = false
                updateSaveButton()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    if(MainActivity.isMainActivityInitialized())
                        MainActivity.instance.registerOpenedBook(bookId)
                    else MainActivity.viewModel.makeShortcutOf(bookId)

                }
                exit()
            }
        }
    }

    fun exitWithoutSave()
    {
        edited = false
        exit()
    }
}