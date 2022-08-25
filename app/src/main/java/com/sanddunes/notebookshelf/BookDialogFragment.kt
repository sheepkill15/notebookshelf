package com.sanddunes.notebookshelf

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sanddunes.notebookshelf.activities.EditBookActivity
import com.sanddunes.notebookshelf.activities.MainActivity
import com.sanddunes.notebookshelf.room.BookData

class BookDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(data: BookData): BookDialogFragment {
            val args = Bundle()
            args.putSerializable(ARGS_BOOKDATA, data)
            val newDialogFragment = BookDialogFragment()
            newDialogFragment.arguments = args
            return newDialogFragment
        }

        const val ARGS_BOOKDATA = "bookData"
        const val LAST_EDITED = "Last edited: "
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val data: BookData = arguments?.getSerializable(ARGS_BOOKDATA) as BookData

        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            builder.setTitle(data.title)
                .setMessage(LAST_EDITED + data.lastEdited)
                .setPositiveButton(R.string.edit) { _, _ ->
                    run {
                        val intent = Intent(MainActivity.instance, EditBookActivity::class.java)
                        intent.putExtra(EditBookActivity.INTENT_BOOKID, data.id)
                        startActivity(intent)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            MainActivity.instance.registerOpenedBook(data.id!!)
                        }
                    }
                }
                .setNegativeButton(R.string.delete) { _, _ ->  MainActivity.instance.removeBook(data.id!!)}


            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}