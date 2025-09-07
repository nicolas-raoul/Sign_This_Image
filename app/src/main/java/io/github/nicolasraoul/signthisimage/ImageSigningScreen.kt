package io.github.nicolasraoul.signthisimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSigningScreen(
    imageUri: Uri?,
    onSave: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    var paths by remember { mutableStateOf(listOf<DrawPath>()) }
    var currentPath by remember { mutableStateOf(listOf<Offset>()) }
    var isDrawing by remember { mutableStateOf(false) }
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Load background image
    LaunchedEffect(imageUri) {
        imageUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    backgroundBitmap = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Default drawing properties
    val strokeColor = Color.Black
    val strokeWidth = 5.dp.value * density.density
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background image or placeholder
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap!!.asImageBitmap(),
                contentDescription = "Background image to sign",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (imageUri != null) "Loading image..." else "No image selected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        }
        
        // Drawing canvas overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath = listOf(offset)
                            isDrawing = true
                        },
                        onDrag = { change, _ ->
                            if (isDrawing) {
                                currentPath = currentPath + change.position
                            }
                        },
                        onDragEnd = {
                            if (isDrawing && currentPath.isNotEmpty()) {
                                paths = paths + DrawPath(
                                    points = currentPath,
                                    color = strokeColor,
                                    strokeWidth = strokeWidth
                                )
                                currentPath = emptyList()
                                isDrawing = false
                            }
                        }
                    )
                }
        ) {
            // Draw completed paths
            paths.forEach { drawPath ->
                if (drawPath.points.size > 1) {
                    for (i in 0 until drawPath.points.size - 1) {
                        drawLine(
                            color = drawPath.color,
                            start = drawPath.points[i],
                            end = drawPath.points[i + 1],
                            strokeWidth = drawPath.strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            
            // Draw current path being drawn
            if (isDrawing && currentPath.size > 1) {
                for (i in 0 until currentPath.size - 1) {
                    drawLine(
                        color = strokeColor,
                        start = currentPath[i],
                        end = currentPath[i + 1],
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        
        // Clear button (optional - not in original spec but useful)
        if (paths.isNotEmpty()) {
            FloatingActionButton(
                onClick = { paths = emptyList() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Text("Clear", color = MaterialTheme.colorScheme.onSecondary)
            }
        }
        
        // Save Floating Action Button
        FloatingActionButton(
            onClick = {
                val bitmap = createCompositeBitmap(
                    backgroundBitmap = backgroundBitmap,
                    paths = paths,
                    canvasSize = size
                )
                onSave(bitmap)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save signed image"
            )
        }
    }
}

// Enhanced bitmap creation with proper background image composition
private fun createCompositeBitmap(
    backgroundBitmap: Bitmap?,
    paths: List<DrawPath>,
    canvasSize: androidx.compose.ui.geometry.Size
): Bitmap {
    val width = backgroundBitmap?.width ?: 800
    val height = backgroundBitmap?.height ?: 600
    
    val compositeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(compositeBitmap)
    
    // Draw background
    if (backgroundBitmap != null) {
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)
    } else {
        canvas.drawColor(android.graphics.Color.WHITE)
    }
    
    // Calculate scaling factors if canvas size is different from bitmap size
    val scaleX = width.toFloat() / canvasSize.width
    val scaleY = height.toFloat() / canvasSize.height
    
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    
    // Draw paths on canvas with proper scaling
    paths.forEach { drawPath ->
        if (drawPath.points.size > 1) {
            for (i in 0 until drawPath.points.size - 1) {
                canvas.drawLine(
                    drawPath.points[i].x * scaleX,
                    drawPath.points[i].y * scaleY,
                    drawPath.points[i + 1].x * scaleX,
                    drawPath.points[i + 1].y * scaleY,
                    paint
                )
            }
        }
    }
    
    return compositeBitmap
}