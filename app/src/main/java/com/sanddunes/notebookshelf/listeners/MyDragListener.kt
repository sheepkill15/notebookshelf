package com.sanddunes.notebookshelf.listeners

import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.sanddunes.notebookshelf.activities.MainActivity
import com.sanddunes.notebookshelf.views.BookView

class MyDragListener(private val container: LinearLayout) : View.OnDragListener {

    override fun onDrag(v: View?, event: DragEvent?): Boolean {

        when(event?.action ?: -1) {
            DragEvent.ACTION_DRAG_STARTED -> {
                event?.let {
                    val view = it.localState as View
                    view.visibility = View.GONE
                }
            }
            DragEvent.ACTION_DRAG_ENTERED -> {

            }
            DragEvent.ACTION_DRAG_EXITED -> {

            }
            DragEvent.ACTION_DROP -> {
                val view: BookView = event?.localState as BookView

                val owner: ViewGroup = view.parent as ViewGroup
                owner.removeView(view)

                container.addView(view)
                view.visibility = View.VISIBLE

                val oldIndex = owner.tag as Int
                val newIndex = container.tag as Int
                MainActivity.instance.moveBook(oldIndex, newIndex, view.index1)
                owner.tag = newIndex
                view.index1 = container.childCount - 1

            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val view: View = event?.localState as View
                view.visibility = View.VISIBLE
            }
        }
        return true
    }
}