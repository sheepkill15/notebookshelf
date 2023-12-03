package com.sanddunes.notebookshelf.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.core.text.getSpans
import androidx.core.view.ContentInfoCompat
import androidx.core.view.ViewCompat
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import com.google.android.material.textfield.TextInputEditText
import com.sanddunes.notebookshelf.CenteredImageSpan
import com.sanddunes.notebookshelf.activities.EditBookActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URL
import kotlin.math.floor
import kotlin.math.max

class LineTextInputEditText(passedContext: Context, attrs: AttributeSet) : TextInputEditText(passedContext, attrs) {

    private val mPaint: Paint = Paint().apply { color = Color.BLACK }
    private val mRect: Rect = Rect()


    private var spanLength: Int = -1


    init {
        mPaint.style = Paint.Style.FILL_AND_STROKE
    /*    val displayMetrics = DisplayMetrics()
        val activity = MainActivity.instance
        activity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        maxImageSize = min(displayMetrics.widthPixels, displayMetrics.heightPixels)*/

        doBeforeTextChanged { _, start, count, after ->
            if(start == 0) return@doBeforeTextChanged
            if(count > after)
            {
                val spans = editableText.getSpans<CenteredImageSpan>(start + count, start + count)
                if(spans.isEmpty()) return@doBeforeTextChanged

                for(i in spans.indices)
                {
                    val end = editableText.getSpanEnd(spans[i])
                    if(end != start + count) continue
                    val newText = spans[i].source
                    spanLength = newText?.length?.minus(1) ?: -1
                    editableText.removeSpan(spans[i])
                }
            }
        }

        doOnTextChanged { _, start, _, _ ->
            if(spanLength > -1)
            {
                val length = spanLength
                spanLength = -1
                editableText.replace(start - length, start, "")
            }
        }
    }

    companion object {
        private const val maxImageSize: Int = 720
        fun getBitmap(uri: Uri):Bitmap? {
            try {
                var input = if(!uri.scheme.equals("https")) EditBookActivity.instance.contentResolver.openInputStream(uri) else URL(uri.toString()).openStream()

                val onlyBoundsOptions = BitmapFactory.Options()
                onlyBoundsOptions.inJustDecodeBounds = true
                onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
                BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
                input?.close()

                if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1))
                    return null
                val originalSize: Int = max(onlyBoundsOptions.outHeight, onlyBoundsOptions.outWidth)

                val ratio: Double =
                    if (originalSize > maxImageSize) (originalSize / maxImageSize.toDouble()) else 1.0

                val bitmapOptions = BitmapFactory.Options()
                bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio)
                bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
                input = if(!uri.scheme.equals("https")) EditBookActivity.instance.contentResolver.openInputStream(uri) else URL(uri.toString()).openStream()

                val bitmap: Bitmap? = BitmapFactory.decodeStream(input, null, bitmapOptions)

                input?.close()

                return bitmap
            }
            catch (e: Exception) {
                Log.e("InsertImage", e.message.toString())
                return null
            }
        }

        private fun getPowerOfTwoForSampleRatio(ratio: Double): Int {
            val k = Integer.highestOneBit(floor(ratio).toInt())
            return if(k==0) 1
            else k
        }

        const val TOKEN = "@$"
    }
    override fun onDraw(canvas: Canvas) {

        val newLineHeight = lineHeight
        var count = height / newLineHeight

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> mPaint.color = Color.WHITE
            Configuration.UI_MODE_NIGHT_NO -> mPaint.color = Color.BLACK
        }

        if(lineCount > count)
        {
            count = lineCount
        }

        val r: Rect = mRect
        var baseline = getLineBounds(0, r)
        for(i in 0 until count)
        {
            canvas?.drawLine(r.left.toFloat(), (baseline + 1).toFloat(),
                r.right.toFloat(), (baseline + 1).toFloat(), mPaint)
            baseline += newLineHeight
        }

        super.onDraw(canvas)
    }

    fun insertImageToCursor(bitmap: Bitmap?, position: Int?, uri: Uri, shouldInsertText: Boolean = true)
    {
        if(bitmap == null) return
        val drawable = BitmapDrawable(resources, bitmap)

        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val stringToBeInserted = "$TOKEN$uri "

        val selectionCursor: Int = position ?: selectionStart

        if(shouldInsertText)
            text?.insert(selectionCursor, "$stringToBeInserted ")

        val builder = SpannableStringBuilder(text)
        builder.setSpan(CenteredImageSpan(drawable, stringToBeInserted), selectionCursor,
            selectionCursor + stringToBeInserted.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text = builder
        setSelection(selectionCursor + stringToBeInserted.length)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        if(ic == null) return ic

        EditorInfoCompat.setContentMimeTypes(outAttrs, arrayOf("image/*"))
        ViewCompat.setOnReceiveContentListener(this, arrayOf("image/*")) { _: View, inputContentInfo: ContentInfoCompat ->
            Log.d("edittext", inputContentInfo.linkUri.toString())
            val lacksPermission = (inputContentInfo.flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission)
            {
                try {
                    EditBookActivity.instance.requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 10)
//                    inputContentInfo.requestPermission()
                } catch (e: Exception)
                {
                    return@setOnReceiveContentListener inputContentInfo
                }
            }

            runBlocking {
                launch(Dispatchers.IO) {
                    val bitmap = inputContentInfo.linkUri?.let { getBitmap(it) }

                    val handler = Handler(Looper.getMainLooper())
                    handler.post {
                        inputContentInfo.linkUri?.let { insertImageToCursor(bitmap, null, it) }
                    }
                }
            }
            if(inputContentInfo.linkUri == null) {
                return@setOnReceiveContentListener inputContentInfo
            }

            return@setOnReceiveContentListener null
        }
//        val callback =
//            InputConnectionCompat.OnCommitContentListener { inputContentInfo, flags, _ ->
//                val lacksPermission = (flags and InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION) != 0
//                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && lacksPermission)
//                {
//                    try {
//                        inputContentInfo.requestPermission()
//                    } catch (e: Exception)
//                    {
//                        return@OnCommitContentListener false
//                    }
//                }
//
//                runBlocking {
//                    launch(Dispatchers.IO) {
//                        val bitmap = getBitmap(inputContentInfo.contentUri)
//
//                        val handler = Handler(Looper.getMainLooper())
//                        handler.post {
//                            insertImageToCursor(bitmap, null, inputContentInfo.contentUri)
//                        }
//                    }
//                }
//
//                true
//            }
//        return InputConnectionCompat.createWrapper(ic, outAttrs, callback)
        return InputConnectionCompat.createWrapper(this, ic, outAttrs)
    }
}