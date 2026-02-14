import { useState, useEffect, useMemo, useRef } from 'react'
import { ChevronLeft, ChevronRight, Calendar, Clock, MapPin, RefreshCw, Grid3x3, List } from 'lucide-react'
import axios from 'axios'
import toast from 'react-hot-toast'
import { expandAllEvents } from '../utils/recurrenceExpander'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function CalendarView({ refreshTrigger, user }) {
  const [events, setEvents] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [currentDate, setCurrentDate] = useState(new Date())
  const [viewMode, setViewMode] = useState('month') // 'month', 'week', 'day'
  const [selectedDate, setSelectedDate] = useState(new Date())

  const fetchEvents = async (silent = false) => {
    try {
      if (!silent) {
        setIsLoading(true)
      }
      
      const params = new URLSearchParams()
      if (user?.id) {
        params.append('googleUserId', user.id)
      } else {
        const sessionId = localStorage.getItem('chatSessionId') || 'default'
        params.append('sessionId', sessionId)
      }
      
      const response = await axios.get(`${API_BASE_URL}/events?${params.toString()}`, {
        withCredentials: true
      })
      setEvents(response.data || [])
    } catch (error) {
      console.error('Error fetching events:', error)
      if (!silent) {
        toast.error('Failed to load events')
      }
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchEvents()
  }, [refreshTrigger, user])

  // Auto-refresh every 10 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      fetchEvents(true)
    }, 10000)
    return () => clearInterval(interval)
  }, [user])

  const navigateDate = (direction) => {
    const newDate = new Date(currentDate)
    if (viewMode === 'month') {
      newDate.setMonth(newDate.getMonth() + direction)
    } else if (viewMode === 'week') {
      newDate.setDate(newDate.getDate() + (direction * 7))
    } else {
      newDate.setDate(newDate.getDate() + direction)
    }
    setCurrentDate(newDate)
  }

  const goToToday = () => {
    setCurrentDate(new Date())
    setSelectedDate(new Date())
  }

  // Get expanded events for the visible date range
  const getExpandedEvents = useMemo(() => {
    if (!events || events.length === 0) return []
    
    // Calculate visible date range based on current view
    let rangeStart, rangeEnd
    const now = new Date()
    
    if (viewMode === 'month') {
      const year = currentDate.getFullYear()
      const month = currentDate.getMonth()
      rangeStart = new Date(year, month, 1)
      rangeEnd = new Date(year, month + 1, 0)
      // Only show current month - no extra days
    } else if (viewMode === 'week') {
      const startOfWeek = new Date(currentDate)
      const day = startOfWeek.getDay()
      startOfWeek.setDate(startOfWeek.getDate() - day)
      rangeStart = new Date(startOfWeek)
      rangeEnd = new Date(startOfWeek)
      rangeEnd.setDate(rangeEnd.getDate() + 7)
    } else {
      // Day view
      rangeStart = new Date(currentDate)
      rangeStart.setHours(0, 0, 0, 0)
      rangeEnd = new Date(currentDate)
      rangeEnd.setHours(23, 59, 59, 999)
    }
    
    return expandAllEvents(events, rangeStart, rangeEnd)
  }, [events, currentDate, viewMode])

  // Get events for a specific date
  const getEventsForDate = (date) => {
    return getExpandedEvents.filter(event => {
      if (!event.startTime) return false
      const eventDate = new Date(event.startTime)
      return eventDate.toDateString() === date.toDateString()
    })
  }

  // Month view
  const MonthView = () => {
    const year = currentDate.getFullYear()
    const month = currentDate.getMonth()
    
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)
    const daysInMonth = lastDay.getDate()
    const startingDayOfWeek = firstDay.getDay()
    
    const days = []
    
    // Previous month's trailing days
    const prevMonth = new Date(year, month - 1, 0)
    const daysInPrevMonth = prevMonth.getDate()
    for (let i = startingDayOfWeek - 1; i >= 0; i--) {
      days.push({
        date: new Date(year, month - 1, daysInPrevMonth - i),
        isCurrentMonth: false,
      })
    }
    
    // Current month's days
    for (let day = 1; day <= daysInMonth; day++) {
      days.push({
        date: new Date(year, month, day),
        isCurrentMonth: true,
      })
    }
    
    // Only show current month days - don't add next month's days
    const currentMonthDays = days.filter(dayObj => dayObj.isCurrentMonth)
    
    const weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
    
    return (
      <div className="flex-1 overflow-auto">
        <div className="grid grid-cols-7 border-b border-slate-700 sticky top-0 bg-slate-800/95 z-10">
          {weekDays.map(day => (
            <div key={day} className="p-2 text-center text-xs font-semibold text-slate-400 border-r border-slate-700 last:border-r-0">
              {day}
            </div>
          ))}
        </div>
        <div className="grid grid-cols-7 auto-rows-fr">
          {currentMonthDays.map((dayObj, index) => {
            const dayEvents = getEventsForDate(dayObj.date)
            const isToday = dayObj.date.toDateString() === new Date().toDateString()
            const isSelected = dayObj.date.toDateString() === selectedDate.toDateString()
            
            return (
              <div
                key={index}
                onClick={() => setSelectedDate(dayObj.date)}
                className={`min-h-[100px] border-r border-b border-slate-700 p-1 cursor-pointer hover:bg-slate-800/50 transition-colors ${
                  isToday ? 'bg-blue-900/20' : ''} ${isSelected ? 'ring-2 ring-blue-500' : ''}`}
              >
                <div className={`text-xs font-medium mb-1 px-1 ${
                  isToday ? 'bg-blue-600 text-white rounded-full w-6 h-6 flex items-center justify-center' : 'text-slate-300'
                }`}>
                  {dayObj.date.getDate()}
                </div>
                <div className="space-y-0.5">
                  {dayEvents.slice(0, 3).map((event, eventIndex) => {
                    const startTime = new Date(event.startTime)
                    const timeStr = startTime.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true })
                    return (
                      <div
                        key={event.id}
                        className="text-xs px-1.5 py-0.5 rounded truncate"
                        style={{
                          backgroundColor: event.color || '#3b82f6',
                          color: 'white',
                        }}
                        title={`${timeStr} - ${event.title}`}
                      >
                        <span className="font-medium">{timeStr}</span> {event.title}
                      </div>
                    )
                  })}
                  {dayEvents.length > 3 && (
                    <div className="text-xs text-slate-400 px-1.5">
                      +{dayEvents.length - 3} more
                    </div>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      </div>
    )
  }

  // Week view
  const WeekView = () => {
    const weekViewRef = useRef(null)
    
    // Scroll to 8 AM on mount and when currentDate or viewMode changes
    useEffect(() => {
      if (viewMode === 'week') {
        // Use requestAnimationFrame to ensure DOM is fully rendered
        const scrollTo8AM = () => {
          if (weekViewRef.current) {
            // 8 AM = 8 hours * 64px per hour = 512px
            weekViewRef.current.scrollTop = 512
          } else {
            // If ref not ready, try again on next frame
            requestAnimationFrame(scrollTo8AM)
          }
        }
        
        // Double RAF to ensure layout is complete
        requestAnimationFrame(() => {
          requestAnimationFrame(scrollTo8AM)
        })
      }
    }, [currentDate, viewMode])
    
    const startOfWeek = new Date(currentDate)
    const day = startOfWeek.getDay()
    startOfWeek.setDate(startOfWeek.getDate() - day)
    
    const weekDays = []
    for (let i = 0; i < 7; i++) {
      const date = new Date(startOfWeek)
      date.setDate(startOfWeek.getDate() + i)
      weekDays.push(date)
    }
    
    const hours = Array.from({ length: 24 }, (_, i) => i)
    
    return (
      <div className="flex-1 overflow-auto" ref={weekViewRef}>
        <div className="grid grid-cols-8 border-b border-slate-700 sticky top-0 bg-slate-800/95 z-10">
          <div className="p-2 border-r border-slate-700"></div>
          {weekDays.map((date, index) => {
            const isToday = date.toDateString() === new Date().toDateString()
            return (
              <div key={index} className={`p-2 text-center border-r border-slate-700 last:border-r-0 ${isToday ? 'bg-blue-900/20' : ''}`}>
                <div className="text-xs text-slate-400 font-medium">
                  {date.toLocaleDateString('en-US', { weekday: 'short' })}
                </div>
                <div className={`text-lg font-semibold mt-1 ${isToday ? 'text-blue-400' : 'text-white'}`}>
                  {date.getDate()}
                </div>
              </div>
            )
          })}
        </div>
        <div className="grid grid-cols-8">
          <div className="border-r border-slate-700">
            {hours.map(hour => (
              <div key={hour} className="h-16 border-b border-slate-700 p-1 text-xs text-slate-500">
                {hour === 0 ? '12 AM' : hour < 12 ? `${hour} AM` : hour === 12 ? '12 PM' : `${hour - 12} PM`}
              </div>
            ))}
          </div>
          {weekDays.map((date, dayIndex) => {
            const dayEvents = getEventsForDate(date)
            return (
              <div key={dayIndex} className="border-r border-slate-700 last:border-r-0">
                {hours.map(hour => {
                  const hourEvents = dayEvents.filter(event => {
                    const eventDate = new Date(event.startTime)
                    return eventDate.getHours() === hour
                  })
                  return (
                    <div key={hour} className="h-16 border-b border-slate-700 p-0.5 relative">
                      {hourEvents.map((event, eventIndex) => {
                        const startTime = new Date(event.startTime)
                        const endTime = new Date(event.endTime)
                        const duration = (endTime - startTime) / (1000 * 60) // minutes
                        const height = Math.max((duration / 60) * 64, 20) // 64px per hour
                        const top = (startTime.getMinutes() / 60) * 64
                        
                        return (
                          <div
                            key={event.id}
                            className="absolute left-0 right-0 rounded px-1 text-xs text-white flex items-center justify-center"
                            style={{
                              backgroundColor: event.color || '#3b82f6',
                              top: `${top}px`,
                              height: `${height}px`,
                              zIndex: 10,
                            }}
                            title={`${event.title} - ${startTime.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })}`}
                          >
                            <div className="font-medium break-words text-center">{event.title}</div>
                          </div>
                        )
                      })}
                    </div>
                  )
                })}
              </div>
            )
          })}
        </div>
      </div>
    )
  }

  // Day view
  const DayView = () => {
    const dayViewRef = useRef(null)
    
    // Scroll to 8 AM on mount and when currentDate or viewMode changes
    useEffect(() => {
      if (viewMode === 'day') {
        // Use requestAnimationFrame to ensure DOM is fully rendered
        const scrollTo8AM = () => {
          if (dayViewRef.current) {
            // 8 AM = 8 hours * 64px per hour = 512px
            dayViewRef.current.scrollTop = 512
          } else {
            // If ref not ready, try again on next frame
            requestAnimationFrame(scrollTo8AM)
          }
        }
        
        // Double RAF to ensure layout is complete
        requestAnimationFrame(() => {
          requestAnimationFrame(scrollTo8AM)
        })
      }
    }, [currentDate, viewMode])
    
    const hours = Array.from({ length: 24 }, (_, i) => i)
    const dayEvents = getEventsForDate(currentDate)
    
    return (
      <div className="flex-1 overflow-auto" ref={dayViewRef}>
        <div className="grid grid-cols-2 border-b border-slate-700 sticky top-0 bg-slate-800/95 z-10">
          <div className="p-2 border-r border-slate-700"></div>
          <div className="p-2 text-center">
            <div className="text-xs text-slate-400 font-medium">
              {currentDate.toLocaleDateString('en-US', { weekday: 'long' })}
            </div>
            <div className="text-lg font-semibold mt-1 text-white">
              {currentDate.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
            </div>
          </div>
        </div>
        <div className="grid grid-cols-2">
          <div className="border-r border-slate-700">
            {hours.map(hour => (
              <div key={hour} className="h-16 border-b border-slate-700 p-1 text-xs text-slate-500">
                {hour === 0 ? '12 AM' : hour < 12 ? `${hour} AM` : hour === 12 ? '12 PM' : `${hour - 12} PM`}
              </div>
            ))}
          </div>
          <div className="relative">
            {hours.map(hour => {
              const hourEvents = dayEvents.filter(event => {
                const eventDate = new Date(event.startTime)
                return eventDate.getHours() === hour
              })
              return (
                <div key={hour} className="h-16 border-b border-slate-700 p-0.5 relative">
                  {hourEvents.map((event, eventIndex) => {
                    const startTime = new Date(event.startTime)
                    const endTime = new Date(event.endTime)
                    const duration = (endTime - startTime) / (1000 * 60) // minutes
                    const height = Math.max((duration / 60) * 64, 20) // 64px per hour
                    const top = (startTime.getMinutes() / 60) * 64
                    
                    return (
                      <div
                        key={event.id}
                        className="absolute left-0 right-0 rounded px-2 py-1 text-xs text-white flex flex-col items-center justify-center"
                        style={{
                          backgroundColor: event.color || '#3b82f6',
                          top: `${top}px`,
                          height: `${height}px`,
                          zIndex: 10,
                        }}
                      >
                        <div className="font-semibold break-words text-center">{event.title}</div>
                        {event.location && (
                          <div className="text-xs opacity-75 mt-1 flex items-center gap-1">
                            <MapPin className="w-3 h-3" />
                            {event.location}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              )
            })}
          </div>
        </div>
      </div>
    )
  }

  const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
  const currentMonthYear = `${monthNames[currentDate.getMonth()]} ${currentDate.getFullYear()}`

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Header */}
      <div className="px-6 py-3 border-b border-slate-700 flex items-center justify-between flex-shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigateDate(-1)}
            className="p-1.5 hover:bg-slate-700 rounded-lg transition-colors"
          >
            <ChevronLeft className="w-5 h-5 text-slate-400" />
          </button>
          <button
            onClick={() => navigateDate(1)}
            className="p-1.5 hover:bg-slate-700 rounded-lg transition-colors"
          >
            <ChevronRight className="w-5 h-5 text-slate-400" />
          </button>
          <button
            onClick={goToToday}
            className="px-4 py-1.5 text-sm bg-slate-700 hover:bg-slate-600 text-white rounded-lg transition-colors"
          >
            Today
          </button>
          <h2 className="text-xl font-semibold text-white ml-2">
            {viewMode === 'month' ? currentMonthYear : 
             viewMode === 'week' ? `Week of ${currentDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}` :
             currentDate.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
          </h2>
        </div>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-1 bg-slate-700 rounded-lg p-1">
            <button
              onClick={() => setViewMode('month')}
              className={`px-3 py-1.5 text-sm rounded transition-colors ${
                viewMode === 'month' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:bg-slate-600'
              }`}
            >
              Month
            </button>
            <button
              onClick={() => setViewMode('week')}
              className={`px-3 py-1.5 text-sm rounded transition-colors ${
                viewMode === 'week' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:bg-slate-600'
              }`}
            >
              Week
            </button>
            <button
              onClick={() => setViewMode('day')}
              className={`px-3 py-1.5 text-sm rounded transition-colors ${
                viewMode === 'day' ? 'bg-blue-600 text-white' : 'text-slate-300 hover:bg-slate-600'
              }`}
            >
              Day
            </button>
          </div>
          <button
            onClick={() => fetchEvents(false)}
            disabled={isLoading}
            className="p-2 hover:bg-slate-700 rounded-lg transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 text-slate-400 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Calendar Content */}
      {isLoading && events.length === 0 ? (
        <div className="flex-1 flex items-center justify-center">
          <div className="text-center">
            <RefreshCw className="w-8 h-8 text-blue-400 animate-spin mx-auto mb-2" />
            <p className="text-slate-400">Loading calendar...</p>
          </div>
        </div>
      ) : (
        <>
          {viewMode === 'month' && <MonthView />}
          {viewMode === 'week' && <WeekView />}
          {viewMode === 'day' && <DayView />}
        </>
      )}

      {/* Selected Date Events Sidebar (if needed) */}
      {viewMode === 'month' && (
        <div className="border-t border-slate-700 p-4 bg-slate-800/50 flex-shrink-0 max-h-32 overflow-y-auto">
          <div className="text-sm font-semibold text-white mb-2">
            {selectedDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
          </div>
          {getEventsForDate(selectedDate).length === 0 ? (
            <p className="text-xs text-slate-500">No events</p>
          ) : (
            <div className="space-y-1">
              {getEventsForDate(selectedDate).map(event => {
                const startTime = new Date(event.startTime)
                return (
                  <div key={event.id} className="text-xs p-2 rounded" style={{ backgroundColor: event.color || '#3b82f6', color: 'white' }}>
                    <div className="font-medium">{event.title}</div>
                    <div className="opacity-90">{startTime.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit', hour12: true })}</div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export default CalendarView

