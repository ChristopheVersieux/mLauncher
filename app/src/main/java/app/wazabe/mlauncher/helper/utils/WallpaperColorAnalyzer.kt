package app.wazabe.mlauncher.helper.utils

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.drawable.toBitmap

/**
 * Utility to analyze wallpaper brightness and determine optimal text color
 */
object WallpaperColorAnalyzer {

    /**
     * Returns true if the wallpaper is predominantly dark
     */
    fun isWallpaperDark(context: Context): Boolean {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val drawable = wallpaperManager.drawable ?: return true // Default to dark if no wallpaper
            
            val bitmap = drawable.toBitmap(100, 100) // Scale down for faster analysis
            val brightness = calculateAverageBrightness(bitmap)
            
            // If brightness < 128, wallpaper is dark; otherwise light
            brightness < 128
        } catch (e: Exception) {
            // Default to dark if we can't read wallpaper
            true
        }
    }

    /**
     * Returns the recommended text color (white for dark wallpaper, black for light)
     */
    fun getRecommendedTextColor(context: Context): Int {
        return if (isWallpaperDark(context)) {
            Color.WHITE
        } else {
            Color.BLACK
        }
    }

    private fun calculateAverageBrightness(bitmap: Bitmap): Double {
        var totalBrightness = 0.0
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 10 // Sample every 10th pixel for speed
        var pixelCount = 0

        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Luminance formula
                val brightness = 0.299 * r + 0.587 * g + 0.114 * b
                totalBrightness += brightness
                pixelCount++
            }
        }

        return if (pixelCount > 0) totalBrightness / pixelCount else 128.0
    }
}
