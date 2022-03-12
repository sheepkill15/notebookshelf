package com.sanddunes.notebookshelf

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.google.android.material.color.DynamicColors

class NotebookshelfApplication: MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}