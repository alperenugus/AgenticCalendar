package com.agent.appointmentscheduler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to extract structured information from natural language user messages.
 * This helps the LLM by pre-extracting key information.
 */
public class MessageExtractor {

    private static final Logger log = LoggerFactory.getLogger(MessageExtractor.class);

    // Patterns for extracting date of birth
    private static final Pattern[] DOB_PATTERNS = {
        Pattern.compile("(?i)born\\s+on\\s+([0-9]{4}-[0-9]{2}-[0-9]{2})"),
        Pattern.compile("(?i)born\\s+on\\s+([A-Za-z]+\\s+\\d{1,2},?\\s+\\d{4})"),
        Pattern.compile("(?i)DOB[\\s:]+([0-9]{4}-[0-9]{2}-[0-9]{2})"),
        Pattern.compile("(?i)date\\s+of\\s+birth[\\s:]+([0-9]{4}-[0-9]{2}-[0-9]{2})"),
        Pattern.compile("(?i)birthday[\\s:]+([0-9]{4}-[0-9]{2}-[0-9]{2})"),
    };

    /**
     * Extracts date of birth from a message and adds it as a hint for the LLM.
     */
    public static String enhanceMessageWithExtractedInfo(String message) {
        String enhanced = message;
        
        // Try to extract DOB
        String extractedDob = extractDateOfBirth(message);
        if (extractedDob != null) {
            // Add a hint to the message to help the LLM
            enhanced = message + "\n\n[EXTRACTED INFO: The user mentioned date of birth: " + extractedDob + ". Use this dob value when calling getUser function.]";
            log.debug("Extracted DOB from message: {}", extractedDob);
        }
        
        return enhanced;
    }

    /**
     * Extracts date of birth from the message using regex patterns.
     */
    private static String extractDateOfBirth(String message) {
        for (Pattern pattern : DOB_PATTERNS) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String dobStr = matcher.group(1).trim();
                try {
                    // Try to parse and normalize the date
                    LocalDate dob = DateParser.parseDate(dobStr);
                    return dob.toString(); // Return in yyyy-MM-dd format
                } catch (Exception e) {
                    log.debug("Could not parse extracted DOB: {}", dobStr);
                    return dobStr; // Return as-is if parsing fails
                }
            }
        }
        return null;
    }
}

