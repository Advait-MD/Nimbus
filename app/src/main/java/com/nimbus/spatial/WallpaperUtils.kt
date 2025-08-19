package com.nimbus.spatial // Change to match your package

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import java.io.IOException

object WallpaperUtils {

    // Change both wallpaper and lockscreen to the provided image resource
    fun setWallpaper(context: Context, imageResId: Int) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val bitmap = BitmapFactory.decodeResource(context.resources, imageResId)

        try {
            // Set wallpaper
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)

            // Set lockscreen wallpaper (only works on API 24+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Generate and set a pure black wallpaper + lockscreen
    fun setBlackWallpaper(context: Context) {
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val blackBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(blackBitmap)
        canvas.drawColor(Color.BLACK)

        val wallpaperManager = WallpaperManager.getInstance(context)

        try {
            wallpaperManager.setBitmap(blackBitmap, null, true, WallpaperManager.FLAG_SYSTEM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(blackBitmap, null, true, WallpaperManager.FLAG_LOCK)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
