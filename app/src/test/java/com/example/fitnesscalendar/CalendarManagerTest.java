package com.example.fitnesscalendar;

import com.example.fitnesscalendar.logic.calendar.CalendarManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class CalendarManagerTest {
    private CalendarManager calendarManager;
    private final long MILLIS_IN_DAY = 24 * 60 * 60 * 1000L;

    @Before
    public void setup() {
        // force UTC to avoid TimeZone shifts
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        calendarManager = new CalendarManager();
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2026, Calendar.APRIL, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        calendarManager.setCurrentDate(cal.getTime());
    }


    @Test
    public void testDaysInMonth() {
        // April 2026 has 30 days.
        // April 1st, 2026 - Wednesday.
        // Monday Start Logic: M="", Tu="", W="1" -> 2 empty slots for M and Tu
        List<String> days = calendarManager.getDaysOfMonthList();

        Assert.assertEquals("", days.get(0)); // M
        Assert.assertEquals("", days.get(1)); // Tu
        Assert.assertEquals("1", days.get(2)); // W

        Assert.assertTrue(days.contains("1"));
        Assert.assertTrue(days.contains("30"));
        Assert.assertEquals(32, days.size()); // 2 empty + 30 days
    }

    @Test
    public void testMonthNavigation() {
        calendarManager.goToNextMonth();
        Assert.assertTrue(calendarManager.getHeaderString().contains("May 2026"));

        calendarManager.goToPrevMonth();
        Assert.assertTrue(calendarManager.getHeaderString().contains("April 2026"));
    }

    @Test
    public void testGetHeaderStringForPastMonth() {
        Assert.assertEquals("April 2026", calendarManager.getHeaderString());
    }

    @Test
    public void testGetEpochDayForDay() {
        // Testing for April 20th, 2026
        String dayText = "20";
        Long result = calendarManager.getEpochDayForDay(dayText);

        Calendar expectedCal = Calendar.getInstance();
        expectedCal.set(2026, Calendar.APRIL, 20);
        long expectedEpoch = expectedCal.getTimeInMillis() / MILLIS_IN_DAY;

        Assert.assertNotNull(result);
        Assert.assertEquals(expectedEpoch, (long) result);
    }

    @Test
    public void testGetEpochDayForDay_EmptyString_ReturnsNull() {
        Assert.assertNull(calendarManager.getEpochDayForDay(""));
    }


    @Test
    public void testGetStartOfMonthEpochDay() {
        long startEpoch = calendarManager.getStartOfMonthEpochDay();

        Calendar expectedCal = Calendar.getInstance();
        expectedCal.set(2026, Calendar.APRIL, 1, 0, 0, 0);
        expectedCal.set(Calendar.MILLISECOND, 0);
        long expectedValue = expectedCal.getTimeInMillis() / MILLIS_IN_DAY;

        Assert.assertEquals(expectedValue, startEpoch);
    }

    @Test
    public void testGetEndOfMonthEpochDay() {
        long endEpoch = calendarManager.getEndOfMonthEpochDay();

        Calendar expectedCal = Calendar.getInstance();
        expectedCal.set(2026, Calendar.APRIL, 30, 23, 59, 59);
        long expectedValue = expectedCal.getTimeInMillis() / MILLIS_IN_DAY;

        Assert.assertEquals(expectedValue, endEpoch);
    }

    @Test
    public void testMonthBoundaryRange() {
        // April has exactly 30 days?
        long start = calendarManager.getStartOfMonthEpochDay();
        long end = calendarManager.getEndOfMonthEpochDay();

        Assert.assertEquals(29, end - start);
    }
}
