package io.github.nicolasraoul.signthisimage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
            
            // For Android 10+ (API 29+), use MediaStore for better compatibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(context, bitmap, fileName)
            } else {
                saveToDCIM(bitmap, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun saveToMediaStore(context: Context, bitmap: Bitmap, fileName: String): File? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
        }
        
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        return uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            // Return a file reference for UI purposes
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera/$fileName")
        }
    }
    
    private fun saveToDCIM(bitmap: Bitmap, fileName: String): File {
        val dcimDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera"
        )
        
        // Create directory if it doesn't exist
        if (!dcimDir.exists()) {
            dcimDir.mkdirs()
        }
        
        val file = File(dcimDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
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
            // If we can't get the name from URI, try to extract from path
            uri.path?.substringAfterLast("/")
        }
    }
}