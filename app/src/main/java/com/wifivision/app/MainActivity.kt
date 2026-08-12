package com.wifivision.app

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var wifiManager: WifiManager

    private lateinit var status: TextView
    private lateinit var ssid: TextView
    private lateinit var rssi: TextView
    private lateinit var quality: TextView
    private lateinit var networks: TextView
    private lateinit var measureButton: Button

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateWifi()
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

        measureButton.setOnClickListener {
            requestPermissionsIfNeeded()
        }

        registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            Context.RECEIVER_NOT_EXPORTED
        )

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(missing.toTypedArray(), 100)
        } else {
            measure()
        }
    }

    private fun measure() {

        if (!wifiManager.isWifiEnabled) {
            status.text = "Wi-Fi is OFF"
            return
        }

        status.text = "Scanning Wi-Fi..."

        updateWifi()

        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()

        if (!started) {
            status.text = "Scan throttled — showing latest data"
        }
    }

    @Suppress("DEPRECATION")
    private fun updateWifi() {

        val info = wifiManager.connectionInfo

        val currentSsid = info.ssid
            ?.removeSurrounding("\"")
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

        try {

            val results = wifiManager.scanResults

            val text = StringBuilder()

            for ((index, result) in results
                .sortedByDescending { it.level }
                .take(15)
                .withIndex()) {

                val name =
                    if (result.SSID.isBlank()) "<hidden>"
                    else result.SSID

                text.append(
                    "${index + 1}. $name\n" +
                    "   ${result.level} dBm\n\n"
                )
            }

            networks.text =
                if (text.isEmpty()) "No networks found."
                else text.toString()

            status.text =
                "Measurement complete • ${results.size} networks"

        } catch (e: SecurityException) {

            status.text = "Wi-Fi permission required"

        }
    }

    override fun onDestroy() {

        unregisterReceiver(receiver)

        super.onDestroy()
    }
}
