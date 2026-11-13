package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.AppEntry
import com.example.model.IconPackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/**
 * Apple CarPlay iOS 18+ Icon Engine:
 * - Renders exact Apple CarPlay native icons for core apps:
 *   Phone, Music, Maps, Messages, Now Playing, Podcasts, Audiobooks, Calendar, Settings
 * - Strips weird transparent borders/margins from any installed third-party Android app icon,
 *   normalizes size uniformly, synthesizes clean opaque gradient backgrounds where needed,
 *   and clips every icon inside Apple's 22.5% continuous Superellipse (Squircle).
 * - Supports external Icon Packs via appfilter.xml.
 */
class IconProcessor(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(200)
    private val iconPackMapCache = ConcurrentHashMap<String, Map<String, String>>()
    private val squirclePathCache = ConcurrentHashMap<Int, Path>()
    private val glossOverlayCache = ConcurrentHashMap<Int, Bitmap>()
    private val dayKeyFormat = ThreadLocal.withInitial { SimpleDateFormat("yyyyMMdd", Locale.US) }

    fun clearCache() {
        iconCache.evictAll()
    }

    fun observePackageChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                clearCache()
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        context.registerReceiver(receiver, filter)
        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    suspend fun loadInstalledApps(
        hiddenPackages: Set<String>,
        customLabels: Map<String, String>,
        savedOrder: List<String>
    ): List<AppEntry> = withContext(Dispatchers.IO) {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val ownPackage = context.packageName
        val orderLookup = savedOrder.withIndex().associate { it.value to it.index }

        // Exact Apple CarPlay iOS 18 Core Home Screen Apps (5x2 Page 1 order matching reference)
        val appleCoreEntries = listOf(
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_PHONE,
                activityName = "Phone",
                label = "Phone",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_PHONE],
                isHidden = AppLauncherHelper.VIRTUAL_PHONE in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_PHONE] ?: 0,
                virtualAction = AppLauncherHelper.VIRTUAL_PHONE
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_MUSIC,
                activityName = "Music",
                label = "Music",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_MUSIC],
                isHidden = AppLauncherHelper.VIRTUAL_MUSIC in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_MUSIC] ?: 1,
                virtualAction = AppLauncherHelper.VIRTUAL_MUSIC
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_MAPS,
                activityName = "Maps",
                label = "Maps",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_MAPS],
                isHidden = AppLauncherHelper.VIRTUAL_MAPS in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_MAPS] ?: 2,
                virtualAction = AppLauncherHelper.VIRTUAL_MAPS
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_MESSAGES,
                activityName = "Messages",
                label = "Messages",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_MESSAGES],
                isHidden = AppLauncherHelper.VIRTUAL_MESSAGES in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_MESSAGES] ?: 3,
                virtualAction = AppLauncherHelper.VIRTUAL_MESSAGES
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_NOW_PLAYING,
                activityName = "NowPlaying",
                label = "Now Playing",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_NOW_PLAYING],
                isHidden = AppLauncherHelper.VIRTUAL_NOW_PLAYING in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_NOW_PLAYING] ?: 4,
                virtualAction = AppLauncherHelper.VIRTUAL_NOW_PLAYING
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_PODCASTS,
                activityName = "Podcasts",
                label = "Podcasts",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_PODCASTS],
                isHidden = AppLauncherHelper.VIRTUAL_PODCASTS in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_PODCASTS] ?: 5,
                virtualAction = AppLauncherHelper.VIRTUAL_PODCASTS
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_AUDIOBOOKS,
                activityName = "Audiobooks",
                label = "Audiobooks",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_AUDIOBOOKS],
                isHidden = AppLauncherHelper.VIRTUAL_AUDIOBOOKS in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_AUDIOBOOKS] ?: 6,
                virtualAction = AppLauncherHelper.VIRTUAL_AUDIOBOOKS
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_CALENDAR,
                activityName = "Calendar",
                label = "Calendar",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_CALENDAR],
                isHidden = AppLauncherHelper.VIRTUAL_CALENDAR in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_CALENDAR] ?: 7,
                virtualAction = AppLauncherHelper.VIRTUAL_CALENDAR
            ),
            AppEntry(
                packageName = AppLauncherHelper.VIRTUAL_SETTINGS,
                activityName = "Settings",
                label = "Settings",
                customLabel = customLabels[AppLauncherHelper.VIRTUAL_SETTINGS],
                isHidden = AppLauncherHelper.VIRTUAL_SETTINGS in hiddenPackages,
                sortOrder = orderLookup[AppLauncherHelper.VIRTUAL_SETTINGS] ?: 8,
                virtualAction = AppLauncherHelper.VIRTUAL_SETTINGS
            )
        )

        val installedEntries = resolveInfos
            .asSequence()
            .filter { it.activityInfo?.packageName != null && it.activityInfo.packageName != ownPackage }
            .distinctBy { it.activityInfo.packageName }
            .mapIndexed { idx, info ->
                val pkg = info.activityInfo.packageName
                val act = info.activityInfo.name
                val rawLabel = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                AppEntry(
                    packageName = pkg,
                    activityName = act,
                    label = rawLabel,
                    customLabel = customLabels[pkg],
                    isHidden = pkg in hiddenPackages,
                    sortOrder = orderLookup[pkg] ?: (100 + idx),
                    virtualAction = null
                )
            }
            .toList()

        (appleCoreEntries + installedEntries).sortedWith(
            compareBy<AppEntry> { it.sortOrder }
                .thenBy { it.displayName.lowercase(Locale.US) }
        )
    }

    suspend fun discoverIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val themeActions = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.anddoes.launcher.THEME"
        )
        val packs = linkedMapOf<String, IconPackInfo>()
        for (action in themeActions) {
            val intent = Intent(action)
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            for (ri in resolved) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (!packs.containsKey(pkg)) {
                    val label = ri.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
                    packs[pkg] = IconPackInfo(pkg, label)
                }
            }
        }
        packs.values.sortedBy { it.label.lowercase(Locale.US) }
    }

    suspend fun getProcessedIcon(
        app: AppEntry,
        iconPackPackage: String?,
        sizePx: Int = 192
    ): ImageBitmap = withContext(Dispatchers.IO) {
        val dayKey = if (app.packageName == AppLauncherHelper.VIRTUAL_CALENDAR ||
            app.packageName.contains("calendar", ignoreCase = true)
        ) {
            dayKeyFormat.get()?.format(Date()).orEmpty()
        } else ""

        val cacheKey = "${app.packageName}|${iconPackPackage ?: "apple_carplay"}|$sizePx|$dayKey"
        iconCache.get(cacheKey)?.let { return@withContext it }

        val customDrawable = if (app.virtualAction == null && iconPackPackage != null) {
            loadFromIconPack(iconPackPackage, app)
        } else null

        val appleKind = detectAppleIconKind(app)
        val processedBitmap = if (customDrawable == null && appleKind != null) {
            renderExactAppleCarPlayIcon(appleKind, sizePx)
        } else {
            val sourceDrawable = customDrawable ?: runCatching {
                pm.getApplicationIcon(app.packageName)
            }.getOrNull() ?: pm.defaultActivityIcon
            renderAppleSquircleBitmap(sourceDrawable, sizePx)
        }

        val imageBitmap = processedBitmap.asImageBitmap()
        iconCache.put(cacheKey, imageBitmap)
        imageBitmap
    }

    private enum class AppleCoreKind {
        PHONE,
        MUSIC,
        MAPS,
        MESSAGES,
        NOW_PLAYING,
        PODCASTS,
        AUDIOBOOKS,
        CALENDAR,
        SETTINGS
    }

    private fun detectAppleIconKind(app: AppEntry): AppleCoreKind? {
        return when (app.virtualAction) {
            AppLauncherHelper.VIRTUAL_PHONE -> AppleCoreKind.PHONE
            AppLauncherHelper.VIRTUAL_MUSIC -> AppleCoreKind.MUSIC
            AppLauncherHelper.VIRTUAL_MAPS -> AppleCoreKind.MAPS
            AppLauncherHelper.VIRTUAL_MESSAGES -> AppleCoreKind.MESSAGES
            AppLauncherHelper.VIRTUAL_NOW_PLAYING -> AppleCoreKind.NOW_PLAYING
            AppLauncherHelper.VIRTUAL_PODCASTS -> AppleCoreKind.PODCASTS
            AppLauncherHelper.VIRTUAL_AUDIOBOOKS -> AppleCoreKind.AUDIOBOOKS
            AppLauncherHelper.VIRTUAL_CALENDAR -> AppleCoreKind.CALENDAR
            AppLauncherHelper.VIRTUAL_SETTINGS -> AppleCoreKind.SETTINGS
            else -> {
                val pkg = app.packageName.lowercase(Locale.US)
                val lbl = app.label.lowercase(Locale.US)
                when {
                    pkg.contains("dialer") || pkg.contains("telecom") || lbl == "phone" -> AppleCoreKind.PHONE
                    pkg.contains("messaging") || pkg.contains("mms") || lbl == "messages" -> AppleCoreKind.MESSAGES
                    pkg == "com.google.android.apps.maps" -> AppleCoreKind.MAPS
                    pkg.contains("calendar") -> AppleCoreKind.CALENDAR
                    pkg == "com.android.settings" -> AppleCoreKind.SETTINGS
                    pkg.contains("podcasts") -> AppleCoreKind.PODCASTS
                    pkg.contains("audible") -> AppleCoreKind.AUDIOBOOKS
                    else -> null
                }
            }
        }
    }

    private fun renderExactAppleCarPlayIcon(kind: AppleCoreKind, sizePx: Int): Bitmap {
        val raw = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(raw)
        val s = sizePx.toFloat()

        when (kind) {
            AppleCoreKind.PHONE -> {
                drawVerticalGradient(c, s, Color.rgb(103, 237, 117), Color.rgb(26, 201, 54))
                drawAppleHandset(c, s)
            }
            AppleCoreKind.MESSAGES -> {
                drawVerticalGradient(c, s, Color.rgb(103, 237, 117), Color.rgb(26, 201, 54))
                drawAppleSpeechBubble(c, s)
            }
            AppleCoreKind.MUSIC -> {
                drawVerticalGradient(c, s, Color.rgb(250, 92, 114), Color.rgb(250, 35, 59))
                drawAppleMusicNotes(c, s)
            }
            AppleCoreKind.NOW_PLAYING -> {
                c.drawColor(Color.WHITE)
                drawAppleNowPlayingGlyph(c, s)
            }
            AppleCoreKind.PODCASTS -> {
                drawVerticalGradient(c, s, Color.rgb(200, 101, 250), Color.rgb(123, 34, 226))
                drawApplePodcastsGlyph(c, s)
            }
            AppleCoreKind.AUDIOBOOKS -> {
                drawVerticalGradient(c, s, Color.rgb(255, 182, 56), Color.rgb(255, 106, 0))
                drawAppleAudiobooksGlyph(c, s)
            }
            AppleCoreKind.MAPS -> {
                drawAppleMapsIcon(c, s)
            }
            AppleCoreKind.CALENDAR -> {
                drawAppleCalendarIcon(c, s)
            }
            AppleCoreKind.SETTINGS -> {
                drawAppleSettingsIcon(c, s)
            }
        }

        return clipAndGlossSquircle(raw, sizePx)
    }

    private fun drawVerticalGradient(c: Canvas, s: Float, top: Int, bottom: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, s, top, bottom, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, s, s, p)
    }

    private fun drawAppleHandset(c: Canvas, s: Float) {
        c.save()
        c.translate(s * 0.50f, s * 0.50f)
        c.rotate(-42f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val path = Path().apply {
            moveTo(-s * 0.25f, -s * 0.08f)
            cubicTo(-s * 0.27f, -s * 0.20f, -s * 0.14f, -s * 0.22f, -s * 0.10f, -s * 0.14f)
            lineTo(-s * 0.08f, -s * 0.06f)
            cubicTo(-s * 0.07f, -s * 0.02f, -s * 0.12f, 0.00f, -s * 0.10f, s * 0.03f)
            cubicTo(-s * 0.04f, s * 0.08f, s * 0.04f, s * 0.08f, s * 0.10f, s * 0.03f)
            cubicTo(s * 0.12f, 0.00f, s * 0.07f, -s * 0.02f, s * 0.08f, -s * 0.06f)
            lineTo(s * 0.10f, -s * 0.14f)
            cubicTo(s * 0.14f, -s * 0.22f, s * 0.27f, -s * 0.20f, s * 0.25f, -s * 0.08f)
            cubicTo(s * 0.22f, s * 0.14f, -s * 0.22f, s * 0.14f, -s * 0.25f, -s * 0.08f)
            close()
        }
        c.drawPath(path, p)
        c.restore()
    }

    private fun drawAppleSpeechBubble(c: Canvas, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        c.drawOval(RectF(s * 0.18f, s * 0.22f, s * 0.82f, s * 0.72f), p)
        val tail = Path().apply {
            moveTo(s * 0.28f, s * 0.63f)
            lineTo(s * 0.21f, s * 0.79f)
            quadTo(s * 0.34f, s * 0.74f, s * 0.42f, s * 0.68f)
            close()
        }
        c.drawPath(tail, p)
    }

    private fun drawAppleMusicNotes(c: Canvas, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        c.save()
        c.rotate(-16f, s * 0.32f, s * 0.68f)
        c.drawOval(RectF(s * 0.22f, s * 0.61f, s * 0.40f, s * 0.75f), p)
        c.restore()

        c.save()
        c.rotate(-16f, s * 0.65f, s * 0.62f)
        c.drawOval(RectF(s * 0.55f, s * 0.55f, s * 0.73f, s * 0.69f), p)
        c.restore()

        val stemsAndBeam = Path().apply {
            moveTo(s * 0.35f, s * 0.66f)
            lineTo(s * 0.35f, s * 0.28f)
            lineTo(s * 0.72f, s * 0.20f)
            lineTo(s * 0.72f, s * 0.60f)
            lineTo(s * 0.66f, s * 0.60f)
            lineTo(s * 0.66f, s * 0.33f)
            lineTo(s * 0.41f, s * 0.39f)
            lineTo(s * 0.41f, s * 0.66f)
            close()
        }
        c.drawPath(stemsAndBeam, p)
    }

    private fun drawAppleNowPlayingGlyph(c: Canvas, s: Float) {
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, s * 0.2f, 0f, s * 0.8f,
                Color.rgb(255, 55, 95),
                Color.rgb(250, 35, 59),
                Shader.TileMode.CLAMP
            )
        }
        val barWidth = s * 0.085f
        val heights = listOf(0.24f, 0.42f, 0.60f, 0.38f, 0.22f)
        val xCenters = listOf(0.24f, 0.37f, 0.50f, 0.63f, 0.76f)
        heights.forEachIndexed { idx, hFrac ->
            val bh = s * hFrac
            val cx = s * xCenters[idx]
            val rect = RectF(cx - barWidth / 2f, (s - bh) / 2f, cx + barWidth / 2f, (s + bh) / 2f)
            c.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }

    private fun drawApplePodcastsGlyph(c: Canvas, s: Float) {
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = s * 0.048f
        }
        c.drawArc(RectF(s * 0.20f, s * 0.18f, s * 0.80f, s * 0.78f), 140f, 260f, false, arcPaint)
        c.drawArc(RectF(s * 0.31f, s * 0.29f, s * 0.69f, s * 0.67f), 145f, 250f, false, arcPaint)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        c.drawCircle(s * 0.50f, s * 0.47f, s * 0.075f, fill)
        c.drawRoundRect(RectF(s * 0.435f, s * 0.57f, s * 0.565f, s * 0.79f), s * 0.06f, s * 0.06f, fill)
    }

    private fun drawAppleAudiobooksGlyph(c: Canvas, s: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val leftPage = Path().apply {
            moveTo(s * 0.485f, s * 0.31f)
            quadTo(s * 0.34f, s * 0.26f, s * 0.20f, s * 0.31f)
            lineTo(s * 0.20f, s * 0.71f)
            quadTo(s * 0.34f, s * 0.66f, s * 0.485f, s * 0.72f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(s * 0.515f, s * 0.31f)
            quadTo(s * 0.66f, s * 0.26f, s * 0.80f, s * 0.31f)
            lineTo(s * 0.80f, s * 0.71f)
            quadTo(s * 0.66f, s * 0.66f, s * 0.515f, s * 0.72f)
            close()
        }
        c.drawPath(leftPage, p)
        c.drawPath(rightPage, p)
    }

    private fun drawAppleMapsIcon(c: Canvas, s: Float) {
        c.drawColor(Color.rgb(244, 241, 232))
        val parkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(114, 215, 109) }
        c.drawRect(0f, 0f, s * 0.44f, s * 0.52f, parkPaint)
        val pinkTint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(252, 206, 214) }
        c.drawRect(s * 0.56f, s * 0.44f, s, s, pinkTint)

        val yellowRoad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 204, 0)
            strokeWidth = s * 0.11f
        }
        c.drawLine(0f, s * 0.52f, s, s * 0.39f, yellowRoad)

        val roadCasing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeWidth = s * 0.22f
            strokeCap = Paint.Cap.ROUND
        }
        val roadBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 132, 255)
            strokeWidth = s * 0.14f
            strokeCap = Paint.Cap.ROUND
        }
        c.drawLine(s * 0.47f, s, s * 0.52f, 0f, roadCasing)
        c.drawLine(s * 0.47f, s, s * 0.52f, 0f, roadBlue)

        val badgeWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val badgeBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 122, 255) }
        val cx = s * 0.48f
        val cy = s * 0.62f
        c.drawCircle(cx, cy, s * 0.24f, badgeWhite)
        c.drawCircle(cx, cy, s * 0.20f, badgeBlue)

        val navArrow = Path().apply {
            moveTo(cx, cy - s * 0.11f)
            lineTo(cx + s * 0.09f, cy + s * 0.09f)
            lineTo(cx, cy + s * 0.04f)
            lineTo(cx - s * 0.09f, cy + s * 0.09f)
            close()
        }
        c.drawPath(navArrow, badgeWhite)

        val shieldRed = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 59, 48) }
        val shieldBlue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 122, 255) }
        c.drawRoundRect(RectF(s * 0.64f, s * 0.10f, s * 0.90f, s * 0.34f), s * 0.06f, s * 0.06f, badgeWhite)
        c.drawRoundRect(RectF(s * 0.66f, s * 0.12f, s * 0.88f, s * 0.19f), s * 0.03f, s * 0.03f, shieldRed)
        c.drawRoundRect(RectF(s * 0.66f, s * 0.18f, s * 0.88f, s * 0.32f), s * 0.03f, s * 0.03f, shieldBlue)
    }

    private fun drawAppleCalendarIcon(c: Canvas, s: Float) {
        c.drawColor(Color.WHITE)
        val now = Date()
        val dayName = SimpleDateFormat("EEE", Locale.US).format(now).uppercase(Locale.US)
        val dayNum = SimpleDateFormat("d", Locale.US).format(now)

        val headerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 59, 48)
            textAlign = Paint.Align.CENTER
            textSize = s * 0.17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText(dayName, s * 0.5f, s * 0.27f, headerText)

        val numText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 28, 30)
            textAlign = Paint.Align.CENTER
            textSize = s * 0.52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val numBounds = Rect()
        numText.getTextBounds(dayNum, 0, dayNum.length, numBounds)
        c.drawText(dayNum, s * 0.5f, s * 0.64f + numBounds.height() / 2f, numText)
    }

    private fun drawAppleSettingsIcon(c: Canvas, s: Float) {
        drawVerticalGradient(c, s, Color.rgb(162, 164, 172), Color.rgb(96, 98, 106))

        val cx = s * 0.5f
        val cy = s * 0.5f
        val darkGear = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(43, 43, 46) }
        val lightMetal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(218, 220, 226) }

        c.drawCircle(cx, cy, s * 0.40f, lightMetal)
        c.drawCircle(cx, cy, s * 0.34f, darkGear)

        val toothPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(229, 230, 236) }
        for (i in 0 until 24) {
            c.save()
            c.rotate(i * 15f, cx, cy)
            c.drawRoundRect(
                RectF(cx - s * 0.025f, cy - s * 0.43f, cx + s * 0.025f, cy - s * 0.33f),
                s * 0.015f, s * 0.015f, toothPaint
            )
            c.restore()
        }

        c.drawCircle(cx, cy, s * 0.25f, lightMetal)
        c.drawCircle(cx, cy, s * 0.18f, darkGear)
        for (i in 0 until 3) {
            c.save()
            c.rotate(i * 120f, cx, cy)
            c.drawRoundRect(
                RectF(cx - s * 0.032f, cy - s * 0.26f, cx + s * 0.032f, cy),
                s * 0.02f, s * 0.02f, lightMetal
            )
            c.restore()
        }
        c.drawCircle(cx, cy, s * 0.11f, lightMetal)
        c.drawCircle(cx, cy, s * 0.06f, darkGear)
    }

    private fun loadFromIconPack(iconPackPkg: String, app: AppEntry): Drawable? {
        return runCatching {
            val packRes = pm.getResourcesForApplication(iconPackPkg)
            val map = iconPackMapCache.getOrPut(iconPackPkg) {
                parseAppFilter(packRes, iconPackPkg)
            }
            val componentKey = "ComponentInfo{${app.packageName}/${app.activityName}}"
            val drawableName = map[componentKey] ?: map[app.packageName] ?: return null
            val resId = packRes.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) packRes.getDrawable(resId, null) else null
        }.getOrNull()
    }

    private fun parseAppFilter(res: Resources, packPkg: String): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        val xmlId = res.getIdentifier("appfilter", "xml", packPkg)
        if (xmlId != 0) {
            runCatching {
                val parser = res.getXml(xmlId)
                readAppFilterFromParser(parser, mapping)
            }
        } else {
            runCatching {
                res.assets.open("appfilter.xml").use { stream ->
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(stream, "UTF-8")
                    readAppFilterFromParser(parser, mapping)
                }
            }
        }
        return mapping
    }

    private fun readAppFilterFromParser(parser: XmlPullParser, mapping: MutableMap<String, String>) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                    mapping[component] = drawable
                    val inside = component.removePrefix("ComponentInfo{").removeSuffix("}")
                    val pkg = inside.substringBefore('/')
                    if (pkg.isNotBlank() && !mapping.containsKey(pkg)) {
                        mapping[pkg] = drawable
                    }
                }
            }
            event = parser.next()
        }
    }

    private fun renderAppleSquircleBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        val contentBitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val contentCanvas = Canvas(contentBitmap)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            val bg = drawable.background
            val fg = drawable.foreground
            // Full-bleed crop of adaptive background so zero transparent edge leaks remain
            val bgPad = (sizePx * -0.22f).toInt()
            val bgBounds = Rect(bgPad, bgPad, sizePx - bgPad, sizePx - bgPad)
            if (bg != null) {
                bg.bounds = bgBounds
                bg.draw(contentCanvas)
                // Verify background actually filled corners; if transparent, paint soft gradient first
                if (measureCornerTransparencyRatio(contentBitmap) > 0.15f) {
                    val tempFg = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                    val fgCanvas = Canvas(tempFg)
                    fg?.let {
                        it.bounds = bgBounds
                        it.draw(fgCanvas)
                    }
                    val accent = sampleDominantColor(tempFg)
                    contentCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    drawSoftGradientBackdrop(contentCanvas, sizePx, accent)
                    bg.draw(contentCanvas)
                    tempFg.recycle()
                }
            } else {
                drawSoftGradientBackdrop(contentCanvas, sizePx, Color.rgb(34, 118, 255))
            }
            if (fg != null) {
                val fgPad = (sizePx * -0.17f).toInt()
                fg.bounds = Rect(fgPad, fgPad, sizePx - fgPad, sizePx - fgPad)
                fg.draw(contentCanvas)
            }
        } else {
            val rawBitmap = drawableToBitmap(drawable, sizePx)
            val rw = rawBitmap.width
            val rh = rawBitmap.height
            val pixels = IntArray(rw * rh)
            rawBitmap.getPixels(pixels, 0, rw, 0, 0, rw, rh)
            val transparencyRatio = measureOuterTransparencyRatio(pixels, rw, rh)
            if (transparencyRatio > 0.08f) {
                // Trim weird transparent padding around legacy Android icons and center uniformly
                val opaqueBounds = findOpaqueContentBounds(pixels, rw, rh)
                val accent = sampleDominantColor(pixels, rw, rh)
                drawSoftGradientBackdrop(contentCanvas, sizePx, accent)
                val inset = (sizePx * 0.16f).toInt()
                val dstRect = Rect(inset, inset, sizePx - inset, sizePx - inset)
                contentCanvas.drawBitmap(
                    rawBitmap,
                    opaqueBounds,
                    dstRect,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
            } else {
                contentCanvas.drawBitmap(
                    rawBitmap,
                    0f,
                    0f,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
            }
        }

        return clipAndGlossSquircle(contentBitmap, sizePx)
    }

    private fun getCachedSquirclePath(sizePx: Int): Path {
        return squirclePathCache.getOrPut(sizePx) {
            createSuperellipsePath(sizePx.toFloat(), sizePx.toFloat(), exponent = 4.65f)
        }
    }

    private fun getCachedGlossOverlay(sizePx: Int, squirclePath: Path): Bitmap {
        return glossOverlayCache.getOrPut(sizePx) {
            val overlay = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val c = Canvas(overlay)

            val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (sizePx * 0.016f).coerceAtLeast(1.6f)
                color = Color.argb(34, 0, 0, 0)
            }
            c.drawPath(squirclePath, innerShadowPaint)

            val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, sizePx * 0.30f,
                    intArrayOf(
                        Color.argb(62, 255, 255, 255),
                        Color.argb(14, 255, 255, 255),
                        Color.argb(0, 255, 255, 255)
                    ),
                    floatArrayOf(0f, 0.35f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawPath(squirclePath, glossPaint)

            val rimHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = (sizePx * 0.015f).coerceAtLeast(1.5f)
                shader = LinearGradient(
                    0f, 0f, 0f, sizePx * 0.42f,
                    Color.argb(128, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
            }
            c.drawPath(squirclePath, rimHighlightPaint)
            overlay
        }
    }

    private fun clipAndGlossSquircle(contentBitmap: Bitmap, sizePx: Int): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val squirclePath = getCachedSquirclePath(sizePx)

        val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(squirclePath, maskPaint)
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(contentBitmap, 0f, 0f, maskPaint)
        maskPaint.xfermode = null

        val cachedOverlay = getCachedGlossOverlay(sizePx, squirclePath)
        canvas.drawBitmap(cachedOverlay, 0f, 0f, null)

        contentBitmap.recycle()
        return output
    }

    private fun createSuperellipsePath(width: Float, height: Float, exponent: Float): Path {
        val path = Path()
        val rx = width / 2f - 0.5f
        val ry = height / 2f - 0.5f
        val cx = width / 2f
        val cy = height / 2f
        val exp = 2.0 / exponent.toDouble()
        val steps = 128

        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps
            val cosT = cos(theta)
            val sinT = sin(theta)
            val x = cx + (rx * sign(cosT) * abs(cosT).pow(exp)).toFloat()
            val y = cy + (ry * sign(sinT) * abs(sinT).pow(exp)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun drawableToBitmap(drawable: Drawable, sizePx: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val bmp = drawable.bitmap
            return if (bmp.width == sizePx && bmp.height == sizePx) bmp
            else Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true)
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }

    private fun findOpaqueContentBounds(pixels: IntArray, w: Int, h: Int): Rect {
        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1
        val step = (w / 48).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            val rowOffset = y * w
            var x = 0
            while (x < w) {
                if (Color.alpha(pixels[rowOffset + x]) > 36) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
                x += step
            }
            y += step
        }
        if (maxX <= minX || maxY <= minY) return Rect(0, 0, w, h)
        // Make bounding rect square so icon proportions stay undistorted
        val bw = maxX - minX
        val bh = maxY - minY
        val side = maxOf(bw, bh)
        val cx = (minX + maxX) / 2
        val cy = (minY + maxY) / 2
        val half = side / 2
        return Rect(
            (cx - half).coerceAtLeast(0),
            (cy - half).coerceAtLeast(0),
            (cx + half).coerceAtMost(w),
            (cy + half).coerceAtMost(h)
        )
    }

    private fun measureCornerTransparencyRatio(bitmap: Bitmap): Float {
        val w = bitmap.width
        val h = bitmap.height
        val points = listOf(
            2 to 2, w - 3 to 2, 2 to h - 3, w - 3 to h - 3,
            w / 6 to 2, (w * 5) / 6 to 2, w / 6 to h - 3, (w * 5) / 6 to h - 3
        )
        val transparent = points.count { (x, y) ->
            Color.alpha(bitmap.getPixel(x.coerceIn(0, w - 1), y.coerceIn(0, h - 1))) < 200
        }
        return transparent.toFloat() / points.size.toFloat()
    }

    private fun measureOuterTransparencyRatio(pixels: IntArray, w: Int, h: Int): Float {
        var transparentCount = 0
        var totalSamples = 0
        val stepX = (w / 14).coerceAtLeast(1)
        val stepY = (h / 14).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            val rowOffset = y * w
            var x = 0
            while (x < w) {
                totalSamples++
                if (Color.alpha(pixels[rowOffset + x]) < 40) transparentCount++
                x += stepX
            }
            y += stepY
        }
        return if (totalSamples == 0) 0f else transparentCount.toFloat() / totalSamples.toFloat()
    }

    private fun sampleDominantColor(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        return sampleDominantColor(pixels, w, h)
    }

    private fun sampleDominantColor(pixels: IntArray, w: Int, h: Int): Int {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L
        val stepX = (w / 12).coerceAtLeast(1)
        val stepY = (h / 12).coerceAtLeast(1)
        var y = 0
        while (y < h) {
            val rowOffset = y * w
            var x = 0
            while (x < w) {
                val pixel = pixels[rowOffset + x]
                if (Color.alpha(pixel) > 120) {
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val lum = (r + g + b) / 3
                    if (lum in 28..232) {
                        rSum += r
                        gSum += g
                        bSum += b
                        count++
                    }
                }
                x += stepX
            }
            y += stepY
        }
        return if (count > 0) {
            Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
        } else {
            Color.rgb(245, 247, 252)
        }
    }

    private fun drawSoftGradientBackdrop(canvas: Canvas, sizePx: Int, seedColor: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(seedColor, hsv)
        val topHsv = floatArrayOf(hsv[0], (hsv[1] * 0.25f).coerceIn(0.04f, 0.28f), 0.98f)
        val bottomHsv = floatArrayOf(hsv[0], (hsv[1] * 0.42f).coerceIn(0.08f, 0.45f), 0.86f)
        val topColor = Color.HSVToColor(topHsv)
        val bottomColor = Color.HSVToColor(bottomHsv)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, sizePx.toFloat(),
                topColor,
                bottomColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
    }
}
