package com.example.ui.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

object AppIconCache {
    // Cache up to 300 bitmaps in memory for instant 60/120fps scrolling
    private val memoryCache = LruCache<String, Bitmap>(300)

    fun get(packageName: String): Bitmap? {
        return memoryCache.get(packageName)
    }

    fun put(packageName: String, bitmap: Bitmap) {
        memoryCache.put(packageName, bitmap)
    }

    fun load(context: Context, packageName: String): Bitmap? {
        memoryCache.get(packageName)?.let { return it }
        return try {
            val pm = context.packageManager
            val drawable: Drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            if (bitmap != null) {
                memoryCache.put(packageName, bitmap)
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val b = drawable.bitmap
            if (b.width <= 192 && b.height <= 192) return b
        }
        val rawWidth = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
        val rawHeight = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
        val width = minOf(rawWidth, 192)
        val height = minOf(rawHeight, 192)
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
