package com.mithaq.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Regression guard for the UTC-stale prayer-times bug (v2.1.7): adhan scheduling must derive
 * its DateComponents from the device's LOCAL calendar fields, not from a UTC conversion of a
 * Date. [AdhanScheduler.localDateComponents] is the canonical helper that fixed it.
 */
class AdhanLocalDateTest {

    @Test
    fun usesLocalCalendarFieldsWithOneBasedMonth() {
        // 5 June 2026, local time — month is 0-based in Calendar (5 = June).
        val cal = GregorianCalendar(2026, Calendar.JUNE, 5)

        val dc = AdhanScheduler.localDateComponents(cal)

        assertEquals(2026, dc.year)
        assertEquals(6, dc.month) // adhan DateComponents is 1-based
        assertEquals(5, dc.day)
    }

    @Test
    fun reflectsLocalDateNotUtcAcrossMidnightOffset() {
        // Just after local midnight in a positive-offset zone: the UTC instant is still the
        // PREVIOUS day. A correct local extraction must report the local day, not the UTC day.
        val tz = TimeZone.getTimeZone("Asia/Riyadh") // UTC+3, no DST
        val cal = GregorianCalendar(tz).apply {
            set(2026, Calendar.JUNE, 5, 1, 30, 0) // 01:30 local 5 Jun -> 22:30 UTC 4 Jun
            set(Calendar.MILLISECOND, 0)
        }

        val dc = AdhanScheduler.localDateComponents(cal)

        assertEquals(5, dc.day) // local day, not the UTC-rolled 4th
        assertEquals(6, dc.month)
        assertEquals(2026, dc.year)
    }
}
