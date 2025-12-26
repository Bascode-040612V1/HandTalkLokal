package com.example.handtalklokal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Paint
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object ImageUtils {
    /**
     * Convert ImageProxy to Bitmap
     * Handles YUV_420_888 format which is commonly used by CameraX
     */
    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer // Y
        val uBuffer = image.planes[1].buffer // U
        val vBuffer = image.planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * Convert YUV_420_888 ImageProxy to ARGB_8888 Bitmap directly
     * More efficient than compressing to JPEG
     */
    fun yuvToRgb(image: ImageProxy): Bitmap {
        val width = image.width
        val height = image.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Get the three planes
        val planes = image.planes
        val buffer0 = planes[0].buffer // Y
        val buffer1 = planes[1].buffer // U
        val buffer2 = planes[2].buffer // V

        // Save the current positions
        val pos0 = buffer0.position()
        val pos1 = buffer1.position()
        val pos2 = buffer2.position()

        // Read the data safely
        val yPixel = ByteArray(buffer0.remaining())
        val uPixel = ByteArray(buffer1.remaining())
        val vPixel = ByteArray(buffer2.remaining())

        buffer0.get(yPixel)
        buffer1.get(uPixel)
        buffer2.get(vPixel)

        // Restore the positions
        buffer0.position(pos0)
        buffer1.position(pos1)
        buffer2.position(pos2)

        // Convert YUV to RGB
        val argbPixels = IntArray(width * height)
        convertYUVToARGB(yPixel, uPixel, vPixel, argbPixels, width, height)

        bitmap.setPixels(argbPixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun convertYUVToARGB(
        yData: ByteArray,
        uData: ByteArray,
        vData: ByteArray,
        argbPixels: IntArray,
        width: Int,
        height: Int
    ) {
        // YUV 420sp to RGB
        for (j in 0 until height) {
            for (i in 0 until width) {
                val yIndex = j * width + i
                
                // For YUV_420_888, UV values are subsampled (one UV pair for every 2x2 block)
                // Calculate the corresponding UV indices
                val uvRow = j / 2
                val uvCol = i / 2
                val uvWidth = (width + 1) / 2  // Round up to handle odd widths
                val uvIndex = uvRow * uvWidth + uvCol
                
                // Make sure we don't go out of bounds
                val yValue = yData[yIndex].toInt() and 0xff
                val uValue = uData[uvIndex].toInt() and 0xff
                val vValue = vData[uvIndex].toInt() and 0xff

                val r = yValue + (1.370705 * (vValue - 128)).toInt()
                val g = yValue - (0.698001 * (vValue - 128)).toInt() - (0.337633 * (uValue - 128)).toInt()
                val b = yValue + (1.732446 * (uValue - 128)).toInt()

                val rgb =
                    -0x1000000 + (clamp(r) shl 16) + (clamp(g) shl 8) + clamp(b)
                argbPixels[yIndex] = rgb
            }
        }
    }

    private fun clamp(value: Int): Int {
        return when {
            value < 0 -> 0
            value > 255 -> 255
            else -> value
        }
    }
}