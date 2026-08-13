package com.wifivision.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.sqrt

class MainActivity : Activity() {

    private lateinit var wifiManager: WifiManager

    private lateinit var status: TextView
    private lateinit var ssid: TextView
    private lateinit var rssi: TextView
    private lateinit var quality: TextView
    private lateinit var networks: TextView
    private lateinit var measureButton: Button
    private lateinit var sensingButton: Button
    private lateinit var sensingResult: TextView
    private lateinit var stability: TextView
    private lateinit var fluctuation: TextView

    private val handler = Handler(Looper.getMainLooper())

    private val samples = ArrayDeque<Int>()
    private var sensing = false

    private val sensingRunnable = object : Runnable {
        override fun run() {
            if (sensing) {
                collectSignalSample()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        wifiManager =
            applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        status = findViewById(R.id.status)
        ssid = findViewById(R.id.ssid)
        rssi = findViewById(R.id.rssi)
        quality = findViewById(R.id.quality)
        networks = findViewById(R.id.networks)
        measureButton = findViewById(R.id.measureButton)
        sensingButton = findViewById(R.id.sensingButton)
        sensingResult = findViewById(R.id.sensingResult)
        stability = findViewById(R.id.stability)
        fluctuation = findViewById(R.id.fluctuation)

        measureButton.setOnClickListener {
            measureWifi()
        }

        sensingButton.setOnClickListener {
            toggleSensing()
        }

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 100)
        } else {
            measureWifi()
        }
    }

    private fun measureWifi() {

        if (!wifiManager.isWifiEnabled) {
            status.text = "Wi-Fi is OFF"
            return
        }

        try {
            val info = wifiManager.connectionInfo

            val currentSsid = info.ssid
                ?.removeSurrounding("\"")
                ?.takeIf {
                    it.isNotBlank() && it != "<unknown ssid>"
                }
                ?: "Unknown"

            val currentRssi = info.rssi

            ssid.text = "SSID: $currentSsid"

            if (currentRssi > -100) {

                rssi.text = "RSSI: $currentRssi dBm"

                quality.text = when {
                    currentRssi >= -50 -> "Signal quality: Excellent"
                    currentRssi >= -60 -> "Signal quality: Good"
                    currentRssi >= -70 -> "Signal quality: Fair"
                    currentRssi >= -80 -> "Signal quality: Weak"
                    else -> "Signal quality: Very weak"
                }

            } else {
                rssi.text = "RSSI: unavailable"
                quality.text = "Signal quality: unavailable"
            }

            @Suppress("DEPRECATION")
            val results = wifiManager.scanResults

            val sortedResults = results
                .filter { it.SSID.isNotBlank() }
                .sortedByDescending { it.level }
                .take(15)

            val text = StringBuilder()

            for ((index, result) in sortedResults.withIndex()) {
                text.append(
                    "${index + 1}. ${result.SSID}\n" +
                    "   ${result.level} dBm\n\n"
                )
            }

            networks.text =
                if (text.isEmpty()) {
                    "No networks found."
                } else {
                    text.toString()
                }

            status.text =
                "Measurement complete • ${sortedResults.size} networks"

        } catch (_: SecurityException) {
            status.text = "Wi-Fi permission required"
        }
    }

    private fun toggleSensing() {

        if (!wifiManager.isWifiEnabled) {
            sensingResult.text = "Turn Wi-Fi ON first"
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
        sensing = true

        sensingButton.text = "STOP SENSING"
        sensingResult.text = "🟡 Calibrating signal..."
        stability.text = "Stability: --"
        fluctuation.text = "Fluctuation: --"

        handler.post(sensingRunnable)
    }

    private fun stopSensing() {

        sensing = false
        handler.removeCallbacks(sensingRunnable)

        sensingButton.text = "START SENSING"
        sensingResult.text = "Sensing stopped"
    }

    @Suppress("DEPRECATION")
    private fun collectSignalSample() {

        try {

            val info = wifiManager.connectionInfo
            val currentRssi = info.rssi

            if (currentRssi <= -100) {
                return
            }

            samples.addLast(currentRssi)

            while (samples.size > 20) {
                samples.removeFirst()
            }

            rssi.text = "RSSI: $currentRssi dBm"

            if (samples.size < 5) {
                sensingResult.text =
                    "🟡 Calibrating signal...\n${samples.size}/5 samples"
                return
            }

            val values = samples.toList()

            val mean =
                values.average()

            val variance =
                values.map {
                    (it - mean) * (it - mean)
                }.average()

            val standardDeviation =
                sqrt(variance)

            val range =
                values.maxOrNull()!! - values.minOrNull()!!

            val movementScore =
                ((standardDeviation * 20) + (range * 2))
                    .coerceIn(0.0, 100.0)

            val stabilityScore =
                (100.0 - movementScore)
                    .coerceIn(0.0, 100.0)

            fluctuation.text =
                "Fluctuation: ${movementScore.toInt()}%"

            stability.text =
                "Stability: ${stabilityScore.toInt()}%"

            when {
                movementScore < 20 -> {
                    sensingResult.text =
                        "🟢 Signal stable\nNo significant movement detected"
                }

                movementScore < 50 -> {
                    sensingResult.text =
                        "🟡 Signal changing\nPossible movement detected"
                }

                else -> {
                    sensingResult.text =
                        "🟠 Strong signal changes\nSignificant movement detected"
                }
            }

        } catch (_: SecurityException) {

            sensingResult.text =
                "Wi-Fi permission required"

        } catch (_: Exception) {

            sensingResult.text =
                "Unable to read Wi-Fi signal"
        }
    }

    override fun onDestroy() {

        sensing = false
        handler.removeCallbacks(sensingRunnable)

        super.onDestroy()
    }
}

