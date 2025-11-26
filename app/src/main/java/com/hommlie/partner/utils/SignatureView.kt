package com.hommlie.partner.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class SignatureView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var paint: Paint = Paint()
    private var path: Path = Path()
    private var canvasBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var bitmapPaint: Paint = Paint(Paint.DITHER_FLAG)
    private var isSigned = false   // Track signature

    init {
        paint.isAntiAlias = true
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 8f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap!!, 0f, 0f, bitmapPaint)
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                isSigned = true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                canvas?.drawPath(path, paint)
                path.reset()
            }
        }

        invalidate()
        return true
    }

    fun clearSignature() {
        canvasBitmap?.eraseColor(Color.TRANSPARENT)
        isSigned = false
        invalidate()
    }

    fun hasSignature(): Boolean {
        return isSigned
    }

    fun saveSignature(): Bitmap {
        return canvasBitmap!!
    }

    fun saveSignatureToFile(): ByteArray? {
        val outputStream = ByteArrayOutputStream()
        canvasBitmap?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}

