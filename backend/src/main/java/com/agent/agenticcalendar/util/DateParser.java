package com.agent.agenticcalendar.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Utility class for parsing dates from various formats including natural language.
 */
public class DateParser {

    private static final Logger log = LoggerFactory.getLogger(DateParser.class);
    
    // Common date patterns
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE,                    // yyyy-MM-dd
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),           // yyyy/MM/dd
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),           // MM/dd/yyyy
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),           // dd/MM/yyyy
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),           // dd-MM-yyyy
        DateTimeFormatter.ofPattern("yyyy.MM.dd"),           // yyyy.MM.dd
    };

    /**
     * Parse a date from various formats. Handles ISO format, common formats, and attempts
     * to parse natural language dates.
     */
    public static LocalDate parseDate(Object dateObj) {
        if (dateObj == null) {
            return null;
        }

        // If already a LocalDate, return it
        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        }

        String dateStr = dateObj.toString().trim();

        // Try standard ISO format first (most common)
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            // Continue to try other formats
        }

        // Try other common formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Continue to next format
            }
        }

        // Try to extract date from natural language patterns
        LocalDate extracted = extractDateFromNaturalLanguage(dateStr);
        if (extracted != null) {
            return extracted;
        }

        log.warn("Could not parse date: {}", dateStr);
        throw new IllegalArgumentException("Invalid date format: " + dateStr + ". Expected format: yyyy-MM-dd");
    }

    /**
     * Attempts to extract date from natural language strings like "January 1, 1990" or "1st January 1990"
     */
    private static LocalDate extractDateFromNaturalLanguage(String dateStr) {
        // Pattern for "January 1, 1990" or "Jan 1, 1990"
        Pattern pattern1 = Pattern.compile(
            "(?i)(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|September|Oct|October|Nov|November|Dec|December)\\s+(\\d{1,2}),?\\s+(\\d{4})"
        );
        
        // Pattern for "1 January 1990" or "1st January 1990"
        Pattern pattern2 = Pattern.compile(
            "(?i)(\\d{1,2})(?:st|nd|rd|th)?\\s+(Jan|January|Feb|February|Mar|March|Apr|April|May|Jun|June|Jul|July|Aug|August|Sep|September|Oct|October|Nov|November|Dec|December)\\s+(\\d{4})"
        );

        // Try pattern1
        var matcher1 = pattern1.matcher(dateStr);
        if (matcher1.find()) {
            try {
                int month = parseMonth(matcher1.group(1));
                int day = Integer.parseInt(matcher1.group(2));
                int year = Integer.parseInt(matcher1.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                log.debug("Failed to parse date from pattern1: {}", dateStr);
            }
        }

        // Try pattern2
        var matcher2 = pattern2.matcher(dateStr);
        if (matcher2.find()) {
            try {
                int day = Integer.parseInt(matcher2.group(1));
                int month = parseMonth(matcher2.group(2));
                int year = Integer.parseInt(matcher2.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                log.debug("Failed to parse date from pattern2: {}", dateStr);
            }
        }

        return null;
    }

    private static int parseMonth(String monthStr) {
        String month = monthStr.toLowerCase();
        return switch (month) {
            case "jan", "january" -> 1;
            case "feb", "february" -> 2;
            case "mar", "march" -> 3;
            case "apr", "april" -> 4;
            case "may" -> 5;
            case "jun", "june" -> 6;
            case "jul", "july" -> 7;
            case "aug", "august" -> 8;
            case "sep", "september" -> 9;
            case "oct", "october" -> 10;
            case "nov", "november" -> 11;
            case "dec", "december" -> 12;
            default -> throw new IllegalArgumentException("Invalid month: " + monthStr);
        };
    }
}

