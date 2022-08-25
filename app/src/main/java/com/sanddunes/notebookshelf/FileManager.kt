package com.sanddunes.notebookshelf

import java.io.*

class FileManager {
    companion object {

        private fun createFileIfNotExist(file: File) {
            if(file.isDirectory)
            {
                throw NoSuchFileException(file)
            }
            if (!file.exists()) {
                file.createNewFile()
            }
        }

        fun readFromFile(path: String): ArrayList<ArrayList<Int>> {

            val file = File(path)
            createFileIfNotExist(file)
            val fin = FileInputStream(file)


            var readData: ArrayList<ArrayList<Int>>
            try {
                val ois = ObjectInputStream(fin)
                @Suppress("UNCHECKED_CAST")
                readData = ois.readObject() as ArrayList<ArrayList<Int>>
                ois.close()

            } catch (e: Exception) {
                readData = arrayListOf(arrayListOf())
            }
            fin.close()

            return readData
        }

        fun saveToFile(path: String,
                       data: ArrayList<ArrayList<Int>>) {
            val file = File(path)
            createFileIfNotExist(file)
            val fos = FileOutputStream(file)
            val oos = ObjectOutputStream(fos)
            oos.writeObject(data)
            oos.close()
            fos.close()
        }
    }
}