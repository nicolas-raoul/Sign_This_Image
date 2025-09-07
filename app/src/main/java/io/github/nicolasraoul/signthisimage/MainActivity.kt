package io.github.nicolasraoul.signthisimage

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.nicolasraoul.signthisimage.ui.theme.SignThisImageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check if we were opened via share intent
        val sharedImageUri = when {
            intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            else -> null
        }
        
        setContent {
            SignThisImageTheme {
                SignThisImageApp(initialImageUri = sharedImageUri)
            }
        }
    }
}

@Composable
fun SignThisImageApp(
    initialImageUri: Uri? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentImageUri by remember { mutableStateOf(initialImageUri) }
    var showSigningScreen by remember { mutableStateOf(initialImageUri != null) }
    
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (showSigningScreen && currentImageUri != null) {
            ImageSigningScreen(
                imageUri = currentImageUri,
                onSave = { bitmap ->
                    saveSignedImage(context, bitmap, currentImageUri)
                },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            ImagePickerScreen(
                onImageSelected = { uri ->
                    currentImageUri = uri
                    showSigningScreen = true
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

private fun saveSignedImage(
    context: android.content.Context,
    bitmap: Bitmap,
    originalUri: Uri?
) {
    val originalFileName = originalUri?.let { uri ->
        FileSaver.getFileNameFromUri(context, uri)
    }
    
    val savedFile = FileSaver.saveBitmap(
        context = context,
        bitmap = bitmap,
        originalUri = originalUri,
        originalFileName = originalFileName
    )
    
    if (savedFile != null) {
        Toast.makeText(
            context,
            "Signed image saved as: ${savedFile.name}",
            Toast.LENGTH_LONG
        ).show()
    } else {
        Toast.makeText(
            context,
            "Failed to save signed image",
            Toast.LENGTH_SHORT
        ).show()
    }
}