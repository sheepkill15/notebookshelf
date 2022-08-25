package com.sanddunes.notebookshelf.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.sanddunes.notebookshelf.BookDialogFragment
import com.sanddunes.notebookshelf.activities.MainActivity
import com.sanddunes.notebookshelf.listeners.MyLongClickListener

class BookView : AppCompatImageView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, flags: Int) : super(context, attrs, flags)

    var index1: Int = -1

    init {
        setOnLongClickListener(MyLongClickListener())
    }

    fun setBook(index: Int)
    {
        setOnClickListener {
            MainActivity.instance.getBook(index) {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    val dialog =
                        BookDialogFragment.newInstance(
                            it
                        )
                    dialog.show(MainActivity.instance.supportFragmentManager, "")
                }
            }
        }
    }
}