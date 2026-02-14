/**
 * Utility to expand recurring events into individual occurrences for calendar display.
 * Parses RFC 5545 RRULE format and generates event instances for a given date range.
 */

/**
 * Expands a recurring event into individual occurrences for the visible date range.
 * @param {Object} event - Event object with startTime, endTime, and recurrenceRule
 * @param {Date} rangeStart - Start of the visible date range
 * @param {Date} rangeEnd - End of the visible date range
 * @returns {Array} Array of expanded event instances
 */
export function expandRecurringEvent(event, rangeStart, rangeEnd) {
  if (!event.recurrenceRule || !event.startTime) {
    // Not a recurring event, return as single event
    return [event]
  }

  const occurrences = []
  const startDate = new Date(event.startTime)
  const endDate = new Date(event.endTime)
  const duration = endDate - startDate // Duration in milliseconds

  // Parse RRULE
  const rrule = parseRRULE(event.recurrenceRule)
  
  if (!rrule) {
    // Invalid RRULE, return original event
    return [event]
  }

  // Generate occurrences based on frequency
  let currentDate = new Date(startDate)
  const maxOccurrences = 100 // Limit to prevent infinite loops
  let count = 0

  while (currentDate <= rangeEnd && count < maxOccurrences) {
    // Check if this occurrence is within the visible range
    if (currentDate >= rangeStart) {
      const occurrence = {
        ...event,
        id: `${event.id}-${currentDate.getTime()}`, // Unique ID for this occurrence
        startTime: new Date(currentDate),
        endTime: new Date(currentDate.getTime() + duration),
        isRecurringInstance: true,
        originalEventId: event.id
      }
      occurrences.push(occurrence)
    }

    // Calculate next occurrence based on frequency
    currentDate = getNextOccurrence(currentDate, rrule, startDate)
    count++
  }

  return occurrences.length > 0 ? occurrences : [event]
}

/**
 * Parses RFC 5545 RRULE format into an object.
 * @param {string} rruleString - RRULE string (e.g., "FREQ=WEEKLY;BYDAY=MO")
 * @returns {Object} Parsed RRULE object
 */
function parseRRULE(rruleString) {
  if (!rruleString) return null

  const rrule = {}
  const parts = rruleString.split(';')

  for (const part of parts) {
    const [key, value] = part.split('=')
    if (key && value) {
      rrule[key.toUpperCase()] = value.toUpperCase()
    }
  }

  return rrule
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

  switch (freq) {
    case 'DAILY':
      const interval = rrule.INTERVAL ? parseInt(rrule.INTERVAL) : 1
      next.setDate(next.getDate() + interval)
      break

    case 'WEEKLY':
      const weekInterval = rrule.INTERVAL ? parseInt(rrule.INTERVAL) : 1
      if (rrule.BYDAY) {
        // Specific day(s) of week
        const days = rrule.BYDAY.split(',')
        const currentDay = next.getDay()
        const dayCodes = { 'SU': 0, 'MO': 1, 'TU': 2, 'WE': 3, 'TH': 4, 'FR': 5, 'SA': 6 }
        
        // Find next matching day
        let found = false
        for (const day of days) {
          const targetDay = dayCodes[day]
          if (targetDay > currentDay) {
            const daysToAdd = targetDay - currentDay
            next.setDate(next.getDate() + daysToAdd)
            found = true
            break
          }
        }
        
        if (!found) {
          // Move to next week
          const firstDay = dayCodes[days[0]]
          const daysToAdd = 7 - currentDay + firstDay + (weekInterval - 1) * 7
          next.setDate(next.getDate() + daysToAdd)
        }
      } else {
        // Same day of week, next interval
        next.setDate(next.getDate() + (7 * weekInterval))
      }
      break

    case 'MONTHLY':
      const monthInterval = rrule.INTERVAL ? parseInt(rrule.INTERVAL) : 1
      if (rrule.BYMONTHDAY) {
        // Specific day of month
        const dayOfMonth = parseInt(rrule.BYMONTHDAY)
        next.setMonth(next.getMonth() + monthInterval)
        next.setDate(dayOfMonth)
      } else {
        // Same day of month, next interval
        next.setMonth(next.getMonth() + monthInterval)
      }
      break

    case 'YEARLY':
      const yearInterval = rrule.INTERVAL ? parseInt(rrule.INTERVAL) : 1
      next.setFullYear(next.getFullYear() + yearInterval)
      break

    default:
      // Unknown frequency, default to daily
      next.setDate(next.getDate() + 1)
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
      expanded.push(event)
    }
  }
  
  return expanded
}

