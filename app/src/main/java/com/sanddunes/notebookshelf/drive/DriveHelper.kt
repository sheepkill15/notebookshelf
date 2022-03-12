package com.sanddunes.notebookshelf.drive

import android.content.Intent
import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.sanddunes.notebookshelf.activities.MainActivity
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.util.*

object DriveHelper {
    lateinit var service: Drive

    private fun retrieveSaveFiles(): Pair<File, File> {
        val files = service.files().list()
            .setSpaces("appDataFolder")
            .setFields("nextPageToken, files(id, name)")
            .setPageSize(10)
            .execute()

        var listFile = File()
        listFile.name = "Empty"
        var dbFile = File()
        dbFile.name = "Empty"

        for(file in files.files) {
            Log.d("DRIVE", file.name)
            val name = file.name
            if(name == "list_file") {
                listFile = file
            }
            else if(name == "db_file") {
                dbFile = file
            }
        }
        return Pair(listFile, dbFile)
    }

    fun getSaveFiles() {
        val (listFile, dbFile) = retrieveSaveFiles()
        if(listFile.name != "Empty") {
            val oldListFile = java.io.File(MainActivity.filePath)
            oldListFile.delete()
            val outputStream = ByteArrayOutputStream()
            service.files().get(listFile.id)
                .executeAndDownloadTo(outputStream)
            outputStream.writeTo(FileOutputStream(MainActivity.filePath))
            Log.d("SAVE", MainActivity.filePath)
        }
        if(dbFile.name != "Empty") {
            val oldDbFile = MainActivity.instance.getDatabasePath("book_database")
            oldDbFile.delete()
            val dbOutputStream = ByteArrayOutputStream()
            service.files().get(dbFile.id)
                .executeAndDownloadTo(dbOutputStream)
            dbOutputStream.writeTo(FileOutputStream(oldDbFile.absolutePath))
            Log.d("SAVE", oldDbFile.absolutePath)
        }
        val pm = MainActivity.instance.packageManager
        val intent = pm.getLaunchIntentForPackage(MainActivity.instance.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        MainActivity.instance.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }

    fun setSaveFiles() {
        val (uploadedListFile, uploadedDbFile) = retrieveSaveFiles()

        val fileMetadata = File()
        fileMetadata.apply {
            name = "list_file"
            parents = Collections.singletonList("appDataFolder")
        }
        val mediaContent = FileContent("application/octet-stream", java.io.File(MainActivity.filePath))
        if(uploadedListFile.name != "Empty") {
            val id = uploadedListFile.id
            val newFile = File()
            newFile.name = "list_file"
            val file = service.files().update(id, newFile, mediaContent)
                .execute()
        } else {
            val file = service.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
        }
        val dbFile = MainActivity.instance.getDatabasePath("book_database")
        val dbFileMetadata = File()
        dbFileMetadata.apply {
            name = "db_file"
            parents = Collections.singletonList("appDataFolder")
        }
        val dbMediaContent = FileContent("application/octet-stream", dbFile)
        if(uploadedDbFile.name != "Empty") {
            val id = uploadedDbFile.id
            val newFile = File()
            newFile.name = "db_file"
            val file = service.files().update(id, newFile, dbMediaContent).execute()
        }
        else {
            val dbFileDrive = service.files().create(dbFileMetadata, dbMediaContent)
                .setFields("id")
                .execute()
        }
    }
}