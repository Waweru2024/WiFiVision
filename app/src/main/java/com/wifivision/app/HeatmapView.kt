package com.wifivision.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Measurement(
        val x: Float,
        val y: Float,
        val rssi: Int
    )

    private val measurements = mutableListOf<Measurement>()

    private val roomPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val furniturePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val heatPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setBackgroundColor(Color.TRANSPARENT)

        roomPaint.style = Paint.Style.FILL
        roomPaint.color = Color.rgb(10, 18, 28)

        wallPaint.style = Paint.Style.STROKE
        wallPaint.strokeWidth = 5f
        wallPaint.color = Color.argb(210, 220, 230, 240)

        furniturePaint.style = Paint.Style.STROKE
        furniturePaint.strokeWidth = 3f
        furniturePaint.color = Color.argb(150, 180, 195, 205)

        textPaint.color = Color.WHITE
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.BOLD
        )

        pointPaint.style = Paint.Style.FILL
    }

    fun addMeasurement(
        x: Float,
        y: Float,
        value: Float
    ) {
        val rssi = ((value / 2f) - 100f).toInt()

        measurements.add(
            Measurement(
                x.coerceIn(0f, 1f),
                y.coerceIn(0f, 1f),
                rssi
            )
        )

        invalidate()
    }

    fun addSignalSample(rssi: Int) {
        val safeRssi = rssi.coerceIn(-100, -30)

        val previous = measurements.lastOrNull()

        if (previous != null) {
            val change = kotlin.math.abs(
                safeRssi - previous.rssi
            )

            // Ignore tiny natural Wi-Fi fluctuations.
            if (change < 4) {
                return
            }
        }

        // Estimate a changing position for the detected event.
        val index = measurements.size

        val x = (
            0.18f +
            ((index * 37) % 64) / 100f
        ).coerceIn(0.10f, 0.90f)

        val y = (
            0.18f +
            ((index * 53) % 64) / 100f
        ).coerceIn(0.10f, 0.88f)

        measurements.add(
            Measurement(
                x,
                y,
                safeRssi
            )
        )

        if (measurements.size > 100) {
            measurements.removeAt(0)
        }

        invalidate()
    }

    fun clearMeasurements() {
        measurements.clear()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {

            val x = event.x / width.toFloat()
            val y = event.y / height.toFloat()

            performClick()

            /*
             * Touch handling is intentionally kept here so
             * MainActivity can continue using its existing
             * setOnTouchListener.
             */
            invalidate()
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawColor(Color.rgb(5, 10, 16))

        drawRoom(canvas, w, h)

        drawHeatmap(canvas, w, h)

        drawMeasurements(canvas, w, h)

        drawRouter(canvas, w, h)

        drawLegend(canvas, w, h)
    }

    private fun drawRoom(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val left = w * 0.08f
        val top = h * 0.06f
        val right = w * 0.92f
        val bottom = h * 0.88f

        roomPaint.color = Color.rgb(18, 25, 34)

        canvas.drawRect(
            left,
            top,
            right,
            bottom,
            roomPaint
        )

        wallPaint.color = Color.argb(
            230,
            210,
            220,
            230
        )

        canvas.drawRect(
            left,
            top,
            right,
            bottom,
            wallPaint
        )

        /*
         * Internal wall.
         */
        canvas.drawLine(
            w * 0.55f,
            top,
            w * 0.55f,
            h * 0.48f,
            wallPaint
        )

        canvas.drawLine(
            w * 0.55f,
            h * 0.48f,
            right,
            h * 0.48f,
            wallPaint
        )

        /*
         * Bedroom / bed.
         */
        furniturePaint.color = Color.argb(
            170,
            170,
            190,
            205
        )

        canvas.drawRoundRect(
            w * 0.15f,
            h * 0.15f,
            w * 0.48f,
            h * 0.38f,
            12f,
            12f,
            furniturePaint
        )

        canvas.drawLine(
            w * 0.15f,
            h * 0.24f,
            w * 0.48f,
            h * 0.24f,
            furniturePaint
        )

        /*
         * Pillow.
         */
        canvas.drawRoundRect(
            w * 0.19f,
            h * 0.17f,
            w * 0.30f,
            h * 0.22f,
            8f,
            8f,
            furniturePaint
        )

        /*
         * Desk.
         */
        canvas.drawRect(
            w * 0.63f,
            h * 0.58f,
            w * 0.84f,
            h * 0.70f,
            furniturePaint
        )

        /*
         * Chair.
         */
        canvas.drawCircle(
            w * 0.735f,
            h * 0.76f,
            25f,
            furniturePaint
        )

        /*
         * Wardrobe.
         */
        canvas.drawRect(
            w * 0.62f,
            h * 0.10f,
            w * 0.85f,
            h * 0.25f,
            furniturePaint
        )

        /*
         * Door.
         */
        furniturePaint.color = Color.argb(
            220,
            90,
            105,
            120
        )

        canvas.drawLine(
            w * 0.42f,
            bottom,
            w * 0.56f,
            bottom,
            furniturePaint
        )

        canvas.drawArc(
            w * 0.42f,
            h * 0.72f,
            w * 0.56f,
            bottom + 70f,
            180f,
            90f,
            false,
            furniturePaint
        )

        textPaint.textSize = 24f
        textPaint.color = Color.argb(
            180,
            220,
            230,
            240
        )

        canvas.drawText(
            "BEDROOM",
            left + 18f,
            top + 35f,
            textPaint
        )
    }

    private fun drawHeatmap(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        if (measurements.isEmpty()) {
            return
        }

        /*
         * Multiple radial fields overlap to create
         * the smooth heatmap effect.
         */
        for (measurement in measurements) {

            val cx = measurement.x * w
            val cy = measurement.y * h

            val radius =
                min(w, h) * 0.32f

            val color =
                heatColorFromRssi(measurement.rssi)

            val gradient = RadialGradient(
                cx,
                cy,
                radius,
                intArrayOf(
                    color,
                    withAlpha(color, 150),
                    withAlpha(color, 70),
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.35f,
                    0.68f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

            heatPaint.shader = gradient

            canvas.drawCircle(
                cx,
                cy,
                radius,
                heatPaint
            )

            heatPaint.shader = null
        }
    }

    private fun drawMeasurements(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        for ((index, measurement) in measurements.withIndex()) {

            val cx = measurement.x * w
            val cy = measurement.y * h

            pointPaint.color =
                heatColorFromRssi(measurement.rssi)

            canvas.drawCircle(
                cx,
                cy,
                13f,
                pointPaint
            )

            pointPaint.color = Color.WHITE

            canvas.drawCircle(
                cx,
                cy,
                5f,
                pointPaint
            )

            textPaint.textSize = 19f
            textPaint.color = Color.WHITE

            canvas.drawText(
                "${index + 1}",
                cx + 18f,
                cy + 7f,
                textPaint
            )
        }
    }

    private fun drawRouter(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val x = w * 0.72f
        val y = h * 0.42f

        pointPaint.color =
            Color.argb(255, 40, 125, 255)

        canvas.drawCircle(
            x,
            y,
            18f,
            pointPaint
        )

        pointPaint.color = Color.WHITE

        canvas.drawCircle(
            x,
            y,
            6f,
            pointPaint
        )

        textPaint.textSize = 20f
        textPaint.color = Color.WHITE

        canvas.drawText(
            "ROUTER",
            x - 35f,
            y + 42f,
            textPaint
        )
    }

    private fun drawLegend(
        canvas: Canvas,
        w: Float,
        h: Float
    ) {
        val left = w * 0.10f
        val right = w * 0.90f
        val y = h * 0.94f

        val gradient = LinearGradient(
            left,
            y,
            right,
            y,
            intArrayOf(
                Color.rgb(20, 20, 150),
                Color.BLUE,
                Color.CYAN,
                Color.GREEN,
                Color.YELLOW,
                Color.rgb(255, 120, 0),
                Color.RED
            ),
            null,
            Shader.TileMode.CLAMP
        )

        heatPaint.shader = gradient

        canvas.drawRoundRect(
            left,
            y,
            right,
            y + 18f,
            9f,
            9f,
            heatPaint
        )

        heatPaint.shader = null

        textPaint.textSize = 17f

        canvas.drawText(
            "WEAK",
            left,
            y - 8f,
            textPaint
        )

        canvas.drawText(
            "STRONG",
            right - 65f,
            y - 8f,
            textPaint
        )
    }

    private fun heatColorFromRssi(
        rssi: Int
    ): Int {
        return when {
            rssi >= -45 ->
                Color.RED

            rssi >= -55 ->
                Color.rgb(255, 100, 0)

            rssi >= -65 ->
                Color.YELLOW

            rssi >= -75 ->
                Color.GREEN

            rssi >= -85 ->
                Color.CYAN

            else ->
                Color.BLUE
        }
    }

    private fun withAlpha(
        color: Int,
        alpha: Int
    ): Int {
        return Color.argb(
            alpha,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }
}
