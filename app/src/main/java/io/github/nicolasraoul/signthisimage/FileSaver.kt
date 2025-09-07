package io.github.nicolasraoul.signthisimage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object FileSaver {
    
    /**
     * Save the signed bitmap with proper filename convention
     * Following README spec: [original_filename]_signed.png
     */
    fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        originalUri: Uri? = null,
        originalFileName: String? = null
    ): File? {
        return try {
            val fileName = generateFileName(originalFileName)
            val file = getOutputFile(fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    private fun generateFileName(originalFileName: String?): String {
        return if (originalFileName != null && originalFileName.isNotEmpty()) {
            // Remove extension from original filename if present
            val nameWithoutExtension = originalFileName.substringBeforeLast(".")
            "${nameWithoutExtension}_signed.png"
        } else {
            // Generate default filename with timestamp
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                .format(Date())
            "signed_image_${timestamp}.png"
        }
    }
    
    private fun getOutputFile(fileName: String): File {
        // Try to save to DCIM/Camera folder as per README spec
        val dcimDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera"
        )
        
        // Create directory if it doesn't exist
        if (!dcimDir.exists()) {
            dcimDir.mkdirs()
        }
        
        return File(dcimDir, fileName)
    }
    
    /**
     * Extract filename from URI if possible
     */
    fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}