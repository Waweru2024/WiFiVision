package com.wifivision.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var wifiManager: WifiManager

    private lateinit var status: TextView
    private lateinit var scanTimer: TextView
    private lateinit var pointCounter: TextView
    private lateinit var weakestValue: TextView
    private lateinit var averageValue: TextView
    private lateinit var strongestValue: TextView

    private lateinit var measureButton: Button
    private lateinit var sensingButton: Button
    private lateinit var clearButton: Button
    private lateinit var heatmap: HeatmapView

    private val handler = Handler(Looper.getMainLooper())

    private val samples = ArrayDeque<Int>()

    private var sensing = false
    private var scanSeconds = 0

    private val sensingRunnable = object : Runnable {
        override fun run() {
            if (sensing) {
                collectSignalSample()

                scanSeconds++
                updateTimer()

                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        wifiManager =
            applicationContext.getSystemService(
                Context.WIFI_SERVICE
            ) as WifiManager

        status = findViewById(R.id.status)
        scanTimer = findViewById(R.id.scanTimer)
        pointCounter = findViewById(R.id.pointCounter)

        weakestValue = findViewById(R.id.weakestValue)
        averageValue = findViewById(R.id.averageValue)
        strongestValue = findViewById(R.id.strongestValue)

        measureButton = findViewById(R.id.measureButton)
        sensingButton = findViewById(R.id.sensingButton)
        clearButton = findViewById(R.id.clearButton)

        heatmap = findViewById(R.id.heatmap)

        /*
         * Tapping the square room records a manual
         * Wi-Fi measurement at that location.
         */
        heatmap.setOnTouchListener { view, event ->

            if (event.action == MotionEvent.ACTION_UP) {

                val x =
                    (event.x / view.width.toFloat())
                        .coerceIn(0f, 1f)

                val y =
                    (event.y / view.height.toFloat())
                        .coerceIn(0f, 1f)

                measureWifiAtPoint(x, y)
            }

            true
        }

        measureButton.setOnClickListener {
            measureWifi()
        }

        sensingButton.setOnClickListener {
            toggleSensing()
        }

        clearButton.setOnClickListener {
            clearScan()
        }

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(
                Manifest.permission.NEARBY_WIFI_DEVICES
            )
        }

        permissions.add(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        permissions.add(
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val missing = permissions.filter {
            checkSelfPermission(it) !=
                    PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {

            requestPermissions(
                missing.toTypedArray(),
                100
            )

        } else {

            measureWifi()
        }
    }

    @Suppress("DEPRECATION")
    private fun getCurrentRssi(): Int {

        return wifiManager.connectionInfo.rssi
    }

    private fun measureWifi() {

        if (!wifiManager.isWifiEnabled) {

            status.text = "Wi-Fi is OFF"
            return
        }

        try {

            val rssi = getCurrentRssi()

            if (rssi <= -100) {

                status.text = "Signal unavailable"
                return
            }

            addSample(rssi)

            status.text =
                "Measurement complete"

        } catch (_: SecurityException) {

            status.text =
                "Wi-Fi permission required"

        } catch (_: Exception) {

            status.text =
                "Unable to read Wi-Fi signal"
        }
    }

    private fun toggleSensing() {

        if (!wifiManager.isWifiEnabled) {

            status.text = "Turn Wi-Fi ON first"
            return
        }

        if (sensing) {

            stopSensing()

        } else {

            startSensing()
        }
    }

    private fun startSensing() {

        samples.clear()

        scanSeconds = 0
        sensing = true

        sensingButton.text = "STOP SCAN"

        status.text =
            "Scanning signal changes..."

        scanTimer.text = "00:00"

        handler.removeCallbacks(
            sensingRunnable
        )

        handler.post(sensingRunnable)
    }

    private fun stopSensing() {

        sensing = false

        handler.removeCallbacks(
            sensingRunnable
        )

        sensingButton.text = "START SCAN"

        status.text =
            "Scan complete"
    }

    @Suppress("DEPRECATION")
    private fun collectSignalSample() {

        try {

            val currentRssi =
                getCurrentRssi()

            if (currentRssi <= -100) {

                status.text =
                    "Signal unavailable"

                return
            }

            /*
             * Immediately after obtaining RSSI,
             * send it to the heatmap.
             *
             * HeatmapView decides whether the
             * change is large enough to display.
             */
            heatmap.addSignalSample(
                currentRssi
            )

            addSample(currentRssi)

            status.text =
                "Scanning • $currentRssi dBm"

        } catch (_: SecurityException) {

            status.text =
                "Wi-Fi permission required"

        } catch (_: Exception) {

            status.text =
                "Unable to read Wi-Fi signal"
        }
    }

    private fun addSample(rssi: Int) {

        samples.addLast(rssi)

        while (samples.size > 60) {
            samples.removeFirst()
        }

        updateStatistics()
    }

    private fun updateStatistics() {

        if (samples.isEmpty()) {

            weakestValue.text = "-- dBm"
            averageValue.text = "-- dBm"
            strongestValue.text = "-- dBm"
            pointCounter.text = "0 points"

            return
        }

        val values = samples.toList()

        val weakest =
            values.minOrNull() ?: return

        val strongest =
            values.maxOrNull() ?: return

        val average =
            values.average()

        weakestValue.text =
            "$weakest dBm"

        averageValue.text =
            "${average.toInt()} dBm"

        strongestValue.text =
            "$strongest dBm"

        pointCounter.text =
            "${values.size} samples"
    }

    private fun updateTimer() {

        val minutes =
            scanSeconds / 60

        val seconds =
            scanSeconds % 60

        scanTimer.text =
            String.format(
                "%02d:%02d",
                minutes,
                seconds
            )
    }

    private fun measureWifiAtPoint(
        x: Float,
        y: Float
    ) {

        if (!wifiManager.isWifiEnabled) {

            status.text =
                "Wi-Fi is OFF"

            return
        }

        try {

            val currentRssi =
                getCurrentRssi()

            if (currentRssi <= -100) {

                status.text =
                    "Signal unavailable"

                return
            }

            /*
             * Manual room measurement.
             *
             * The user taps where they are standing
             * in the square room.
             */
            heatmap.addMeasurement(
                x,
                y,
                ((currentRssi + 100) * 2)
                    .coerceIn(0, 100)
                    .toFloat()
            )

            addSample(currentRssi)

            status.text =
                "Point measured • $currentRssi dBm"

        } catch (_: SecurityException) {

            status.text =
                "Wi-Fi permission required"

        } catch (_: Exception) {

            status.text =
                "Unable to measure signal"
        }
    }

    private fun clearScan() {

        stopSensing()

        samples.clear()

        scanSeconds = 0

        scanTimer.text =
            "00:00"

        pointCounter.text =
            "0 points"

        weakestValue.text =
            "-- dBm"

        averageValue.text =
            "-- dBm"

        strongestValue.text =
            "-- dBm"

        heatmap.clearMeasurements()

        status.text =
            "Ready to scan"
    }

    override fun onDestroy() {

        sensing = false

        handler.removeCallbacks(
            sensingRunnable
        )

        super.onDestroy()
    }
}
