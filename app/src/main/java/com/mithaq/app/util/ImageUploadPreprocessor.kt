package com.mithaq.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

suspend fun prepareForUpload(
    context: Context,
    uri: Uri,
    maxLongEdgePx: Int = 1280,
    jpegQuality: Int = 80
): ByteArray = withContext(Dispatchers.IO) {
    require(maxLongEdgePx > 0) { "maxLongEdgePx must be positive." }
    require(jpegQuality in 0..100) { "jpegQuality must be between 0 and 100." }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    } ?: throw IOException("Unable to open image.")

    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("Unable to decode image bounds.")
    }

    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxLongEdgePx)
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    var working = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOptions)
    } ?: throw IOException("Unable to decode image.")

    try {
        val oriented = applyExifOrientation(working, orientation)
        if (oriented !== working) {
            working.recycle()
            working = oriented
        }

        val scaled = scaleToLongEdge(working, maxLongEdgePx)
        if (scaled !== working) {
            working.recycle()
            working = scaled
        }

        ByteArrayOutputStream().use { output ->
            if (!working.compress(Bitmap.CompressFormat.JPEG, jpegQuality, output)) {
                throw IOException("Unable to encode image.")
            }
            output.toByteArray()
        }
    } finally {
        if (!working.isRecycled) {
            working.recycle()
        }
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxLongEdgePx: Int): Int {
    val longEdge = max(width, height)
    var sampleSize = 1
    while (longEdge / (sampleSize * 2) >= maxLongEdgePx) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun scaleToLongEdge(bitmap: Bitmap, maxLongEdgePx: Int): Bitmap {
    val longEdge = max(bitmap.width, bitmap.height)
    if (longEdge <= maxLongEdgePx) return bitmap

    val scale = maxLongEdgePx.toFloat() / longEdge.toFloat()
    val targetWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
