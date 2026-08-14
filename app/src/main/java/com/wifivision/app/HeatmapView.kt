package com.wifivision.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Measurement(
        val x: Float,
        val y: Float,
        val value: Float
    )

    private val measurements = mutableListOf<Measurement>()

    init {
        gridPaint.style = Paint.Style.STROKE
        gridPaint.strokeWidth = 1f
        gridPaint.color = 0x55FFFFFF

        pointPaint.style = Paint.Style.FILL
    }

    fun addMeasurement(
        x: Float,
        y: Float,
        value: Float
    ) {
        measurements.add(
            Measurement(
                x.coerceIn(0f, 1f),
                y.coerceIn(0f, 1f),
                value.coerceIn(0f, 100f)
            )
        )

        invalidate()
    }

    fun clearMeasurements() {
        measurements.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawColor(0xFF081018.toInt())

        // Grid
        val columns = 8
        val rows = 10

        for (i in 0..columns) {
            val x = w * i / columns
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }

        for (i in 0..rows) {
            val y = h * i / rows
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Heat zones
        for (m in measurements) {
            val cx = m.x * w
            val cy = m.y * h

            val radius = min(w, h) * 0.28f

            val gradient = RadialGradient(
                cx,
                cy,
                radius,
                heatColor(m.value),
                0x00000000,
                Shader.TileMode.CLAMP
            )

            pointPaint.shader = gradient
            canvas.drawCircle(cx, cy, radius, pointPaint)
            pointPaint.shader = null

            pointPaint.color = heatColor(m.value)
            canvas.drawCircle(cx, cy, 9f, pointPaint)

            pointPaint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(cx, cy, 3f, pointPaint)
        }
    }

    private fun heatColor(value: Float): Int {
        return when {
            value >= 80f -> 0xFFFF0000.toInt()
            value >= 60f -> 0xFFFF6600.toInt()
            value >= 40f -> 0xFFFFFF00.toInt()
            value >= 20f -> 0xFF00FF66.toInt()
            else -> 0xFF0088FF.toInt()
        }
    }
}
