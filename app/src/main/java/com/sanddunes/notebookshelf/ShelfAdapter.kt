package com.sanddunes.notebookshelf

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.sanddunes.notebookshelf.listeners.MyDragListener
import com.sanddunes.notebookshelf.views.BookView

class ShelfAdapter(private val context: Context, private val shelfLayout: ArrayList<ArrayList<Int>>) : RecyclerView.Adapter<ShelfAdapter.BookshelfViewHolder>() {

    private companion object {
        const val PreProcessCount = 2
    }

    inner class BookshelfViewHolder(scrollView: HorizontalScrollView) : RecyclerView.ViewHolder(scrollView) {

        private val shelf = scrollView.getChildAt(0) as LinearLayout

        fun bind(position: Int)
        {
            val targetCount = shelfLayout[position].size
            val count = shelf.childCount
            when {
                count < targetCount -> {
                    for(i in count until targetCount)
                    {
                        createImageView(shelf)
                    }
                }
                count > targetCount -> {
                    for(i in targetCount until count)
                    {
                        if(shelf.childCount > 0)
                            shelf.removeViewAt(0)
                    }
                }
            }

            shelf.tag = position
            for(i in 0 until shelf.childCount)
            {
                val view: BookView = shelf.getChildAt(i) as BookView
                view.index1 = i
                view.setBook(shelfLayout[position][i])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookshelfViewHolder {
        val scrollView = LayoutInflater.from(context).inflate(R.layout.shelf, parent, false) as HorizontalScrollView
        val layout = scrollView.getChildAt(0) as LinearLayout
        scrollView.setOnDragListener(MyDragListener(layout))
        for(i in 0..PreProcessCount) {
            createImageView(layout)
        }

        return BookshelfViewHolder(scrollView)
    }

    override fun getItemCount(): Int {
        return shelfLayout.size
    }

    override fun onBindViewHolder(holder: BookshelfViewHolder, position: Int) {
        holder.bind(position)
    }

    private fun createImageView(layout: LinearLayout)
    {
        LayoutInflater.from(context).inflate(R.layout.shelf_item, layout, true)

      //  layout.addView(defaultImage)
    }
}