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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

// Saver for the list of DrawPaths, wrapped in a MutableState
val DrawPathStateSaver = Saver<MutableState<List<DrawPath>>, List<Any>>(
    save = { state ->
        state.value.map { path ->
            listOf(
                path.points.flatMap { listOf(it.x, it.y) },
                path.color.value,
                path.strokeWidth
            )
        }
    },
    restore = { saved ->
        val list = saved.map { pathData ->
            val pointsData = pathData as List<Any>
            val coordinates = pointsData[0] as List<Float>
            val points = coordinates.chunked(2).map { Offset(it[0], it[1]) }
            val color = Color(pointsData[1] as ULong)
            val strokeWidth = pointsData[2] as Float
            DrawPath(points, color, strokeWidth)
        }
        mutableStateOf(list)
    }
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
    
    
    var paths by rememberSaveable(saver = DrawPathStateSaver) { mutableStateOf(listOf<DrawPath>()) }
    var currentPath by remember { mutableStateOf(listOf<Offset>()) }
    var isDrawing by remember { mutableStateOf(false) }
    var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    // Calculate the actual image bounds within the canvas to correctly normalize/denormalize coordinates
    // Moved to top-level private function


    
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
                .pointerInput(canvasSize, backgroundBitmap) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val imageBounds = calculateImageBounds(canvasSize, backgroundBitmap)
                            if (imageBounds.contains(offset)) {
                                // Normalize coordinate: (0,0) is top-left of IMAGE, (1,1) is bottom-right of IMAGE
                                val normX = (offset.x - imageBounds.left) / imageBounds.width
                                val normY = (offset.y - imageBounds.top) / imageBounds.height
                                currentPath = listOf(Offset(normX, normY))
                                isDrawing = true
                            }
                        },
                        onDrag = { change, _ ->
                            if (isDrawing) {
                                val imageBounds = calculateImageBounds(canvasSize, backgroundBitmap)
                                val offset = change.position
                                // Clamp drawing to image bounds? Or just allow outside?
                                // Let's normalize it anyway, it might be < 0 or > 1 if dragging outside
                                val normX = (offset.x - imageBounds.left) / imageBounds.width
                                val normY = (offset.y - imageBounds.top) / imageBounds.height
                                currentPath = currentPath + Offset(normX, normY)
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
            // Update canvas size
            canvasSize = size
            val imageBounds = calculateImageBounds(size, backgroundBitmap)
            
            // Helper to convert normalized point back to screen point
            fun denormalize(normPoint: Offset): Offset {
                return Offset(
                    x = normPoint.x * imageBounds.width + imageBounds.left,
                    y = normPoint.y * imageBounds.height + imageBounds.top
                )
            }

            // Draw completed paths
            paths.forEach { drawPath ->
                if (drawPath.points.size > 1) {
                    for (i in 0 until drawPath.points.size - 1) {
                        drawLine(
                            color = drawPath.color,
                            start = denormalize(drawPath.points[i]),
                            end = denormalize(drawPath.points[i + 1]),
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
                        start = denormalize(currentPath[i]),
                        end = denormalize(currentPath[i + 1]),
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
                    canvasSize = canvasSize
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
    canvasSize: androidx.compose.ui.geometry.Size // kept signature but unused now
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
    
    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        // Start strokeWidth will be overwritten
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    
    val imageBounds = calculateImageBounds(canvasSize, backgroundBitmap)
    // Avoid division by zero
    val scale = if (imageBounds.width > 0) width.toFloat() / imageBounds.width else 1f
    
    // Draw paths on canvas using normalized coordinates
    paths.forEach { drawPath ->
        paint.strokeWidth = drawPath.strokeWidth * scale
        
        if (drawPath.points.size > 1) {
            for (i in 0 until drawPath.points.size - 1) {
                // Denormalize to bitmap coordinates
                val startX = drawPath.points[i].x * width
                val startY = drawPath.points[i].y * height
                val endX = drawPath.points[i+1].x * width
                val endY = drawPath.points[i+1].y * height

                canvas.drawLine(
                    startX,
                    startY,
                    endX,
                    endY,
                    paint
                )
            }
        }
    }
    
    return compositeBitmap
}

// Calculate the actual image bounds within the canvas to correctly normalize/denormalize coordinates
private fun calculateImageBounds(viewSize: androidx.compose.ui.geometry.Size, bitmap: Bitmap?): androidx.compose.ui.geometry.Rect {
    if (bitmap == null || viewSize.width == 0f || viewSize.height == 0f) {
        return androidx.compose.ui.geometry.Rect.Zero
    }

    val viewAspectRatio = viewSize.width / viewSize.height
    val bitmapAspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

    val imageWidth: Float
    val imageHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (viewAspectRatio > bitmapAspectRatio) {
        // View is wider than image (fit by height)
        imageHeight = viewSize.height
        imageWidth = imageHeight * bitmapAspectRatio
        offsetY = 0f
        offsetX = (viewSize.width - imageWidth) / 2f
    } else {
        // View is taller than image (fit by width)
        imageWidth = viewSize.width
        imageHeight = imageWidth / bitmapAspectRatio
        offsetX = 0f
        offsetY = (viewSize.height - imageHeight) / 2f
    }

    return androidx.compose.ui.geometry.Rect(offsetX, offsetY, offsetX + imageWidth, offsetY + imageHeight)
}