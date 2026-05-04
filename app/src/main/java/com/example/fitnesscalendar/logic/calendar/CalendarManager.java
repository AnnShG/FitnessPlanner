package com.example.fitnesscalendar.logic.calendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * CalendarManager manages all calendar-related calculations.
 * It provided the Fragments with ready-to-use strings for the grid
 * and numeric IDs for the database.
 */
public class CalendarManager {
    private final Calendar currentDate = Calendar.getInstance();

    public void setCurrentDate(java.util.Date date) {
        this.currentDate.setTime(date);
    }

    public List<String> getDaysOfMonthList() {
        List<String> daysList = new ArrayList<>();

        // set a clone to the first day of the currently selected month
        Calendar cal = (Calendar) currentDate.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        // calculate leading empty slots
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 2;// Monday start
        if (firstDayOfWeek < 0) firstDayOfWeek = 6;

        // add empty strings for the previous month's trailing days
        for (int i = 0; i < firstDayOfWeek; i++) {
            daysList.add("");
        }

        // add the actual days of the current month
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            daysList.add(String.valueOf(i));
        }

        return daysList; // a list of 35-42 strings (empty slots & day numbers "1", "2"...)
    }

    public void goToNextMonth() {
        currentDate.add(Calendar.MONTH, 1);
    }

    public void goToPrevMonth() {
        currentDate.add(Calendar.MONTH, -1);
    }

    /**
     * Returns the header string (e.g., "April 2026").
     * If showToday is true, it includes the current day (e.g., "17 April 2026").
     */
    public String getHeaderString(boolean showToday) {
        Calendar today = Calendar.getInstance(); // Get the real-world today date

        // Compare the month and year of what is showed vs. today
        boolean isSameMonth = (currentDate.get(Calendar.MONTH) == today.get(Calendar.MONTH));
        boolean isSameYear = (currentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR));

        SimpleDateFormat sdf;

        if (showToday && isSameMonth && isSameYear) {
            // If opened the current month, show the full date (e.g., 03 April 2026)
            sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH);
//            sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

            // temporary calendar combines today's 'day' with the viewed month/year
            Calendar displayCal = (Calendar) currentDate.clone();
            displayCal.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
            return sdf.format(displayCal.getTime());
        } else {
            // If opened the past or future month, show only Month and Year (e.g., May 2026)
            sdf = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
//            sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            return sdf.format(currentDate.getTime());
        }
    }

    public String getHeaderString() {
        return getHeaderString(true);
    }

//Returns the epochDay (Long) for a given day in the current month.
    public Long getEpochDayForDay(String dayText) { // param - the number from the calendar grid cell
        if (dayText.isEmpty()) return null;
        Calendar tempCal = (Calendar) currentDate.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dayText)); // Converts a grid day string ("15") into a Long ID (Epoch Day)

        return tempCal.getTimeInMillis() / (24 * 60 * 60 * 1000L); // (API 24) the number of days
    }

    /**
     * Returns the epochDay for the first and last day of the currently viewed month.
     */
    public long getStartOfMonthEpochDay() {
        Calendar cal = (Calendar) currentDate.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis() / (24 * 60 * 60 * 1000L);
    }
    public long getEndOfMonthEpochDay() {
        Calendar cal = (Calendar) currentDate.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        return cal.getTimeInMillis() / (24 * 60 * 60 * 1000L);
    }
}
