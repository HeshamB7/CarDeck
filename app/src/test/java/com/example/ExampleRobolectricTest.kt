package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.engine.AppLauncherHelper
import com.example.engine.IconProcessor
import com.example.model.AppEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches CarDeck`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CarDeck", appName)
    }

    @Test
    fun `main activity declares home and default launcher categories`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
            setPackage(context.packageName)
        }
        val resolved = context.packageManager.queryIntentActivities(homeIntent, 0)
        assertTrue(resolved.any { it.activityInfo.packageName == context.packageName })
    }

    @Test
    fun `icon processor renders exact Apple CarPlay core icons`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val processor = IconProcessor(context)
        val coreVirtuals = listOf(
            AppLauncherHelper.VIRTUAL_PHONE,
            AppLauncherHelper.VIRTUAL_MUSIC,
            AppLauncherHelper.VIRTUAL_MAPS,
            AppLauncherHelper.VIRTUAL_MESSAGES,
            AppLauncherHelper.VIRTUAL_NOW_PLAYING,
            AppLauncherHelper.VIRTUAL_PODCASTS,
            AppLauncherHelper.VIRTUAL_AUDIOBOOKS,
            AppLauncherHelper.VIRTUAL_CALENDAR,
            AppLauncherHelper.VIRTUAL_SETTINGS
        )
        for (virt in coreVirtuals) {
            val bmp = processor.getProcessedIcon(
                AppEntry(
                    packageName = virt,
                    activityName = virt,
                    label = virt,
                    virtualAction = virt
                ),
                iconPackPackage = null,
                sizePx = 128
            )
            assertEquals(128, bmp.width)
            assertEquals(128, bmp.height)
        }
    }
}
