package com.hommlie.partner.ui.capturephoto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.hommlie.partner.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoWatermarkProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun process(
        photoFile: File,
        metadata: String
    ): Bitmap = withContext(Dispatchers.Default) {

        val bitmap = decodeBitmap(photoFile)
            ?: throw IOException("Unable to decode captured image")

        drawWatermark(
            bitmap = bitmap,
            metadata = metadata
        )

        bitmap
    }

    private fun decodeBitmap(
        photoFile: File
    ): Bitmap? {

        val decodedBitmap = BitmapFactory.decodeFile(
            photoFile.absolutePath
        ) ?: return null

        val exif = ExifInterface(photoFile.absolutePath)

        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val orientedBitmap = when (orientation) {

            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotateBitmap(
                    decodedBitmap,
                    90f
                )
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotateBitmap(
                    decodedBitmap,
                    180f
                )
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotateBitmap(
                    decodedBitmap,
                    270f
                )
            }

            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                flipBitmap(
                    decodedBitmap,
                    horizontal = true
                )
            }

            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                flipBitmap(
                    decodedBitmap,
                    horizontal = false
                )
            }

            else -> decodedBitmap
        }

        /*
         * IMPORTANT:
         *
         * BitmapFactory can return an immutable bitmap.
         * Canvas requires a mutable bitmap.
         */
        return if (orientedBitmap.isMutable) {

            orientedBitmap

        } else {

            orientedBitmap.copy(
                Bitmap.Config.ARGB_8888,
                true
            ).also {

                if (it !== orientedBitmap) {
                    orientedBitmap.recycle()
                }
            }
        }
    }

    private fun rotateBitmap(
        bitmap: Bitmap,
        degrees: Float
    ): Bitmap {

        val matrix = Matrix().apply {
            postRotate(degrees)
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        ).also {

            if (it !== bitmap) {
                bitmap.recycle()
            }
        }
    }

    private fun flipBitmap(
        bitmap: Bitmap,
        horizontal: Boolean
    ): Bitmap {

        val matrix = Matrix().apply {

            if (horizontal) {
                postScale(-1f, 1f)
            } else {
                postScale(1f, -1f)
            }
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        ).also {

            if (it !== bitmap) {
                bitmap.recycle()
            }
        }
    }

    private fun drawWatermark(
        bitmap: Bitmap,
        metadata: String
    ) {

        val canvas = Canvas(bitmap)

        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        /*
         * Scale everything according to actual
         * captured image resolution.
         *
         * 1080 = reference width.
         */
        val scale = width / 1080f

        val padding = 25f * scale

        /*
         * Logo size
         * Previous: 180
         * New:      220
         */
        val logoWidth = 280f * scale

        /*
         * Gap between logo and metadata text.
         * Approximately 2px at reference resolution.
         */
        val logoTextGap = 10f * scale

        /*
         * Metadata text size
         * Previous: 32
         * New:      36
         */
        val textSize = 32f * scale
        val textStartPadding = 60f * scale

        val lineSpacing = 12f * scale

        val backgroundPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            color = Color.argb(
                170,
                0,
                0,
                0
            )

            style = Paint.Style.FILL
        }

        val logo = loadLogo()

        val logoHeight =
            logoWidth *
                    logo.height.toFloat() /
                    logo.width.toFloat()

        val textPaint = Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {

            color = Color.WHITE

            this.textSize = textSize

            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.NORMAL
            )
        }

        val textLines = wrapText(
            metadata = metadata,
            paint = textPaint,
            maxWidth = width - textStartPadding - padding
        )

        /*
         * Calculate complete metadata area height.
         */
        val textHeight =
            if (textLines.isNotEmpty()) {

                (textLines.size * textSize) +
                        ((textLines.size - 1)
                            .coerceAtLeast(0) * lineSpacing)

            } else {
                0f
            }

        val metadataHeight =
            padding +
                    logoHeight +
                    logoTextGap +
                    textHeight +
                    padding

        val top = height - metadataHeight

        /*
         * Background.
         */
        canvas.drawRect(
            0f,
            top,
            width,
            height,
            backgroundPaint
        )

        /*
         * Logo.
         */
        val logoRect = RectF(
            padding,
            top + padding,
            padding + logoWidth,
            top + padding + logoHeight
        )

        canvas.drawBitmap(
            logo,
            null,
            logoRect,
            Paint(Paint.ANTI_ALIAS_FLAG)
        )

        /*
         * Metadata text.
         *
         * Only 2px scaled gap after logo.
         */
        var textY =
            logoRect.bottom +
                    logoTextGap +
                    textSize

        textLines.forEach { line ->

            canvas.drawText(
                line,
                textStartPadding,
                textY,
                textPaint
            )

            textY += textSize + lineSpacing
        }

        logo.recycle()
    }

    private fun loadLogo(): Bitmap {

        val drawable = ContextCompat.getDrawable(
            context, R.drawable.app_logo_white
        ) ?: throw IllegalStateException(
            "App logo drawable not found"
        )

        val width = drawable.intrinsicWidth
                .takeIf { it > 0 }
                ?: 500

        val height = drawable.intrinsicHeight
                .takeIf { it > 0 }
                ?: 200

        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        drawable.setBounds(
            0,
            0,
            canvas.width,
            canvas.height
        )

        drawable.draw(canvas)

        return bitmap
    }

    private fun wrapText(
        metadata: String,
        paint: Paint,
        maxWidth: Float
    ): List<String> {

        if (metadata.isBlank()) {
            return emptyList()
        }

        val result = mutableListOf<String>()

        metadata
            .split("\n")
            .forEach { paragraph ->

                if (paragraph.isBlank()) {
                    return@forEach
                }

                var currentLine = ""

                paragraph
                    .split(" ")
                    .forEach { word ->

                        val testLine =
                            if (currentLine.isEmpty()) {
                                word
                            } else {
                                "$currentLine $word"
                            }

                        if (
                            paint.measureText(
                                testLine
                            ) <= maxWidth
                        ) {

                            currentLine = testLine

                        } else {

                            if (currentLine.isNotEmpty()) {
                                result.add(currentLine)
                            }

                            currentLine = word
                        }
                    }

                if (currentLine.isNotEmpty()) {
                    result.add(currentLine)
                }
            }

        return result
    }
}