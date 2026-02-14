package com.agent.agenticcalendar.util;

import com.agent.agenticcalendar.model.Event;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Utility class to expand recurring events into individual occurrences.
 * Implements Google Calendar-style recurrence handling using RFC 5545 RRULE format.
 * 
 * This class handles:
 * - FREQ (DAILY, WEEKLY, MONTHLY, YEARLY)
 * - INTERVAL (every N days/weeks/months/years)
 * - BYDAY (specific days of week)
 * - BYMONTHDAY (specific day of month)
 * - BYMONTH (specific months)
 * - UNTIL (end date)
 * - COUNT (number of occurrences)
 * - EXDATE (excluded dates - deleted instances)
 */
public class RecurrenceExpander {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final int MAX_OCCURRENCES = 1000; // Safety limit

    /**
     * Represents a parsed RRULE
     */
    public static class ParsedRRULE {
        public String FREQ;
        public Integer INTERVAL;
        public String BYDAY;
        public Integer BYMONTHDAY;
        public Integer BYMONTH;
        public LocalDate UNTIL;
        public Integer COUNT;
    }

    /**
     * Expands a recurring event into individual occurrences within a date range.
     * 
     * @param event The recurring event to expand
     * @param rangeStart Start of the date range
     * @param rangeEnd End of the date range
     * @return List of event occurrences (as Event objects with modified start/end times)
     */
    public static List<Event> expandEvent(Event event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (event.getRecurrenceRule() == null || event.getRecurrenceRule().trim().isEmpty()) {
            // Not a recurring event - return as single event if in range
            if (isEventInRange(event, rangeStart, rangeEnd)) {
                return Collections.singletonList(event);
            }
            return Collections.emptyList();
        }

        ParsedRRULE rrule = parseRRULE(event.getRecurrenceRule());
        if (rrule == null) {
            // Invalid RRULE - return original event if in range
            if (isEventInRange(event, rangeStart, rangeEnd)) {
                return Collections.singletonList(event);
            }
            return Collections.emptyList();
        }

        Set<LocalDate> excludedDates = parseExcludedDates(event.getExcludedDates());
        LocalDateTime eventStart = event.getStartTime();
        LocalDateTime eventEnd = event.getEndTime();
        long durationMinutes = ChronoUnit.MINUTES.between(eventStart, eventEnd);

        List<Event> occurrences = new ArrayList<>();
        LocalDateTime currentOccurrence = eventStart;
        int occurrenceCount = 0;
        LocalDate rangeStartDate = rangeStart.toLocalDate();
        LocalDate rangeEndDate = rangeEnd.toLocalDate();

        // Start from the first occurrence that's >= rangeStart (or the original start)
        if (currentOccurrence.isBefore(rangeStart)) {
            // Fast-forward to first occurrence in range
            currentOccurrence = findNextOccurrence(eventStart, rrule, rangeStart.toLocalDate());
            if (currentOccurrence == null) {
                return Collections.emptyList();
            }
        }

        while (currentOccurrence.toLocalDate().isBefore(rangeEndDate.plusDays(1)) && 
               occurrenceCount < MAX_OCCURRENCES) {
            
            LocalDate occurrenceDate = currentOccurrence.toLocalDate();
            
            // Check if this occurrence is excluded
            if (!excludedDates.contains(occurrenceDate)) {
                // Check if occurrence is within range
                if (!occurrenceDate.isBefore(rangeStartDate) && !occurrenceDate.isAfter(rangeEndDate)) {
                    // Check UNTIL constraint
                    if (rrule.UNTIL != null && occurrenceDate.isAfter(rrule.UNTIL)) {
                        break;
                    }
                    
                    // Create occurrence
                    Event occurrence = createOccurrence(event, currentOccurrence, durationMinutes);
                    occurrences.add(occurrence);
                }
            }

            // Move to next occurrence
            currentOccurrence = getNextOccurrence(currentOccurrence, rrule, eventStart);
            if (currentOccurrence == null) {
                break;
            }
            
            occurrenceCount++;
            
            // Check COUNT constraint
            if (rrule.COUNT != null && occurrenceCount >= rrule.COUNT) {
                break;
            }
        }

        return occurrences;
    }

    /**
     * Checks if an event overlaps with a date range.
     */
    private static boolean isEventInRange(Event event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        return !event.getEndTime().isBefore(rangeStart) && !event.getStartTime().isAfter(rangeEnd);
    }

    /**
     * Finds the first occurrence on or after the given date.
     */
    private static LocalDateTime findNextOccurrence(LocalDateTime eventStart, ParsedRRULE rrule, LocalDate targetDate) {
        LocalDateTime current = eventStart;
        int attempts = 0;
        
        while (current.toLocalDate().isBefore(targetDate) && attempts < MAX_OCCURRENCES) {
            current = getNextOccurrence(current, rrule, eventStart);
            if (current == null) {
                return null;
            }
            attempts++;
        }
        
        return current.toLocalDate().isBefore(targetDate) ? null : current;
    }

    /**
     * Creates an occurrence event from the master event.
     */
    private static Event createOccurrence(Event master, LocalDateTime occurrenceStart, long durationMinutes) {
        Event occurrence = new Event();
        occurrence.setId(master.getId());
        occurrence.setTitle(master.getTitle());
        occurrence.setDescription(master.getDescription());
        occurrence.setStartTime(occurrenceStart);
        occurrence.setEndTime(occurrenceStart.plusMinutes(durationMinutes));
        occurrence.setTimezone(master.getTimezone());
        occurrence.setType(master.getType());
        occurrence.setStatus(master.getStatus());
        occurrence.setLocation(master.getLocation());
        occurrence.setIsAllDay(master.getIsAllDay());
        occurrence.setRecurrenceRule(master.getRecurrenceRule());
        occurrence.setReminderTime(master.getReminderTime());
        occurrence.setColor(master.getColor());
        occurrence.setSessionId(master.getSessionId());
        occurrence.setGoogleUserId(master.getGoogleUserId());
        occurrence.setGoogleUserEmail(master.getGoogleUserEmail());
        return occurrence;
    }

    /**
     * Gets the next occurrence based on RRULE.
     */
    private static LocalDateTime getNextOccurrence(LocalDateTime current, ParsedRRULE rrule, LocalDateTime originalStart) {
        if (rrule.FREQ == null) {
            return null;
        }

        int interval = rrule.INTERVAL != null ? rrule.INTERVAL : 1;
        LocalDateTime next = current;

        switch (rrule.FREQ.toUpperCase()) {
            case "DAILY":
                next = current.plusDays(interval);
                break;

            case "WEEKLY":
                if (rrule.BYDAY != null && !rrule.BYDAY.isEmpty()) {
                    // Specific day(s) of week
                    List<DayOfWeek> targetDays = parseDaysOfWeek(rrule.BYDAY);
                    next = findNextDayOfWeek(current, targetDays, interval);
                } else {
                    // Same day of week, next interval
                    next = current.plusWeeks(interval);
                }
                break;

            case "MONTHLY":
                if (rrule.BYMONTHDAY != null) {
                    // Specific day of month
                    next = findNextMonthDay(current, rrule.BYMONTHDAY, interval);
                } else {
                    // Same day of month, next interval
                    next = current.plusMonths(interval);
                }
                break;

            case "YEARLY":
                if (rrule.BYMONTH != null && rrule.BYMONTHDAY != null) {
                    // Specific month and day
                    next = findNextYearDay(current, rrule.BYMONTH, rrule.BYMONTHDAY, interval);
                } else if (rrule.BYMONTH != null) {
                    // Specific month, same day
                    next = findNextYearMonth(current, rrule.BYMONTH, interval);
                } else {
                    // Same month and day, next interval
                    next = current.plusYears(interval);
                }
                break;

            default:
                return null;
        }

        return next;
    }

    /**
     * Finds the next occurrence matching specific days of week.
     */
    private static LocalDateTime findNextDayOfWeek(LocalDateTime current, List<DayOfWeek> targetDays, int interval) {
        LocalDateTime next = current;
        DayOfWeek currentDay = current.getDayOfWeek();
        
        // Find next matching day in current week
        for (DayOfWeek targetDay : targetDays) {
            if (targetDay.getValue() > currentDay.getValue()) {
                int daysToAdd = targetDay.getValue() - currentDay.getValue();
                return current.plusDays(daysToAdd);
            }
        }
        
        // Move to next week and find first matching day
        int daysToNextWeek = 7 - currentDay.getValue() + targetDays.get(0).getValue();
        next = current.plusDays(daysToNextWeek);
        
        // Apply interval
        if (interval > 1) {
            next = next.plusWeeks(interval - 1);
        }
        
        return next;
    }

    /**
     * Finds the next occurrence on a specific day of month.
     */
    private static LocalDateTime findNextMonthDay(LocalDateTime current, int targetDay, int interval) {
        LocalDateTime next = current.plusMonths(interval);
        int lastDayOfMonth = next.toLocalDate().lengthOfMonth();
        int dayToSet = Math.min(targetDay, lastDayOfMonth);
        return next.withDayOfMonth(dayToSet);
    }

    /**
     * Finds the next occurrence on a specific month and day.
     */
    private static LocalDateTime findNextYearDay(LocalDateTime current, int targetMonth, int targetDay, int interval) {
        LocalDateTime next = current.plusYears(interval);
        next = next.withMonth(targetMonth);
        int lastDayOfMonth = next.toLocalDate().lengthOfMonth();
        int dayToSet = Math.min(targetDay, lastDayOfMonth);
        return next.withDayOfMonth(dayToSet);
    }

    /**
     * Finds the next occurrence in a specific month.
     */
    private static LocalDateTime findNextYearMonth(LocalDateTime current, int targetMonth, int interval) {
        LocalDateTime next = current.plusYears(interval);
        return next.withMonth(targetMonth);
    }

    /**
     * Parses RRULE string into ParsedRRULE object.
     */
    public static ParsedRRULE parseRRULE(String rruleString) {
        if (rruleString == null || rruleString.trim().isEmpty()) {
            return null;
        }

        ParsedRRULE rrule = new ParsedRRULE();
        String[] parts = rruleString.toUpperCase().split(";");

        for (String part : parts) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length != 2) continue;

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            switch (key) {
                case "FREQ":
                    rrule.FREQ = value;
                    break;
                case "INTERVAL":
                    try {
                        rrule.INTERVAL = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // Invalid interval, ignore
                    }
                    break;
                case "BYDAY":
                    rrule.BYDAY = value;
                    break;
                case "BYMONTHDAY":
                    try {
                        rrule.BYMONTHDAY = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // Invalid day, ignore
                    }
                    break;
                case "BYMONTH":
                    try {
                        rrule.BYMONTH = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // Invalid month, ignore
                    }
                    break;
                case "UNTIL":
                    try {
                        // UNTIL can be in YYYYMMDD or YYYYMMDDTHHMMSS format
                        if (value.length() == 8) {
                            rrule.UNTIL = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                        } else if (value.length() >= 8) {
                            rrule.UNTIL = LocalDate.parse(value.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
                        }
                    } catch (Exception e) {
                        // Invalid date, ignore
                    }
                    break;
                case "COUNT":
                    try {
                        rrule.COUNT = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        // Invalid count, ignore
                    }
                    break;
            }
        }

        return rrule.FREQ != null ? rrule : null;
    }

    /**
     * Parses days of week from BYDAY value (e.g., "MO,TU,WE").
     */
    private static List<DayOfWeek> parseDaysOfWeek(String byday) {
        List<DayOfWeek> days = new ArrayList<>();
        String[] dayCodes = byday.split(",");
        
        Map<String, DayOfWeek> dayMap = new HashMap<>();
        dayMap.put("MO", DayOfWeek.MONDAY);
        dayMap.put("TU", DayOfWeek.TUESDAY);
        dayMap.put("WE", DayOfWeek.WEDNESDAY);
        dayMap.put("TH", DayOfWeek.THURSDAY);
        dayMap.put("FR", DayOfWeek.FRIDAY);
        dayMap.put("SA", DayOfWeek.SATURDAY);
        dayMap.put("SU", DayOfWeek.SUNDAY);
        
        for (String dayCode : dayCodes) {
            dayCode = dayCode.trim();
            // Handle BYSETPOS (e.g., "1MO" = first Monday)
            if (dayCode.length() > 2) {
                dayCode = dayCode.substring(dayCode.length() - 2);
            }
            DayOfWeek day = dayMap.get(dayCode);
            if (day != null) {
                days.add(day);
            }
        }
        
        return days;
    }

    /**
     * Parses excluded dates from comma-separated string.
     */
    private static Set<LocalDate> parseExcludedDates(String excludedDatesStr) {
        Set<LocalDate> excludedDates = new HashSet<>();
        if (excludedDatesStr == null || excludedDatesStr.trim().isEmpty()) {
            return excludedDates;
        }

        String[] dates = excludedDatesStr.split(",");
        for (String dateStr : dates) {
            try {
                LocalDate date = LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
                excludedDates.add(date);
            } catch (Exception e) {
                // Invalid date format, skip
            }
        }

        return excludedDates;
    }

    /**
     * Checks if a recurring event has any occurrences in a date range.
     * This is used for efficient querying - we can filter recurring events
     * that might have occurrences in the range.
     */
    public static boolean hasOccurrenceInRange(Event event, LocalDateTime rangeStart, LocalDateTime rangeEnd) {
        if (event.getRecurrenceRule() == null) {
            return isEventInRange(event, rangeStart, rangeEnd);
        }

        ParsedRRULE rrule = parseRRULE(event.getRecurrenceRule());
        if (rrule == null) {
            return isEventInRange(event, rangeStart, rangeEnd);
        }

        // Check if event start is before range end (might have occurrences)
        if (event.getStartTime().isAfter(rangeEnd)) {
            return false;
        }

        // Check UNTIL constraint
        if (rrule.UNTIL != null && rrule.UNTIL.isBefore(rangeStart.toLocalDate())) {
            return false;
        }

        // For now, return true if event starts before range end
        // Full expansion will filter out actual occurrences
        return true;
    }
}

