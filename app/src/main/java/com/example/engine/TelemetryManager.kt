package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.model.SystemStatusState
import com.example.model.TelemetryState
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc: Location = result.lastLocation ?: return
            val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f).coerceAtLeast(0f) else 0f
            val bearing = if (loc.hasBearing()) loc.bearing else _telemetry.value.bearingDegrees
            _telemetry.value = _telemetry.value.copy(
                speedKmh = speedKmh,
                bearingDegrees = bearing,
                latitude = loc.latitude,
                longitude = loc.longitude,
                hasGpsFix = true,
                hasLocationPermission = true
            )
            _systemStatus.value = _systemStatus.value.copy(isGpsActive = true)
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
                    _telemetry.value = _telemetry.value.copy(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f).coerceAtLeast(0f) else 0f,
                        hasGpsFix = true
                    )
                }
            }
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            locationActive = true
        }
    }

    private fun startClockAndSystemPoll() {
        clockJob?.cancel()
        clockJob = scope.launch {
            // Classic Apple CarPlay 12-hour clock format (e.g., 9:41)
            val timeFormat = SimpleDateFormat("h:mm", Locale.US)
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
            while (isActive) {
                val now = Date()
                _systemStatus.value = _systemStatus.value.copy(
                    timeText = timeFormat.format(now),
                    dateText = dateFormat.format(now)
                )
                updateSystemIndicators()
                delay(4000L)
            }
        }
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
