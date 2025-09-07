package io.github.nicolasraoul.signthisimage

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
    
    // Default drawing properties
    val strokeColor = Color.Black
    val strokeWidth = 5.dp.value * density.density
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background - for now just a light gray background
        // In a full implementation, this would display the actual image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        )
        
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
        
        // Floating Action Button for saving
        FloatingActionButton(
            onClick = {
                // Create composite bitmap and save
                val bitmap = createCompositeBitmap(size, paths)
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

// Simplified bitmap creation function
private fun createCompositeBitmap(
    size: androidx.compose.ui.geometry.Size,
    paths: List<DrawPath>
): Bitmap {
    val bitmap = Bitmap.createBitmap(
        800, // Default size for now
        600,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    
    // Fill with white background
    canvas.drawColor(android.graphics.Color.WHITE)
    
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    
    // Draw paths on canvas
    paths.forEach { drawPath ->
        if (drawPath.points.size > 1) {
            for (i in 0 until drawPath.points.size - 1) {
                canvas.drawLine(
                    drawPath.points[i].x,
                    drawPath.points[i].y,
                    drawPath.points[i + 1].x,
                    drawPath.points[i + 1].y,
                    paint
                )
            }
        }
    }
    
    return bitmap
}