package com.example.signthisimage

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

data class DrawingPath(val path: Path, val color: Color)

@Composable
fun DrawingCanvas(modifier: Modifier = Modifier, backgroundImage: Bitmap? = null) {
    val paths = remember { mutableStateListOf<DrawingPath>() }
    val imageBitmap = backgroundImage?.asImageBitmap()

    Canvas(modifier = modifier.pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()

            val path = Path().apply {
                moveTo(change.position.x - dragAmount.x, change.position.y - dragAmount.y)
                lineTo(change.position.x, change.position.y)
            }
            paths.add(DrawingPath(path, Color.Black))
        }
    }) {
        if (imageBitmap != null) {
            drawImage(imageBitmap)
        }
        paths.forEach { (path, color) ->
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 5f)
            )
        }
    }
}
