package com.hommlie.partner.ui.attendence

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class TopRightDotSpan(
    private val radius: Float,
    private val color: Int
) : LineBackgroundSpan {

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineNumber: Int
    ) {
        //  Measure text width
        val textWidth = paint.measureText(text, start, end)

        //  Calculate where the text is drawn (centered in the cell)
        val cellWidth = (right - left).toFloat()
        val textStartX = left + (cellWidth - textWidth) / 2f

        //  Calculate top of the text
        val fontMetrics = paint.fontMetrics
        val textTop = baseline + fontMetrics.ascent // ascent is negative

        //  Define larger gap for badge effect
        val gapX = radius * 2f  // horizontal gap
        val gapY = radius * 1.0f  // vertical gap

        //  Position the dot with gaps
        val dotX = textStartX + textWidth + gapX
        val dotY = textTop + radius - gapY

        //  Draw the dot
        val oldColor = paint.color
        paint.color = color
        canvas.drawCircle(dotX, dotY, radius, paint)
        paint.color = oldColor
    }
}
