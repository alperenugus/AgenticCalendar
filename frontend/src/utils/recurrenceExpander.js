/**
 * Utility to expand recurring events into individual occurrences for calendar display.
 * Parses RFC 5545 RRULE format and generates event instances for a given date range.
 * Matches Google Calendar's recurrence handling behavior.
 */

/**
 * Expands a recurring event into individual occurrences for the visible date range.
 * @param {Object} event - Event object with startTime, endTime, recurrenceRule, and excludedDates
 * @param {Date} rangeStart - Start of the visible date range
 * @param {Date} rangeEnd - End of the visible date range
 * @returns {Array} Array of expanded event instances
 */
export function expandRecurringEvent(event, rangeStart, rangeEnd) {
  if (!event.recurrenceRule || !event.startTime) {
    // Not a recurring event, return as single event if in range
    if (isEventInRange(event, rangeStart, rangeEnd)) {
      return [event]
    }
    return []
  }

  const occurrences = []
  const startDate = new Date(event.startTime)
  const endDate = new Date(event.endTime)
  const duration = endDate - startDate // Duration in milliseconds

  // Parse RRULE
  const rrule = parseRRULE(event.recurrenceRule)
  
  if (!rrule) {
    // Invalid RRULE, return original event if in range
    if (isEventInRange(event, rangeStart, rangeEnd)) {
      return [event]
    }
    return []
  }

  // Parse excluded dates (deleted instances)
  const excludedDates = parseExcludedDates(event.excludedDates || '')

  // Generate occurrences based on frequency
  let currentDate = new Date(startDate)
  const maxOccurrences = 1000 // Safety limit
  let occurrenceCount = 0

  // Fast-forward to first occurrence in range if needed
  if (currentDate < rangeStart) {
    currentDate = findNextOccurrenceInRange(startDate, rrule, rangeStart)
    if (!currentDate) {
      return []
    }
  }

  while (currentDate <= rangeEnd && occurrenceCount < maxOccurrences) {
    const occurrenceDate = new Date(currentDate)
    const dateKey = formatDateKey(occurrenceDate)

    // Check if this occurrence is excluded (deleted instance)
    if (!excludedDates.has(dateKey)) {
      // Check UNTIL constraint
      if (rrule.UNTIL) {
        const untilDate = parseUNTILDate(rrule.UNTIL)
        if (untilDate && occurrenceDate > untilDate) {
          break
        }
      }

      // Check if this occurrence is within the visible range
      if (occurrenceDate >= rangeStart && occurrenceDate <= rangeEnd) {
        const occurrence = {
          ...event,
          id: `${event.id}-${occurrenceDate.getTime()}`, // Unique ID for this occurrence
          startTime: new Date(occurrenceDate),
          endTime: new Date(occurrenceDate.getTime() + duration),
          isRecurringInstance: true,
          originalEventId: event.id
        }
        occurrences.push(occurrence)
      }
    }

    // Calculate next occurrence based on frequency
    const nextDate = getNextOccurrence(currentDate, rrule, startDate)
    if (!nextDate || nextDate <= currentDate) {
      break // No more occurrences or infinite loop protection
    }
    currentDate = nextDate
    occurrenceCount++

    // Check COUNT constraint
    if (rrule.COUNT && occurrenceCount >= parseInt(rrule.COUNT)) {
      break
    }
  }

  return occurrences
}

/**
 * Checks if an event overlaps with a date range.
 */
function isEventInRange(event, rangeStart, rangeEnd) {
  const eventStart = new Date(event.startTime)
  const eventEnd = new Date(event.endTime)
  return eventEnd >= rangeStart && eventStart <= rangeEnd
}

/**
 * Finds the first occurrence on or after the given date.
 */
function findNextOccurrenceInRange(eventStart, rrule, targetDate) {
  let current = new Date(eventStart)
  let attempts = 0
  const maxAttempts = 1000

  while (current < targetDate && attempts < maxAttempts) {
    const next = getNextOccurrence(current, rrule, eventStart)
    if (!next || next <= current) {
      return null
    }
    current = next
    attempts++
  }

  return current < targetDate ? null : current
}

/**
 * Parses RFC 5545 RRULE format into an object.
 * @param {string} rruleString - RRULE string (e.g., "FREQ=WEEKLY;BYDAY=MO;UNTIL=20241231")
 * @returns {Object} Parsed RRULE object
 */
function parseRRULE(rruleString) {
  if (!rruleString) return null

  const rrule = {}
  const parts = rruleString.split(';')

  for (const part of parts) {
    const [key, value] = part.split('=')
    if (key && value) {
      const upperKey = key.toUpperCase()
      if (upperKey === 'UNTIL' || upperKey === 'COUNT') {
        rrule[upperKey] = value // Keep original format for UNTIL/COUNT
      } else {
        rrule[upperKey] = value.toUpperCase()
      }
    }
  }

  return rrule.FREQ ? rrule : null
}

/**
 * Parses UNTIL date from RRULE format (YYYYMMDD or YYYYMMDDTHHMMSS).
 */
function parseUNTILDate(untilStr) {
  if (!untilStr) return null
  
  try {
    // Handle YYYYMMDD format
    if (untilStr.length >= 8) {
      const year = parseInt(untilStr.substring(0, 4))
      const month = parseInt(untilStr.substring(4, 6)) - 1 // JS months are 0-indexed
      const day = parseInt(untilStr.substring(6, 8))
      return new Date(year, month, day, 23, 59, 59) // End of day
    }
  } catch (e) {
    // Invalid format
  }
  return null
}

/**
 * Parses excluded dates from comma-separated string.
 */
function parseExcludedDates(excludedDatesStr) {
  const excludedDates = new Set()
  if (!excludedDatesStr || !excludedDatesStr.trim()) {
    return excludedDates
  }

  const dates = excludedDatesStr.split(',')
  for (const dateStr of dates) {
    try {
      const date = new Date(dateStr.trim())
      if (!isNaN(date.getTime())) {
        excludedDates.add(formatDateKey(date))
      }
    } catch (e) {
      // Invalid date format, skip
    }
  }

  return excludedDates
}

/**
 * Formats a date as YYYY-MM-DD key for comparison.
 */
function formatDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * Calculates the next occurrence date based on RRULE.
 * @param {Date} currentDate - Current occurrence date
 * @param {Object} rrule - Parsed RRULE object
 * @param {Date} originalStartDate - Original event start date
 * @returns {Date} Next occurrence date
 */
function getNextOccurrence(currentDate, rrule, originalStartDate) {
  const next = new Date(currentDate)
  const freq = rrule.FREQ
  const interval = rrule.INTERVAL ? parseInt(rrule.INTERVAL) : 1

  switch (freq) {
    case 'DAILY':
      next.setDate(next.getDate() + interval)
      break

    case 'WEEKLY':
      if (rrule.BYDAY) {
        // Specific day(s) of week
        const targetDays = parseDaysOfWeek(rrule.BYDAY)
        const nextDay = findNextDayOfWeek(next, targetDays, interval)
        if (nextDay) {
          return nextDay
        }
      } else {
        // Same day of week, next interval
        next.setDate(next.getDate() + (7 * interval))
      }
      break

    case 'MONTHLY':
      if (rrule.BYMONTHDAY) {
        // Specific day of month
        const dayOfMonth = parseInt(rrule.BYMONTHDAY)
        next.setMonth(next.getMonth() + interval)
        const lastDayOfMonth = new Date(next.getFullYear(), next.getMonth() + 1, 0).getDate()
        next.setDate(Math.min(dayOfMonth, lastDayOfMonth))
      } else {
        // Same day of month, next interval
        next.setMonth(next.getMonth() + interval)
      }
      break

    case 'YEARLY':
      if (rrule.BYMONTH && rrule.BYMONTHDAY) {
        // Specific month and day
        const month = parseInt(rrule.BYMONTH) - 1 // JS months are 0-indexed
        const day = parseInt(rrule.BYMONTHDAY)
        next.setFullYear(next.getFullYear() + interval)
        next.setMonth(month)
        const lastDayOfMonth = new Date(next.getFullYear(), month + 1, 0).getDate()
        next.setDate(Math.min(day, lastDayOfMonth))
      } else if (rrule.BYMONTH) {
        // Specific month, same day
        const month = parseInt(rrule.BYMONTH) - 1
        next.setFullYear(next.getFullYear() + interval)
        next.setMonth(month)
      } else {
        // Same month and day, next interval
        next.setFullYear(next.getFullYear() + interval)
      }
      break

    default:
      // Unknown frequency, default to daily
      next.setDate(next.getDate() + 1)
  }

  return next
}

/**
 * Parses days of week from BYDAY value (e.g., "MO,TU,WE").
 */
function parseDaysOfWeek(byday) {
  const dayCodes = {
    'SU': 0, 'MO': 1, 'TU': 2, 'WE': 3, 'TH': 4, 'FR': 5, 'SA': 6
  }
  const days = []
  const dayStrings = byday.split(',')
  
  for (const dayStr of dayStrings) {
    const trimmed = dayStr.trim()
    // Handle BYSETPOS (e.g., "1MO" = first Monday) - just extract the day code
    const dayCode = trimmed.length > 2 ? trimmed.substring(trimmed.length - 2) : trimmed
    if (dayCodes[dayCode] !== undefined) {
      days.push(dayCodes[dayCode])
    }
  }
  
  return days.sort((a, b) => a - b)
}

/**
 * Finds the next occurrence matching specific days of week.
 */
function findNextDayOfWeek(current, targetDays, interval) {
  const next = new Date(current)
  const currentDay = next.getDay()
  
  // Find next matching day in current week
  for (const targetDay of targetDays) {
    if (targetDay > currentDay) {
      const daysToAdd = targetDay - currentDay
      next.setDate(next.getDate() + daysToAdd)
      return next
    }
  }
  
  // Move to next week and find first matching day
  const daysToNextWeek = 7 - currentDay + targetDays[0]
  next.setDate(next.getDate() + daysToNextWeek)
  
  // Apply interval
  if (interval > 1) {
    next.setDate(next.getDate() + (interval - 1) * 7)
  }
  
  return next
}

/**
 * Expands all events (recurring and non-recurring) for a date range.
 * @param {Array} events - Array of events
 * @param {Date} rangeStart - Start of visible range
 * @param {Date} rangeEnd - End of visible range
 * @returns {Array} Expanded events array
 */
export function expandAllEvents(events, rangeStart, rangeEnd) {
  const expanded = []
  
  for (const event of events) {
    if (event.recurrenceRule) {
      const occurrences = expandRecurringEvent(event, rangeStart, rangeEnd)
      expanded.push(...occurrences)
    } else {
      // Non-recurring event - include if in range
      if (isEventInRange(event, rangeStart, rangeEnd)) {
        expanded.push(event)
      }
    }
  }
  
  return expanded
}
