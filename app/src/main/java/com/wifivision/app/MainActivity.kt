package com.wifivision.app

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
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
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                updateWifi()
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

        measureButton.setOnClickListener {
            requestPermissionsIfNeeded()
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }

        requestPermissionsIfNeeded()
    }

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissions(
                missing.toTypedArray(),
                REQUEST_WIFI_PERMISSIONS
            )
        } else {
            measure()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == REQUEST_WIFI_PERMISSIONS) {

            val allGranted = grantResults.isNotEmpty() &&
                    grantResults.all {
                        it == PackageManager.PERMISSION_GRANTED
                    }

            if (allGranted) {
                measure()
            } else {
                status.text =
                    "Wi-Fi permissions are required to scan networks"
            }
        }
    }

    private fun isLocationEnabled(): Boolean {

        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
            ) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        }
    }

    private fun measure() {

        if (!wifiManager.isWifiEnabled) {
            status.text = "Wi-Fi is OFF"
            return
        }

        if (!isLocationEnabled()) {
            status.text = "Turn ON Location to scan Wi-Fi"

            try {
                startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            } catch (_: Exception) {
                // Ignore if settings cannot be opened.
            }

            return
        }

        status.text = "Scanning Wi-Fi..."

        updateWifi()

        try {

            @Suppress("DEPRECATION")
            val started = wifiManager.startScan()

            if (!started) {
                status.text =
                    "Scan throttled — showing latest data"
            }

        } catch (_: SecurityException) {

            status.text =
                "Wi-Fi permission required"
        }
    }

    @Suppress("DEPRECATION")
    private fun updateWifi() {

        try {

            val info = wifiManager.connectionInfo

            val currentSsid = info.ssid
                ?.removeSurrounding("\"")
                ?.takeIf {
                    it.isNotBlank() &&
                    it != "<unknown ssid>"
                }
                ?: "Unknown"

            val currentRssi = info.rssi

            ssid.text = "SSID: $currentSsid"

            if (currentRssi > -100) {

                rssi.text = "RSSI: $currentRssi dBm"

                quality.text = when {
                    currentRssi >= -50 ->
                        "Signal quality: Excellent"

                    currentRssi >= -60 ->
                        "Signal quality: Good"

                    currentRssi >= -70 ->
                        "Signal quality: Fair"

                    currentRssi >= -80 ->
                        "Signal quality: Weak"

                    else ->
                        "Signal quality: Very weak"
                }

            } else {

                rssi.text = "RSSI: unavailable"
                quality.text = "Signal quality: unavailable"
            }

            val results = wifiManager.scanResults

            val sortedResults = results
                .filter {
                    it.SSID.isNotBlank()
                }
                .distinctBy {
                    "${it.SSID}:${it.BSSID}"
                }
                .sortedByDescending {
                    it.level
                }
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
                    "No networks found.\n\n" +
                    "Make sure Wi-Fi and Location are ON."
                } else {
                    text.toString()
                }

            status.text =
                "Measurement complete • ${sortedResults.size} networks"

        } catch (e: SecurityException) {

            status.text =
                "Wi-Fi permission required"

        } catch (e: Exception) {

            status.text =
                "Unable to read Wi-Fi information"
        }
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(receiver)
        } catch (_: Exception) {
            // Receiver already unregistered.
        }

        super.onDestroy()
    }

    companion object {
        private const val REQUEST_WIFI_PERMISSIONS = 100
    }
}
