package com.example.engine

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Lightweight, 60fps-friendly downscaled box blur for pre-computing frosted backdrops
 * once per wallpaper or album-art update on a background thread.
 */
object BitmapBlurUtils {

    fun createFrostedBitmap(source: Bitmap, targetWidth: Int = 96, radius: Int = 6): Bitmap {
        val ratio = (source.height.toFloat() / source.width.coerceAtLeast(1)).coerceIn(0.25f, 4f)
        val w = targetWidth.coerceAtLeast(16)
        val h = (w * ratio).toInt().coerceAtLeast(16)
        val small = Bitmap.createScaledBitmap(source, w, h, true)
            .let { if (it.config == Bitmap.Config.ARGB_8888) it else it.copy(Bitmap.Config.ARGB_8888, false) }

        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)
        val r = radius.coerceIn(1, 14)

        // Two fast separable box passes approximate Gaussian blur at negligible CPU cost
        repeat(2) {
            boxBlurHorizontal(pixels, temp, w, h, r)
            boxBlurVertical(temp, pixels, w, h, r)
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (small != source) small.recycle()
        return out
    }

    private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        val window = radius * 2 + 1
        for (y in 0 until h) {
            val row = y * w
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0

            for (i in -radius..radius) {
                val p = src[row + i.coerceIn(0, w - 1)]
                aSum += Color.alpha(p)
                rSum += Color.red(p)
                gSum += Color.green(p)
                bSum += Color.blue(p)
            }

            for (x in 0 until w) {
                dst[row + x] = Color.argb(aSum / window, rSum / window, gSum / window, bSum / window)
                val outX = (x - radius).coerceIn(0, w - 1)
                val inX = (x + radius + 1).coerceIn(0, w - 1)
                val pOut = src[row + outX]
                val pIn = src[row + inX]
                aSum += Color.alpha(pIn) - Color.alpha(pOut)
                rSum += Color.red(pIn) - Color.red(pOut)
                gSum += Color.green(pIn) - Color.green(pOut)
                bSum += Color.blue(pIn) - Color.blue(pOut)
            }
        }
    }

    private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        val window = radius * 2 + 1
        for (x in 0 until w) {
            var rSum = 0
            var gSum = 0
            var bSum = 0
            var aSum = 0

            for (i in -radius..radius) {
                val p = src[i.coerceIn(0, h - 1) * w + x]
                aSum += Color.alpha(p)
                rSum += Color.red(p)
                gSum += Color.green(p)
                bSum += Color.blue(p)
            }

            for (y in 0 until h) {
                dst[y * w + x] = Color.argb(aSum / window, rSum / window, gSum / window, bSum / window)
                val outY = (y - radius).coerceIn(0, h - 1)
                val inY = (y + radius + 1).coerceIn(0, h - 1)
                val pOut = src[outY * w + x]
                val pIn = src[inY * w + x]
                aSum += Color.alpha(pIn) - Color.alpha(pOut)
                rSum += Color.red(pIn) - Color.red(pOut)
                gSum += Color.green(pIn) - Color.green(pOut)
                bSum += Color.blue(pIn) - Color.blue(pOut)
            }
        }
    }
}
