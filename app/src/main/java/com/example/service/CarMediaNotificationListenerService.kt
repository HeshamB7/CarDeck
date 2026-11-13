package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.model.MediaPlaybackState
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

class CarMediaNotificationListenerService : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var progressJob: Job? = null

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachToBestController(controllers)
        }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishCurrentState()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishCurrentState()
        }

        override fun onSessionDestroyed() {
            refreshActiveSessions()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _mediaState.value = _mediaState.value.copy(hasNotificationPermission = true)
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val component = ComponentName(this, CarMediaNotificationListenerService::class.java)
        runCatching {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, component)
            val controllers = sessionManager?.getActiveSessions(component).orEmpty()
            attachToBestController(controllers)
        }
        startProgressTicker()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        progressJob?.cancel()
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refreshActiveSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refreshActiveSessions()
    }

    private fun refreshActiveSessions() {
        val component = ComponentName(this, CarMediaNotificationListenerService::class.java)
        val controllers = runCatching { sessionManager?.getActiveSessions(component) }.getOrNull().orEmpty()
        attachToBestController(controllers)
    }

    private fun attachToBestController(controllers: List<MediaController>?) {
        val list = controllers.orEmpty()
        val chosen = list.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: list.firstOrNull()

        if (chosen?.sessionToken != activeController?.sessionToken) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = chosen
            activeController?.registerCallback(controllerCallback)
        }
        activeSessionRef = activeController
        publishCurrentState()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                if (_mediaState.value.isPlaying) {
                    publishCurrentState()
                }
            }
        }
    }

    private fun publishCurrentState() {
        val controller = activeController
        if (controller == null) {
            _mediaState.value = MediaPlaybackState(
                hasNotificationPermission = isNotificationAccessEnabled(this),
                hasActiveSession = false
            )
            return
        }

        val metadata = controller.metadata
        val playback = controller.playbackState
        val pkg = controller.packageName
        val appName = runCatching {
            val ai = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(ai).toString()
        }.getOrDefault(pkg)

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty()
        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val rawPos = playback?.position ?: 0L
        val isPlaying = playback?.state == PlaybackState.STATE_PLAYING

        val computedPos = if (isPlaying && playback != null && playback.lastPositionUpdateTime > 0L) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime
            (rawPos + (elapsed * playback.playbackSpeed).toLong()).coerceAtLeast(0L)
        } else {
            rawPos.coerceAtLeast(0L)
        }

        _mediaState.value = MediaPlaybackState(
            hasNotificationPermission = true,
            hasActiveSession = title.isNotBlank() || playback != null,
            isPlaying = isPlaying,
            title = title,
            artist = artist,
            album = album,
            sourcePackage = pkg,
            sourceAppName = appName,
            albumArt = albumArt,
            positionMs = if (duration > 0L) computedPos.coerceAtMost(duration) else computedPos,
            durationMs = duration
        )
    }

    companion object {
        private val _mediaState = MutableStateFlow(MediaPlaybackState())
        val mediaState: StateFlow<MediaPlaybackState> = _mediaState.asStateFlow()

        @Volatile
        private var activeSessionRef: MediaController? = null

        fun refreshPermissionStatus(context: Context) {
            val granted = isNotificationAccessEnabled(context)
            _mediaState.value = _mediaState.value.copy(hasNotificationPermission = granted)
            if (granted) {
                runCatching {
                    val sm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                    val cn = ComponentName(context, CarMediaNotificationListenerService::class.java)
                    val sessions = sm?.getActiveSessions(cn).orEmpty()
                    val best = sessions.firstOrNull {
                        it.playbackState?.state == PlaybackState.STATE_PLAYING
                    } ?: sessions.firstOrNull()
                    activeSessionRef = best
                    if (best != null) {
                        val md = best.metadata
                        val pb = best.playbackState
                        val pkg = best.packageName
                        val appLabel = runCatching {
                            val ai = context.packageManager.getApplicationInfo(pkg, 0)
                            context.packageManager.getApplicationLabel(ai).toString()
                        }.getOrDefault(pkg)
                        _mediaState.value = MediaPlaybackState(
                            hasNotificationPermission = true,
                            hasActiveSession = md != null || pb != null,
                            isPlaying = pb?.state == PlaybackState.STATE_PLAYING,
                            title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                            artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                            album = md?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
                            sourcePackage = pkg,
                            sourceAppName = appLabel,
                            albumArt = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                                ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                            positionMs = (pb?.position ?: 0L).coerceAtLeast(0L),
                            durationMs = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                        )
                    }
                }
            }
        }

        fun isNotificationAccessEnabled(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val target = ComponentName(context, CarMediaNotificationListenerService::class.java)
            return enabledListeners.contains(target.flattenToString()) ||
                enabledListeners.contains(target.flattenToShortString())
        }

        fun togglePlayPause() {
            val controller = activeSessionRef ?: return
            val state = controller.playbackState?.state
            if (state == PlaybackState.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }

        fun skipNext() {
            activeSessionRef?.transportControls?.skipToNext()
        }

        fun skipPrevious() {
            activeSessionRef?.transportControls?.skipToPrevious()
        }

        fun openActiveMediaApp(context: Context) {
            val controller = activeSessionRef
            if (controller?.sessionActivity != null) {
                runCatching { controller.sessionActivity?.send() }
                return
            }
            val pkg = controller?.packageName ?: return
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent != null) {
                runCatching { context.startActivity(intent) }
            }
        }
    }
}
