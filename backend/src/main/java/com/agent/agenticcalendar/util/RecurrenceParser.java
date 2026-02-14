package com.agent.agenticcalendar.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Utility class to parse natural language recurrence patterns into RFC 5545 RRULE format.
 * 
 * Examples:
 * - "weekly" or "every week" → FREQ=WEEKLY
 * - "daily" or "every day" → FREQ=DAILY
 * - "monthly" or "every month" → FREQ=MONTHLY
 * - "every Monday" → FREQ=WEEKLY;BYDAY=MO
 * - "every weekday" → FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR
 */
public class RecurrenceParser {

    /**
     * Parses natural language recurrence pattern into RFC 5545 RRULE format.
     * 
     * @param recurrencePattern Natural language pattern (e.g., "weekly", "every Monday", "daily")
     * @param startTime The start time of the event (used to determine day of week for weekly patterns)
     * @return RRULE string or null if pattern cannot be parsed
     */
    public static String parseRecurrence(String recurrencePattern, LocalDateTime startTime) {
        if (recurrencePattern == null || recurrencePattern.trim().isEmpty()) {
            return null;
        }

        String pattern = recurrencePattern.toLowerCase().trim();

        // Daily patterns
        if (matches(pattern, "daily", "every day", "each day", "day", "everyday")) {
            return "FREQ=DAILY";
        }

        // Weekly patterns
        if (matches(pattern, "weekly", "every week", "each week", "week")) {
            // If start time is provided, include the day of week
            if (startTime != null) {
                DayOfWeek dayOfWeek = startTime.getDayOfWeek();
                String dayCode = getDayCode(dayOfWeek);
                return "FREQ=WEEKLY;BYDAY=" + dayCode;
            }
            return "FREQ=WEEKLY";
        }

        // Monthly patterns
        if (matches(pattern, "monthly", "every month", "each month", "month")) {
            // If start time is provided, include the day of month
            if (startTime != null) {
                int dayOfMonth = startTime.getDayOfMonth();
                return "FREQ=MONTHLY;BYMONTHDAY=" + dayOfMonth;
            }
            return "FREQ=MONTHLY";
        }

        // Yearly patterns
        if (matches(pattern, "yearly", "every year", "each year", "annually", "annual")) {
            // If start time is provided, include the month and day
            if (startTime != null) {
                int month = startTime.getMonthValue();
                int dayOfMonth = startTime.getDayOfMonth();
                return "FREQ=YEARLY;BYMONTH=" + month + ";BYMONTHDAY=" + dayOfMonth;
            }
            return "FREQ=YEARLY";
        }

        // Specific day of week patterns
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.name().toLowerCase();
            String dayNameFull = getDayNameFull(day);
            
            if (pattern.contains("every " + dayName) || 
                pattern.contains("every " + dayNameFull) ||
                pattern.contains("each " + dayName) ||
                pattern.contains("each " + dayNameFull)) {
                String dayCode = getDayCode(day);
                return "FREQ=WEEKLY;BYDAY=" + dayCode;
            }
        }

        // Weekday patterns (Monday-Friday)
        if (matches(pattern, "weekday", "weekdays", "every weekday", "each weekday", 
                   "monday to friday", "mon-fri", "mon to fri")) {
            return "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR";
        }

        // Weekend patterns (Saturday-Sunday)
        if (matches(pattern, "weekend", "weekends", "every weekend", "each weekend", 
                   "saturday and sunday", "sat-sun", "sat and sun")) {
            return "FREQ=WEEKLY;BYDAY=SA,SU";
        }

        // Custom frequency patterns (e.g., "every 2 weeks", "every 3 months")
        Pattern everyNPattern = Pattern.compile("every\\s+(\\d+)\\s+(day|week|month|year)s?");
        java.util.regex.Matcher matcher = everyNPattern.matcher(pattern);
        if (matcher.find()) {
            int interval = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "day":
                    return "FREQ=DAILY;INTERVAL=" + interval;
                case "week":
                    if (startTime != null) {
                        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
                        String dayCode = getDayCode(dayOfWeek);
                        return "FREQ=WEEKLY;INTERVAL=" + interval + ";BYDAY=" + dayCode;
                    }
                    return "FREQ=WEEKLY;INTERVAL=" + interval;
                case "month":
                    if (startTime != null) {
                        int dayOfMonth = startTime.getDayOfMonth();
                        return "FREQ=MONTHLY;INTERVAL=" + interval + ";BYMONTHDAY=" + dayOfMonth;
                    }
                    return "FREQ=MONTHLY;INTERVAL=" + interval;
                case "year":
                    if (startTime != null) {
                        int month = startTime.getMonthValue();
                        int dayOfMonth = startTime.getDayOfMonth();
                        return "FREQ=YEARLY;INTERVAL=" + interval + ";BYMONTH=" + month + ";BYMONTHDAY=" + dayOfMonth;
                    }
                    return "FREQ=YEARLY;INTERVAL=" + interval;
            }
        }

        // If it's already in RRULE format, return as-is
        if (pattern.startsWith("freq=") || pattern.startsWith("FREQ=")) {
            return recurrencePattern.toUpperCase();
        }

        // Default: return null if pattern cannot be parsed
        return null;
    }

    /**
     * Checks if the pattern matches any of the given keywords.
     */
    private static boolean matches(String pattern, String... keywords) {
        for (String keyword : keywords) {
            if (pattern.equals(keyword) || pattern.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Converts DayOfWeek to RFC 5545 day code.
     */
    private static String getDayCode(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "MO";
            case TUESDAY: return "TU";
            case WEDNESDAY: return "WE";
            case THURSDAY: return "TH";
            case FRIDAY: return "FR";
            case SATURDAY: return "SA";
            case SUNDAY: return "SU";
            default: return "MO";
        }
    }

    /**
     * Gets full day name for matching.
     */
    private static String getDayNameFull(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "monday";
            case TUESDAY: return "tuesday";
            case WEDNESDAY: return "wednesday";
            case THURSDAY: return "thursday";
            case FRIDAY: return "friday";
            case SATURDAY: return "saturday";
            case SUNDAY: return "sunday";
            default: return "monday";
        }
    }
}

