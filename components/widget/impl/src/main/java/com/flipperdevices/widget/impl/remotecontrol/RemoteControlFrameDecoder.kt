package com.flipperdevices.widget.impl.remotecontrol

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.set
import com.flipperdevices.protobuf.screen.ScreenFrame
import com.flipperdevices.protobuf.screen.ScreenOrientation
import kotlin.experimental.and

/**
 * Decodes a raw [ScreenFrame] (Flipper's monochrome 128x64 framebuffer) into a Bitmap
 * for the widget's screen preview.
 *
 * This intentionally duplicates the small decode routine from
 * `screenstreaming.impl.viewmodel.repository.ScreenStreamFrameDecoder` instead of depending
 * on that feature's impl module, to keep the widget module independent from the in-app
 * remote-control screen feature.
 */
object RemoteControlFrameDecoder {
    private const val SCREEN_WIDTH = 128
    private const val SCREEN_HEIGHT = 64
    private const val BACKGROUND_COLOR = -0x73d7 // 0xFFFF8C29
    private const val SINGLE_BIT = 1
    private const val ZERO_BYTE = 0.toByte()
    private const val PIXEL_MASK = 7

    fun emptyBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(BACKGROUND_COLOR)
        return bitmap
    }

    fun decode(streamFrame: ScreenFrame): Bitmap? {
        val bytes = streamFrame.data_.toByteArray()
        if (bytes.isEmpty()) return null
        val flip = when (streamFrame.orientation) {
            ScreenOrientation.HORIZONTAL_FLIP,
            ScreenOrientation.VERTICAL_FLIP -> true

            else -> false
        }
        val bitmap = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888)
        for (x in 0 until SCREEN_WIDTH) {
            for (y in 0 until SCREEN_HEIGHT) {
                val color = if (bytes.isPixelSet(x, y)) Color.BLACK else BACKGROUND_COLOR
                val bitmapX = if (flip) SCREEN_WIDTH - x - 1 else x
                val bitmapY = if (flip) SCREEN_HEIGHT - y - 1 else y
                bitmap[bitmapX, bitmapY] = color
            }
        }
        return bitmap
    }

    private fun ByteArray.isPixelSet(x: Int, y: Int): Boolean {
        val index = (y / Byte.SIZE_BITS) * SCREEN_WIDTH + x
        val modifiedY = y and PIXEL_MASK
        return get(index) and (SINGLE_BIT.shl(modifiedY).toByte()) != ZERO_BYTE
    }
}
