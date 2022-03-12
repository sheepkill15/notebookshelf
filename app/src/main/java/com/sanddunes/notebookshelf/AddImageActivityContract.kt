package com.sanddunes.notebookshelf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import com.sanddunes.notebookshelf.activities.EditBookActivity

class AddImageActivityContract : ActivityResultContract<Int, Uri?>() {

    private var inputValue: Int = -1

    companion object {
        const val PICK_INTENT_TYPE = "image/*"
    }

    override fun createIntent(context: Context, input: Int): Intent {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickIntent.type = PICK_INTENT_TYPE

    //    pickIntent.putExtra("com.sanddunes.notebookshelf.INTENT", input)
        inputValue = input
        return pickIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if(resultCode != Activity.RESULT_OK)
            return null

        val data = intent ?: return null

        when(inputValue) {
            EditBookActivity.ADD_IMAGE_CONTENT -> {

                    val uri = data.data as Uri
                    val filePathCollumn = arrayOf(MediaStore.Images.Media._ID)
                    val cursor =  EditBookActivity.instance.contentResolver.query(uri, filePathCollumn, null, null, null)

                    cursor?.let {
                        it.moveToFirst()
                        val collumnIndex = it.getColumnIndexOrThrow(filePathCollumn[0])
                        return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            it.getLong(collumnIndex).toString()
                        )
                    }
                    cursor?.close()
            }
        }
        return null
    }
}