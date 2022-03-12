package com.sanddunes.notebookshelf

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sanddunes.notebookshelf.activities.EditBookActivity

class ConfirmDialogFragment : DialogFragment() {

    var callingActivity: EditBookActivity? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)

            builder.setTitle(R.string.exit_prompt)
                .setMessage(R.string.unsaved_changes)
                .setPositiveButton(R.string.save_and_exit) { _, _ -> callingActivity?.saveAndExit() }
                .setNegativeButton(R.string.exit_without_save) { _, _ -> callingActivity?.exitWithoutSave() }
                .setNeutralButton(R.string.cancel) { _, _ ->  }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}