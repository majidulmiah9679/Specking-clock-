package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClockPreferences
import com.example.model.ClockLanguage
import com.example.model.ClockSettings
import com.example.model.IntervalPreset
import com.example.model.ServiceStatus
import com.example.scheduler.AlarmScheduler
import com.example.tts.NaturalTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Speaking Clock", appName)
    }

    @Test
    fun `natural time formatter generates correct english phrasing`() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 15, 10, 15, 0)
        }
        val spoken = NaturalTimeFormatter.formatSpokenTime(
            calendar = cal,
            language = ClockLanguage.ENGLISH,
            is24Hour = false,
            announceAmPm = true
        )
        assertTrue("Expected 10 15 AM, got: $spoken", spoken.contains("ten fifteen AM"))
    }

    @Test
    fun `natural time formatter generates correct bengali phrasing`() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 15, 10, 15, 0)
        }
        val spoken = NaturalTimeFormatter.formatSpokenTime(
            calendar = cal,
            language = ClockLanguage.BENGALI,
            is24Hour = false,
            announceAmPm = true
        )
        assertTrue("Expected Bengali text, got: $spoken", spoken.contains("এখন সময়") && spoken.contains("দশটা"))
    }

    @Test
    fun `natural time formatter generates correct hindi phrasing`() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.OCTOBER, 15, 10, 15, 0)
        }
        val spoken = NaturalTimeFormatter.formatSpokenTime(
            calendar = cal,
            language = ClockLanguage.HINDI,
            is24Hour = false,
            announceAmPm = true
        )
        assertTrue("Expected Hindi text, got: $spoken", spoken.contains("अभी") && spoken.contains("दस बजकर"))
    }

    @Test
    fun `alarm scheduler calculates 15-minute aligned interval correctly`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 7)
            set(Calendar.SECOND, 0)
        }
        val settings = ClockSettings(intervalPreset = IntervalPreset.EVERY_15_MIN)
        val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings, fromTimeMillis = cal.timeInMillis)

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextTime }
        assertEquals(15, nextCal.get(Calendar.MINUTE))
        assertEquals(0, nextCal.get(Calendar.SECOND))
    }

    @Test
    fun `alarm scheduler calculates 30-minute aligned interval correctly`() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 0)
        }
        val settings = ClockSettings(intervalPreset = IntervalPreset.EVERY_30_MIN)
        val nextTime = AlarmScheduler.calculateNextAnnouncementTime(settings, fromTimeMillis = cal.timeInMillis)

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextTime }
        assertEquals(30, nextCal.get(Calendar.MINUTE))
        assertEquals(0, nextCal.get(Calendar.SECOND))
    }

    @Test
    fun `clock preferences persist and update settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = ClockPreferences.getInstance(context)

        prefs.updateLanguage(ClockLanguage.BENGALI)
        assertEquals(ClockLanguage.BENGALI, prefs.getSettings().language)

        prefs.updateInterval(IntervalPreset.CUSTOM, customMinutes = 45)
        assertEquals(IntervalPreset.CUSTOM, prefs.getSettings().intervalPreset)
        assertEquals(45, prefs.getSettings().customIntervalMinutes)

        prefs.updateStatus(ServiceStatus.RUNNING)
        assertEquals(ServiceStatus.RUNNING, prefs.getSettings().status)
    }
}
