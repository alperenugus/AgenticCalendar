# Migration Plan: Appointment Scheduler → Agentic Calendar

## 🎯 Vision
Transform from a user management + appointment system into a **Google Calendar-like agentic calendar** where users interact entirely through natural language prompts.

## 📋 Migration Overview

### Phase 1: Core Model Transformation (Week 1)
### Phase 2: Remove User Management (Week 1)
### Phase 3: Add Calendar Features (Week 2-3)
### Phase 4: Enhanced Agent Capabilities (Week 3-4)
### Phase 5: UI/UX Improvements (Week 4-5)

---

## Phase 1: Core Model Transformation

### 1.1 Rename `Appointment` → `Event`
**File**: `backend/src/main/java/com/agent/appointmentscheduler/model/Appointment.java`

**Changes**:
- Rename class to `Event`
- Rename table to `events`
- Remove `userId` field (replace with session-based ownership)
- Add new fields:
  ```java
  private String title;              // Event title (replaces description)
  private String description;        // Optional detailed description
  private LocalDateTime startTime;   // Rename from appointmentDateTime
  private LocalDateTime endTime;     // NEW: Event duration
  private String timezone;           // NEW: User's timezone
  private EventType type;            // NEW: MEETING, REMINDER, TASK, ALL_DAY
  private EventStatus status;        // NEW: TENTATIVE, CONFIRMED, CANCELLED
  private String location;           // NEW: Event location
  private Boolean isAllDay;          // NEW: All-day events
  private String recurrenceRule;     // NEW: For recurring events (RRULE format)
  private LocalDateTime reminderTime; // NEW: When to send reminder
  private String color;             // NEW: Calendar color coding
  ```

**New Enums**:
```java
public enum EventType {
    MEETING, REMINDER, TASK, ALL_DAY, BIRTHDAY
}

public enum EventStatus {
    TENTATIVE, CONFIRMED, CANCELLED, COMPLETED
}
```

### 1.2 Update Repository
- Rename `AppointmentRepository` → `EventRepository`
- Update query methods:
  ```java
  List<Event> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
  List<Event> findByDate(LocalDate date);
  List<Event> findUpcoming(LocalDateTime now);
  List<Event> findConflicting(LocalDateTime start, LocalDateTime end);
  ```

### 1.3 Database Migration
- Create migration script to:
  - Rename table `appointments` → `events`
  - Add new columns
  - Migrate existing data
  - Drop `userId` column (or keep for backward compatibility initially)

---

## Phase 2: Remove User Management

### 2.1 Remove User Management Tools
**File**: `backend/src/main/java/com/agent/appointmentscheduler/tools/AppointmentToolService.java`

**Remove**:
- `getUser()` tool
- `createUser()` tool
- `updateUser()` tool
- `deleteUser()` tool

**Update**:
- All event operations become session-based
- Events belong to the session (no user lookup needed)
- Simplify agent prompts (no user management instructions)

### 2.2 Update System Prompt
**File**: `backend/src/main/java/com/agent/appointmentscheduler/service/AgentService.java`

**Remove**:
- All user management instructions
- User lookup requirements

**Add**:
- Calendar-focused instructions
- Natural language examples for calendar operations

### 2.3 Remove User Controllers/Services
**Files to Remove/Deprecate**:
- `UserController.java` (or keep for admin only)
- `UserService.java` (or simplify to session management)
- `UserRepository.java` (keep if needed for future features)
- `User.java` model (keep for potential future use)

### 2.4 Update Frontend
- Remove "Registered Users" panel
- Update welcome message to focus on calendar
- Remove user management UI components

---

## Phase 3: Add Calendar Features

### 3.1 Recurring Events
**New Tool**: `createRecurringEvent()`
```java
@Tool("Create a recurring event. Examples: 'every Monday at 2pm', 'daily at 9am', 'first Friday of every month'")
public String createRecurringEvent(String title, String startTime, String recurrenceRule, String endTime, String description)
```

**Implementation**:
- Parse recurrence patterns (daily, weekly, monthly, yearly)
- Store as RRULE format
- Generate instances on-the-fly or pre-generate

### 3.2 Event Duration
**Update**: `createEvent()` tool
- Add `duration` parameter (in minutes)
- Calculate `endTime` automatically
- Support natural language: "1 hour meeting", "30 minute call"

### 3.3 Conflict Detection
**New Tool**: `checkConflicts()`
```java
@Tool("Check if a time slot has conflicts with existing events")
public String checkConflicts(String startTime, String endTime)
```

**Features**:
- Detect overlapping events
- Suggest alternative times
- Warn before creating conflicts

### 3.4 Time Zone Support
**Implementation**:
- Store user's timezone in session
- Convert all times to user's timezone
- Display times in user's local time
- Support queries like "what time is my meeting in EST?"

### 3.5 Event Status
**Update**: `updateEventStatus()`
- Allow marking events as tentative, confirmed, cancelled
- Color-code by status in UI

### 3.6 Reminders
**New Tool**: `setReminder()`
```java
@Tool("Set a reminder for an event. Examples: 'remind me 15 minutes before', 'remind me tomorrow'")
public String setReminder(Long eventId, String reminderTime)
```

**Implementation**:
- Store reminder preferences
- Background job to send reminders (email/push)
- Support multiple reminders per event

### 3.7 All-Day Events
**Update**: `createEvent()`
- Support "all day" flag
- Handle date-only events (no time)

### 3.8 Location
**Update**: `createEvent()`
- Add location field
- Support "at office", "Zoom link", addresses
- Display in event details

---

## Phase 4: Enhanced Agent Capabilities

### 4.1 Natural Language Queries
**New Tools**:

1. **`queryCalendar()`** - Complex queries
   ```java
   @Tool("Query calendar with natural language. Examples: 'what's on my calendar next week?', 'show me all meetings this month', 'when is my next appointment?'")
   public String queryCalendar(String query, String dateRange)
   ```

2. **`findFreeTime()`** - Find available slots
   ```java
   @Tool("Find free time slots. Examples: 'when am I free next week?', 'find 2 hours free tomorrow'")
   public String findFreeTime(String dateRange, Integer durationMinutes)
   ```

3. **`suggestTime()`** - Smart scheduling
   ```java
   @Tool("Suggest best time for an event. Examples: 'when should I schedule a 1 hour meeting?', 'best time for a call this week'")
   public String suggestTime(Integer durationMinutes, String preferences)
   ```

### 4.2 Bulk Operations
**New Tools**:

1. **`rescheduleEvent()`** - Reschedule with conflict checking
2. **`cancelEvent()`** - Cancel with optional reason
3. **`duplicateEvent()`** - Copy event to another time
4. **`moveEvent()`** - Move to different date/time

### 4.3 Smart Suggestions
- "You have 3 meetings tomorrow, want to reschedule any?"
- "You're double-booked at 2pm, which should I move?"
- "You haven't scheduled lunch, want me to block time?"

### 4.4 Update System Prompt
**Focus on**:
- Calendar operations only
- Natural language understanding
- Proactive suggestions
- Conflict resolution

---

## Phase 5: UI/UX Improvements

### 5.1 Calendar Views
**New Components**:

1. **Month View** (`CalendarMonthView.jsx`)
   - Grid layout showing full month
   - Click to create/view events
   - Color-coded by event type

2. **Week View** (`CalendarWeekView.jsx`)
   - 7-day horizontal layout
   - Time slots for each day
   - Drag-and-drop (future enhancement)

3. **Day View** (`CalendarDayView.jsx`)
   - Detailed single-day view
   - Time slots with events
   - Better for detailed planning

4. **Agenda View** (`CalendarAgendaView.jsx`)
   - List of upcoming events
   - Grouped by date
   - Current implementation (keep as default)

### 5.2 Event Details Modal
**New Component**: `EventDetailsModal.jsx`
- Show full event information
- Edit event inline
- Quick actions (reschedule, cancel, duplicate)
- Show conflicts if any

### 5.3 Quick Actions
- "Create event" button with smart defaults
- Quick filters (Today, This Week, This Month)
- Search events by title/description

### 5.4 Visual Improvements
- Color coding by event type
- Status indicators (tentative, confirmed)
- Conflict highlighting
- Reminder badges

---

## Implementation Priority

### 🔥 High Priority (MVP)
1. ✅ Rename Appointment → Event
2. ✅ Remove user management
3. ✅ Add event duration
4. ✅ Add conflict detection
5. ✅ Add time zone support
6. ✅ Add event status
7. ✅ Update agent tools and prompts

### ⚡ Medium Priority (Core Features)
8. ✅ Recurring events
9. ✅ Reminders
10. ✅ All-day events
11. ✅ Location field
12. ✅ Natural language queries
13. ✅ Calendar views (month/week/day)

### 🚀 Nice to Have (Polish)
14. ✅ Event colors
15. ✅ Bulk operations
16. ✅ Smart suggestions
17. ✅ Export to ICS
18. ✅ Calendar sharing (future)

---

## File Changes Summary

### Files to Modify
- `Appointment.java` → `Event.java` (major changes)
- `AppointmentRepository.java` → `EventRepository.java`
- `AppointmentService.java` → `EventService.java`
- `AppointmentToolService.java` → `CalendarToolService.java` (remove user tools, add calendar tools)
- `AppointmentController.java` → `EventController.java`
- `AgentService.java` (update system prompt)
- `AppointmentTable.jsx` → `CalendarView.jsx` (multiple view components)
- `ChatComponent.jsx` (update welcome message)

### Files to Remove/Deprecate
- `UserController.java` (or keep minimal)
- `UserService.java` (or simplify)
- `UserTable.jsx` (remove from UI)

### New Files to Create
- `EventType.java` (enum)
- `EventStatus.java` (enum)
- `RecurrenceRule.java` (utility)
- `ConflictDetector.java` (service)
- `ReminderService.java` (service)
- `CalendarMonthView.jsx`
- `CalendarWeekView.jsx`
- `CalendarDayView.jsx`
- `EventDetailsModal.jsx`

---

## Database Migration Script

```sql
-- Rename table
ALTER TABLE appointments RENAME TO events;

-- Add new columns
ALTER TABLE events ADD COLUMN title VARCHAR(255);
ALTER TABLE events ADD COLUMN end_time TIMESTAMP;
ALTER TABLE events ADD COLUMN timezone VARCHAR(50) DEFAULT 'UTC';
ALTER TABLE events ADD COLUMN type VARCHAR(20) DEFAULT 'MEETING';
ALTER TABLE events ADD COLUMN status VARCHAR(20) DEFAULT 'CONFIRMED';
ALTER TABLE events ADD COLUMN location VARCHAR(500);
ALTER TABLE events ADD COLUMN is_all_day BOOLEAN DEFAULT FALSE;
ALTER TABLE events ADD COLUMN recurrence_rule VARCHAR(500);
ALTER TABLE events ADD COLUMN reminder_time TIMESTAMP;
ALTER TABLE events ADD COLUMN color VARCHAR(20) DEFAULT '#3b82f6';

-- Migrate existing data
UPDATE events SET title = description WHERE title IS NULL;
UPDATE events SET end_time = appointment_date_time + INTERVAL '1 hour' WHERE end_time IS NULL;
UPDATE events SET status = 'CONFIRMED';

-- Rename column
ALTER TABLE events RENAME COLUMN appointment_date_time TO start_time;

-- Optional: Keep userId for backward compatibility or remove
-- ALTER TABLE events DROP COLUMN user_id;
```

---

## Updated Agent Tools (Final List)

### Core Event Operations
1. `createEvent(title, startTime, endTime, description, location, type)` - Create event
2. `updateEvent(eventId, startTime?, endTime?, title?, description?, location?, status?)` - Update event
3. `deleteEvent(eventId)` - Delete/cancel event
4. `getEvent(eventId)` - Get event details
5. `getEvents(dateRange)` - Get events in date range

### Advanced Features
6. `createRecurringEvent(title, startTime, recurrenceRule, endTime?, description?)` - Recurring events
7. `checkConflicts(startTime, endTime)` - Conflict detection
8. `findFreeTime(dateRange, durationMinutes)` - Find available time
9. `suggestTime(durationMinutes, preferences)` - Suggest best time
10. `setReminder(eventId, reminderTime)` - Set reminder
11. `rescheduleEvent(eventId, newStartTime, newEndTime?)` - Reschedule with conflict check
12. `queryCalendar(query, dateRange?)` - Natural language queries

---

## Updated System Prompt Focus

```
You are an AI Calendar Assistant. Your goal is to help users manage their calendar 
through natural language.

### YOUR CAPABILITIES:
- Create, update, delete, and view calendar events
- Handle recurring events (daily, weekly, monthly, yearly)
- Detect and prevent scheduling conflicts
- Find free time slots
- Suggest optimal meeting times
- Set reminders
- Answer questions about the calendar

### NATURAL LANGUAGE EXAMPLES:
- "Schedule a meeting tomorrow at 2pm"
- "What's on my calendar next week?"
- "Reschedule my 3pm meeting to Friday"
- "Do I have any conflicts this week?"
- "When am I free for a 1-hour call?"
- "Create a daily reminder at 9am"
- "Cancel all meetings on Monday"

### IMPORTANT:
- Always check for conflicts before creating events
- Suggest alternative times if conflicts exist
- Be proactive: warn about double-bookings
- Keep events organized and clear
```

---

## Testing Strategy

### Unit Tests
- Event creation with all fields
- Recurrence rule parsing
- Conflict detection logic
- Time zone conversions

### Integration Tests
- End-to-end event creation flow
- Conflict detection scenarios
- Recurring event generation
- Natural language query parsing

### Agent Tests
- "Create a meeting tomorrow at 2pm"
- "What's on my calendar next week?"
- "Reschedule my meeting to Friday"
- "Do I have any conflicts?"

---

## Timeline Estimate

- **Week 1**: Phase 1 & 2 (Model transformation, remove user management)
- **Week 2**: Phase 3 (Core calendar features)
- **Week 3**: Phase 4 (Enhanced agent capabilities)
- **Week 4**: Phase 5 (UI improvements)
- **Week 5**: Testing, polish, documentation

**Total: ~5 weeks for full migration**

---

## Success Metrics

✅ Users can create events via natural language
✅ Recurring events work correctly
✅ Conflict detection prevents double-booking
✅ Calendar views display events properly
✅ Agent understands complex queries
✅ No user management complexity
✅ Feels like Google Calendar but with AI

---

## Next Steps

1. Review and approve this plan
2. Create feature branch: `feature/calendar-migration`
3. Start with Phase 1 (model transformation)
4. Iterate and test each phase
5. Deploy incrementally

