package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import com.example.model.AppEntry
import com.example.model.NavigationProvider
import com.example.service.CarMediaNotificationListenerService

object AppLauncherHelper {

    const val VIRTUAL_PHONE = "carplay://phone"
    const val VIRTUAL_MUSIC = "carplay://music"
    const val VIRTUAL_MAPS = "carplay://maps"
    const val VIRTUAL_MESSAGES = "carplay://messages"
    const val VIRTUAL_NOW_PLAYING = "carplay://nowplaying"
    const val VIRTUAL_PODCASTS = "carplay://podcasts"
    const val VIRTUAL_AUDIOBOOKS = "carplay://audiobooks"
    const val VIRTUAL_CALENDAR = "carplay://calendar"
    const val VIRTUAL_SETTINGS = "carplay://settings"

    fun launchEntry(
        context: Context,
        app: AppEntry,
        navProvider: NavigationProvider,
        onOpenSettings: () -> Unit,
        onOpenDashboard: () -> Unit
    ): Boolean {
        return when (app.virtualAction) {
            VIRTUAL_SETTINGS -> {
                onOpenSettings()
                true
            }
            VIRTUAL_NOW_PLAYING -> {
                if (CarMediaNotificationListenerService.mediaState.value.hasActiveSession) {
                    CarMediaNotificationListenerService.openActiveMediaApp(context)
                } else {
                    onOpenDashboard()
                }
                true
            }
            VIRTUAL_PHONE -> {
                val dialer = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                runCatching { context.startActivity(dialer); true }.getOrDefault(false)
            }
            VIRTUAL_MESSAGES -> {
                val sms = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(sms); true }.getOrElse {
                    launchCategoryFallback(context, Intent(Intent.ACTION_VIEW, Uri.parse("sms:")))
                }
            }
            VIRTUAL_MAPS -> {
                openMapApp(context, navProvider)
                true
            }
            VIRTUAL_MUSIC -> {
                val music = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(music); true }.getOrElse {
                    if (CarMediaNotificationListenerService.mediaState.value.hasActiveSession) {
                        CarMediaNotificationListenerService.openActiveMediaApp(context)
                        true
                    } else {
                        launchApp(context, "com.spotify.music") ||
                            launchApp(context, "com.google.android.apps.youtube.music")
                    }
                }
            }
            VIRTUAL_CALENDAR -> {
                val cal = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching { context.startActivity(cal); true }.getOrDefault(false)
            }
            VIRTUAL_PODCASTS -> {
                launchApp(context, "com.google.android.apps.podcasts") ||
                    launchApp(context, "com.spotify.music") ||
                    launchCategoryFallback(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC))
            }
            VIRTUAL_AUDIOBOOKS -> {
                launchApp(context, "com.audible.application") ||
                    launchApp(context, "com.google.android.apps.books") ||
                    launchCategoryFallback(context, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC))
            }
            else -> launchApp(context, app.packageName)
        }
    }

    private fun launchCategoryFallback(context: Context, intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: return false
        return runCatching {
            context.startActivity(launchIntent)
            true
        }.getOrDefault(false)
    }

    fun launchSplitScreenPair(
        context: Context,
        primaryPackage: String,
        secondaryPackage: String
    ): Boolean {
        val primaryIntent = context.packageManager.getLaunchIntentForPackage(primaryPackage)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            } ?: return false
        val secondaryIntent = context.packageManager.getLaunchIntentForPackage(secondaryPackage)
            ?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
            } ?: return false

        return runCatching {
            context.startActivity(primaryIntent)
            context.startActivity(secondaryIntent)
            true
        }.getOrElse {
            Toast.makeText(context, "Split-screen multi-window is unavailable for these apps", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun launchNavigation(
        context: Context,
        query: String,
        provider: NavigationProvider
    ) {
        val encoded = Uri.encode(query)
        val uri = when (provider) {
            NavigationProvider.GOOGLE_MAPS -> Uri.parse("google.navigation:q=$encoded")
            NavigationProvider.WAZE -> Uri.parse("https://waze.com/ul?q=$encoded&navigate=yes")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(provider.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)

        if (!launched) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun openMapApp(context: Context, provider: NavigationProvider) {
        if (!launchApp(context, provider.packageName)) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback) }
        }
    }

    fun triggerVoiceAssistant(context: Context) {
        val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val started = runCatching {
            context.startActivity(voiceIntent)
            true
        }.getOrDefault(false)

        if (!started) {
            val webSearch = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(webSearch) }
        }
    }

    fun openBluetoothSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun openWifiPanel(context: Context) {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }
        runCatching {
            context.startActivity(Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }

    fun openHotspotSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun openDefaultHomeSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
