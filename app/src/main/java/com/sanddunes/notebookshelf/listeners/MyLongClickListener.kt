package com.sanddunes.notebookshelf.listeners

import android.content.ClipData
import android.os.Build
import android.view.View

class MyLongClickListener : View.OnLongClickListener {

    override fun onLongClick(v: View?): Boolean {
        val data: ClipData = ClipData.newPlainText("", "")
        val shadowBuilder: View.DragShadowBuilder = View.DragShadowBuilder(v)

     //   v?.visibility = View.GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            v?.startDragAndDrop(data, shadowBuilder, v, 0)
        }
        else {
            v?.startDrag(data, shadowBuilder, v, 0)
        }
        return true
    }
}