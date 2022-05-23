package com.quizaruim.chat.components.webview

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ImageLoader {
    @Throws(IOException::class)
    fun createImageFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(
            Environment.DIRECTORY_PICTURES
        )
        var imageFile: File? = null
        try {
            imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return imageFile
    }
}