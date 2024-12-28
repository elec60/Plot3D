package com.example.plot3d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.sin

private data class CanvasProperties(
    val width: Float,
    val height: Float,
    val scale: Float,
    val center: Offset = Offset(width / 2f, height / 2f)
)

@Composable
fun Plot3DScreen() {
    var rotationX by remember { mutableFloatStateOf(30f) }
    var rotationY by remember { mutableFloatStateOf(-45f) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Plot3D(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        rotationX = (rotationX - pan.y / 3f).coerceIn(-90f, 90f)
                        // Normalize rotationY to always be between 0 and 360
                        rotationY = ((rotationY + pan.x / 3f) % 360f + 360f) % 360f
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                    }
                },
            rotationX = rotationX,
            rotationY = rotationY,
            scale = scale
        )
    }
}



@Composable
fun Plot3D(
    modifier: Modifier = Modifier,
    rotationX: Float = 30f,
    rotationY: Float = -45f,
    scale: Float = 1f
) {
    Canvas(modifier = modifier) {
        val canvasProperties = CanvasProperties(
            width = size.width,
            height = size.height,
            scale = minOf(size.width, size.height) / 5f * scale
        )

        drawGridLines(canvasProperties, rotationX, rotationY)
        drawAxes(canvasProperties, rotationX, rotationY)
    }
}

private fun DrawScope.drawGridLines(properties: CanvasProperties, rotationX: Float, rotationY: Float) {
    val rangeStart = -2f
    val rangeEnd = 2f
    val steps = 40
    val stepSize = (rangeEnd - rangeStart) / steps

    val gridLines = mutableListOf<Pair<Triple<Float, Float, Float>, Triple<Float, Float, Float>>>()

    // X-direction lines
    var currentX = rangeStart
    while (currentX <= rangeEnd) {
        var currentY = rangeStart
        while (currentY < rangeEnd) {
            val z1 = currentX * currentX + currentY * currentY
            val z2 = currentX * currentX + (currentY + stepSize) * (currentY + stepSize)
            gridLines.add(
                Pair(
                    Triple(currentX, currentY, z1),
                    Triple(currentX, currentY + stepSize, z2)
                )
            )
            currentY += stepSize
        }
        currentX += stepSize
    }

    // Y-direction lines
    var currentY = rangeStart
    while (currentY <= rangeEnd) {
        currentX = rangeStart
        while (currentX < rangeEnd) {
            val z1 = currentX * currentX + currentY * currentY
            val z2 = (currentX + stepSize) * (currentX + stepSize) + currentY * currentY
            gridLines.add(
                Pair(
                    Triple(currentX, currentY, z1),
                    Triple(currentX + stepSize, currentY, z2)
                )
            )
            currentX += stepSize
        }
        currentY += stepSize
    }

    gridLines.forEach { (start, end) ->
        val rotStart = rotateY(start.first, start.second, start.third, rotationY)
        val rotStartX = rotateX(rotStart.first, rotStart.second, rotStart.third, rotationX)
        val rotEnd = rotateY(end.first, end.second, end.third, rotationY)
        val rotEndX = rotateX(rotEnd.first, rotEnd.second, rotEnd.third, rotationX)

        val startPoint = Offset(
            properties.center.x + rotStartX.first * properties.scale,
            properties.center.y + rotStartX.second * properties.scale
        )
        val endPoint = Offset(
            properties.center.x + rotEndX.first * properties.scale,
            properties.center.y + rotEndX.second * properties.scale
        )

        val zValue = (rotStartX.third + rotEndX.third) / 2f

        drawLine(
            color = getColorForZ(zValue),
            start = startPoint,
            end = endPoint,
            strokeWidth = 1f
        )
    }

}

private fun DrawScope.drawAxes(properties: CanvasProperties, rotationX: Float, rotationY: Float) {
    val axisLength = properties.scale * 2f

    val xAxis = Triple(axisLength, 0f, 0f)
    val yAxis = Triple(0f, axisLength, 0f)
    val zAxis = Triple(0f, 0f, axisLength)

    val xAxisRotY = rotateY(xAxis.first, xAxis.second, xAxis.third, rotationY)
    val yAxisRotY = rotateY(yAxis.first, yAxis.second, yAxis.third, rotationY)
    val zAxisRotY = rotateY(zAxis.first, zAxis.second, zAxis.third, rotationY)

    val xAxisFinal = rotateX(xAxisRotY.first, xAxisRotY.second, xAxisRotY.third, rotationX)
    val yAxisFinal = rotateX(yAxisRotY.first, yAxisRotY.second, yAxisRotY.third, rotationX)
    val zAxisFinal = rotateX(zAxisRotY.first, zAxisRotY.second, zAxisRotY.third, rotationX)

    // Draw X axis
    drawLine(
        color = Color.Red,
        start = properties.center,
        end = Offset(
            properties.center.x + xAxisFinal.first * properties.scale,
            properties.center.y + xAxisFinal.second * properties.scale
        ),
        strokeWidth = 2f
    )

    // Draw Y axis
    drawLine(
        color = Color.Green,
        start = properties.center,
        end = Offset(
            properties.center.x + yAxisFinal.first * properties.scale,
            properties.center.y + yAxisFinal.second * properties.scale
        ),
        strokeWidth = 2f
    )

    // Draw Z axis
    drawLine(
        color = Color.Blue,
        start = properties.center,
        end = Offset(
            properties.center.x + zAxisFinal.first * properties.scale,
            properties.center.y + zAxisFinal.second * properties.scale
        ),
        strokeWidth = 2f
    )
}

private fun getColorForZ(z: Float): Color {
    val alpha = (0.3f + (z / 8f)).coerceIn(0f, 1f)
    return Color(
        red = 0f,
        green = 0f,
        blue = 1f,
        alpha = alpha
    ).copy(alpha = alpha)
}

private fun rotateX(x: Float, y: Float, z: Float, angle: Float): Triple<Float, Float, Float> {
    val rad = Math.toRadians(angle.toDouble())
    val cos = cos(rad).toFloat()
    val sin = sin(rad).toFloat()
    return Triple(
        x,
        y * cos - z * sin,
        y * sin + z * cos
    )
}

private fun rotateY(x: Float, y: Float, z: Float, angle: Float): Triple<Float, Float, Float> {
    val rad = Math.toRadians(angle.toDouble())
    val cos = cos(rad).toFloat()
    val sin = sin(rad).toFloat()
    return Triple(
        x * cos + z * sin,
        y,
        -x * sin + z * cos
    )
}

