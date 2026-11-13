package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
import com.example.model.WeatherState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class TelemetryManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _systemStatus = MutableStateFlow(SystemStatusState())
    val systemStatus: StateFlow<SystemStatusState> = _systemStatus.asStateFlow()

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private var clockJob: Job? = null
    private var locationActive = false
    private var lastAlertToneMs = 0L
    private var lastWeatherFetchMs = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc: Location = result.lastLocation ?: return
            val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f).coerceAtLeast(0f) else 0f
            val bearing = if (loc.hasBearing()) loc.bearing else _telemetry.value.bearingDegrees
            val peak = max(_telemetry.value.peakSpeedKmh, speedKmh)
            _telemetry.value = _telemetry.value.copy(
                speedKmh = speedKmh,
                peakSpeedKmh = peak,
                bearingDegrees = bearing,
                latitude = loc.latitude,
                longitude = loc.longitude,
                hasGpsFix = true,
                hasLocationPermission = true
            )
            _systemStatus.value = _systemStatus.value.copy(isGpsActive = true)
            refreshWeatherIfNeeded(loc.latitude, loc.longitude, force = false)
        }
    }

    fun start() {
        refreshPermissionsAndStatus()
        startClockAndSystemPoll()
        startLocationUpdatesIfPermitted()
        refreshNextCalendarEvent()
    }

    fun stop() {
        clockJob?.cancel()
        if (locationActive) {
            runCatching { fusedClient.removeLocationUpdates(locationCallback) }
            locationActive = false
        }
    }

    fun refreshPermissionsAndStatus() {
        val hasLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val hasCalendar = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        _telemetry.value = _telemetry.value.copy(
            hasLocationPermission = hasLocation,
            hasCalendarPermission = hasCalendar
        )

        if (hasLocation && !locationActive) {
            startLocationUpdatesIfPermitted()
        }
        if (hasCalendar) {
            refreshNextCalendarEvent()
        }
        updateSystemIndicators()
        updateBatteryTelemetry()
    }

    fun refreshWeatherNow() {
        val current = _telemetry.value
        if (current.hasGpsFix) {
            refreshWeatherIfNeeded(current.latitude, current.longitude, force = true)
        }
    }

    fun resetTripTimer() {
        _telemetry.value = _telemetry.value.copy(
            tripStartTimeMs = System.currentTimeMillis(),
            peakSpeedKmh = _telemetry.value.speedKmh
        )
    }

    private fun refreshWeatherIfNeeded(lat: Double, lon: Double, force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && _telemetry.value.weather.hasData && now - lastWeatherFetchMs < 15 * 60 * 1000L) {
            return
        }
        if (!force && now - lastWeatherFetchMs < 60_000L) {
            return
        }
        lastWeatherFetchMs = now
        _telemetry.value = _telemetry.value.copy(
            weather = _telemetry.value.weather.copy(isLoading = true)
        )

        scope.launch(Dispatchers.IO) {
            val fetched = runCatching {
                val urlStr = "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,apparent_temperature,relative_humidity_2m,is_day,weather_code,wind_speed_10m" +
                    "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                    "&timezone=auto&forecast_days=1"
                val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                }
                try {
                    if (conn.responseCode !in 200..299) return@runCatching null
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val root = JSONObject(body)
                    val cur = root.getJSONObject("current")
                    val daily = root.optJSONObject("daily")
                    val high = daily?.optJSONArray("temperature_2m_max")?.optDouble(0, cur.optDouble("temperature_2m"))?.toFloat()
                        ?: cur.optDouble("temperature_2m").toFloat()
                    val low = daily?.optJSONArray("temperature_2m_min")?.optDouble(0, cur.optDouble("temperature_2m"))?.toFloat()
                        ?: cur.optDouble("temperature_2m").toFloat()
                    val precip = daily?.optJSONArray("precipitation_probability_max")?.optInt(0, 0) ?: 0

                    WeatherState(
                        hasData = true,
                        isLoading = false,
                        temperatureC = cur.optDouble("temperature_2m").toFloat(),
                        apparentTempC = cur.optDouble("apparent_temperature").toFloat(),
                        highTempC = high,
                        lowTempC = low,
                        windSpeedKmh = cur.optDouble("wind_speed_10m").toFloat(),
                        humidityPercent = cur.optInt("relative_humidity_2m", 0),
                        precipChancePercent = precip,
                        weatherCode = cur.optInt("weather_code", 0),
                        isDaylight = cur.optInt("is_day", 1) == 1,
                        lastUpdatedMs = System.currentTimeMillis()
                    )
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()

            _telemetry.value = _telemetry.value.copy(
                weather = fetched ?: _telemetry.value.weather.copy(isLoading = false)
            )
        }
    }

    fun playSpeedWarningChimeIfNeeded(enabled: Boolean, currentSpeedKmh: Float, limitKmh: Int) {
        if (!enabled || currentSpeedKmh <= limitKmh) return
        val now = System.currentTimeMillis()
        if (now - lastAlertToneMs < 8000L) return
        lastAlertToneMs = now
        scope.launch(Dispatchers.IO) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 45)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP, 180)
                delay(240L)
                tone.release()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdatesIfPermitted() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f).coerceAtLeast(0f) else 0f
                    _telemetry.value = _telemetry.value.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = speedKmh,
                        peakSpeedKmh = max(_telemetry.value.peakSpeedKmh, speedKmh),
                        hasGpsFix = true
                    )
                    refreshWeatherIfNeeded(loc.latitude, loc.longitude, force = false)
                }
            }
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            locationActive = true
        }
    }

    private fun startClockAndSystemPoll() {
        clockJob?.cancel()
        clockJob = scope.launch {
            val timeFormat = SimpleDateFormat("h:mm", Locale.US)
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
            while (isActive) {
                val now = Date()
                _systemStatus.value = _systemStatus.value.copy(
                    timeText = timeFormat.format(now),
                    dateText = dateFormat.format(now)
                )
                updateSystemIndicators()
                updateBatteryTelemetry()
                delay(4000L)
            }
        }
    }

    private fun updateBatteryTelemetry() {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else _telemetry.value.batteryPercent
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
        val tempC = tempTenths / 10f

        _telemetry.value = _telemetry.value.copy(
            batteryPercent = pct,
            isCharging = charging,
            batteryTempC = tempC
        )
    }

    private fun updateSystemIndicators() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var wifi = false
        var cellular = false
        if (cm != null) {
            val net = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(net)
            if (caps != null) {
                wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                cellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            }
        }

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btEnabled = runCatching { btManager?.adapter?.isEnabled == true }.getOrDefault(false)

        val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsProviderEnabled = runCatching {
            locManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)

        _systemStatus.value = _systemStatus.value.copy(
            isWifiConnected = wifi,
            isCellularConnected = cellular,
            isBluetoothEnabled = btEnabled,
            isGpsActive = gpsProviderEnabled && _telemetry.value.hasLocationPermission
        )
    }

    @SuppressLint("MissingPermission")
    fun refreshNextCalendarEvent() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        scope.launch(Dispatchers.IO) {
            runCatching {
                val now = System.currentTimeMillis()
                val end = now + 24 * 60 * 60 * 1000L
                val projection = arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN
                )
                val uriBuilder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                android.content.ContentUris.appendId(uriBuilder, now)
                android.content.ContentUris.appendId(uriBuilder, end)

                context.contentResolver.query(
                    uriBuilder.build(),
                    projection,
                    null,
                    null,
                    "${CalendarContract.Instances.BEGIN} ASC"
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val title = cursor.getString(0)
                        val beginMs = cursor.getLong(1)
                        val fmt = SimpleDateFormat("h:mm a", Locale.US)
                        _telemetry.value = _telemetry.value.copy(
                            nextEventTitle = title,
                            nextEventTime = fmt.format(Date(beginMs))
                        )
                    } else {
                        _telemetry.value = _telemetry.value.copy(
                            nextEventTitle = null,
                            nextEventTime = null
                        )
                    }
                }
            }
        }
    }
}
