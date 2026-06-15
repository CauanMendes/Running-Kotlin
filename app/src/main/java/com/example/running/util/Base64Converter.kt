package com.example.running.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

class Base64Converter {

    companion object {

        fun drawableToString(drawable: Drawable): String {
            val pictureDrawable = drawable as BitmapDrawable
            val bitmap = pictureDrawable.bitmap.scale(150, 150)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val imageString = Base64.encodeToString(outputStream.toByteArray(), 0)
            return imageString
        }

        /** Lê uma imagem de uma Uri (photo picker, câmera) e retorna em Base64. */
        fun uriToString(context: Context, uri: Uri, maxSize: Int = 600): String {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return ""

            val scaled = scaleKeepingAspect(bitmap, maxSize)
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val imageString = Base64.encodeToString(outputStream.toByteArray(), 0)

            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            return imageString
        }

        fun stringToBitmap(imageString: String): Bitmap {
            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            return decodedImage
        }

        private fun scaleKeepingAspect(bitmap: Bitmap, max: Int): Bitmap {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= max && h <= max) return bitmap
            val scale = max.toFloat() / maxOf(w, h)
            return bitmap.scale((w * scale).toInt(), (h * scale).toInt())
        }
    }
}
