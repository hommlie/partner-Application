package com.hommlie.partner.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.hommlie.partner.R
import android.graphics.Path
import android.graphics.Shader
import android.graphics.SweepGradient


class FourSideStrokeDrawable(context: Context) : Drawable() {

        private val cornerRadius = 10f.dpToPx(context)
        private val strokeWidth = 1.5f.dpToPx(context)

        private val white = ContextCompat.getColor(context, android.R.color.white)
        private val blue = ContextCompat.getColor(context, R.color.color_primary)
        private val purple = ContextCompat.getColor(context, R.color.purple)

        private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = white
        }

        private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = this@FourSideStrokeDrawable.strokeWidth
            strokeCap = Paint.Cap.BUTT
        }

        override fun draw(canvas: Canvas) {
            val outer = RectF(bounds)

            // 1. Draw white rounded background
            canvas.drawRoundRect(outer, cornerRadius, cornerRadius, fillPaint)

            // Inset by half stroke to center the stroke
            val inset = strokeWidth / 2.9
            val rect = RectF(
                (outer.left + inset).toFloat(),
                (outer.top + inset).toFloat(),
                (outer.right - inset).toFloat(),
                (outer.bottom - inset).toFloat()
            )

            // 2. Draw top (blue)
            strokePaint.color = blue
            canvas.drawLine(
                rect.left + cornerRadius, rect.top,
                rect.right - cornerRadius, rect.top,
                strokePaint
            )

            // 5. Draw right (purple)
            canvas.drawLine(
                rect.right, rect.top + cornerRadius,
                rect.right, rect.bottom - cornerRadius,
                strokePaint
            )


            // 4. Draw left (purple)
            canvas.drawLine(
                rect.left, rect.top + cornerRadius,
                rect.left, rect.bottom - cornerRadius,
                strokePaint
            )

            strokePaint.color = purple
            // 3. Draw bottom (purple)
            canvas.drawLine(
                rect.left + cornerRadius, rect.bottom,
                rect.right - cornerRadius, rect.bottom,
                strokePaint
            )



            // 6. Draw arcs for 4 corners
            drawCornerArcs(canvas, rect)
        }

        /*private fun drawCornerArcs(canvas: Canvas, rect: RectF) {
            val arcRect = RectF()
            strokePaint.style = Paint.Style.STROKE

            // Top-left corner (between top-blue and left-purple)
            arcRect.set(
                rect.left,
                rect.top,
                rect.left + 2 * cornerRadius,
                rect.top + 2 * cornerRadius
            )
            strokePaint.color = blue
            canvas.drawArc(arcRect, 180f, 90f, false, strokePaint)

            // Top-right corner
            arcRect.set(
                rect.right - 2 * cornerRadius,
                rect.top,
                rect.right,
                rect.top + 2 * cornerRadius
            )
            canvas.drawArc(arcRect, 270f, 90f, false, strokePaint)

            // Bottom-right corner
            arcRect.set(
                rect.right - 2 * cornerRadius,
                rect.bottom - 2 * cornerRadius,
                rect.right,
                rect.bottom
            )
            strokePaint.color = purple
            canvas.drawArc(arcRect, 0f, 90f, false, strokePaint)

            // Bottom-left corner
            arcRect.set(
                rect.left,
                rect.bottom - 2 * cornerRadius,
                rect.left + 2 * cornerRadius,
                rect.bottom
            )
            canvas.drawArc(arcRect, 90f, 90f, false, strokePaint)
        } */

    private fun drawCornerArcs(canvas: Canvas, rect: RectF) {
        val arcRect = RectF()

        fun makeGradientShader(cx: Float, cy: Float, startColor: Int, endColor: Int): Shader {
            return SweepGradient(cx, cy, intArrayOf(startColor, endColor), floatArrayOf(0f, 1f))
        }

        strokePaint.style = Paint.Style.STROKE

        // 1. Top-left corner (blue to purple)
        arcRect.set(rect.left, rect.top, rect.left + 2 * cornerRadius, rect.top + 2 * cornerRadius)
        strokePaint.shader = makeGradientShader(
            arcRect.centerX(), arcRect.centerY(),
            blue, blue
        )
        canvas.drawArc(arcRect, 180f, 90f, false, strokePaint)

        // 2. Top-right corner (blue to purple)
        arcRect.set(rect.right - 2 * cornerRadius, rect.top, rect.right, rect.top + 2 * cornerRadius)
        strokePaint.shader = makeGradientShader(
            arcRect.centerX(), arcRect.centerY(),
            blue, blue
        )
        canvas.drawArc(arcRect, 270f, 90f, false, strokePaint)

        // 3. Bottom-right corner (purple to blue)
        arcRect.set(rect.right - 2 * cornerRadius, rect.bottom - 2 * cornerRadius, rect.right, rect.bottom)
        strokePaint.shader = makeGradientShader(
            arcRect.centerX(), arcRect.centerY(),
            purple, blue
        )
        canvas.drawArc(arcRect, 0f, 90f, false, strokePaint)

        // 4. Bottom-left corner (purple to blue)
        arcRect.set(rect.left, rect.bottom - 2 * cornerRadius, rect.left + 2 * cornerRadius, rect.bottom)
        strokePaint.shader = makeGradientShader(
            arcRect.centerX(), arcRect.centerY(),
            purple, blue
        )
        canvas.drawArc(arcRect, 90f, 90f, false, strokePaint)

        // Reset shader to avoid affecting future strokes
        strokePaint.shader = null
    }


    override fun setAlpha(alpha: Int) {
            fillPaint.alpha = alpha
            strokePaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            fillPaint.colorFilter = colorFilter
            strokePaint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    // Extension to convert dp to px
    fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
