package com.wifivision.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class SignalWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private var signal = 0f
    private var phase = 0f

    fun updateSignal(value: Float) {
        signal = value.coerceIn(0f, 100f)
        phase += 0.35f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        canvas.drawColor(0xFF101820.toInt())

        // Horizontal grid lines
        for (i in 1..4) {
            val y = height * i / 5f
            canvas.drawLine(
                0f,
                y,
                width,
                y,
                gridPaint
            )
        }

        // Center line
        val centerY = height / 2f
        canvas.drawLine(
            0f,
            centerY,
            width,
            centerY,
            gridPaint
        )

        val path = Path()

        val amplitude =
            10f + (signal / 100f) * (height * 0.35f)

        val frequency =
            0.035f + (signal / 100f) * 0.025f

        for (x in 0..width.toInt()) {

                x / width

            val y =
                centerY +
                    sin(
                        x * frequency + phase
                    ).toFloat() * amplitude

            if (x == 0) {
                path.moveTo(x.toFloat(), y)
            } else {
                path.lineTo(x.toFloat(), y)
            }

        }

        canvas.drawPath(path, wavePaint)
    }
}
