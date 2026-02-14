import { useState, useEffect } from 'react'
import { Calendar, Clock, RefreshCw, AlertCircle, MapPin } from 'lucide-react'
import axios from 'axios'
import toast from 'react-hot-toast'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function EventTable({ refreshTrigger, user }) {
  const [events, setEvents] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isManualRefresh, setIsManualRefresh] = useState(false)
  const [error, setError] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)

  const fetchEvents = async (silent = false) => {
    try {
      if (!silent) {
        setIsLoading(true)
      }
      setError(null)
      
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
      setLastUpdated(new Date())
    } catch (error) {
      console.error('Error fetching events:', error)
      setError('Failed to load events')
      if (error.response) {
        toast.error('Failed to fetch events from server')
      } else if (error.request) {
        toast.error('Backend server is not responding')
      }
    } finally {
      setIsLoading(false)
      setIsManualRefresh(false)
    }
  }

  useEffect(() => {
    fetchEvents()
  }, [refreshTrigger, user])

  // Auto-refresh every 5 seconds (silent - no loading indicator)
  useEffect(() => {
    const interval = setInterval(() => {
      fetchEvents(true) // Silent refresh
    }, 5000)

    return () => clearInterval(interval)
  }, [user])

  const formatDateTime = (dateTimeString) => {
    if (!dateTimeString) return 'N/A'
    try {
      const date = new Date(dateTimeString)
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch (error) {
      return dateTimeString
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'CONFIRMED':
        return 'text-green-400'
      case 'TENTATIVE':
        return 'text-yellow-400'
      case 'CANCELLED':
        return 'text-red-400'
      default:
        return 'text-slate-400'
    }
  }

  if (isLoading && events.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <RefreshCw className="w-8 h-8 text-blue-400 animate-spin mx-auto mb-2" />
          <p className="text-slate-400">Loading events...</p>
        </div>
      </div>
    )
  }

  if (error && events.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-8 h-8 text-red-400 mx-auto mb-2" />
          <p className="text-slate-400">{error}</p>
          <button
            onClick={fetchEvents}
            className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* Header with refresh button */}
      <div className="px-6 py-3 border-b border-slate-700 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <p className="text-sm text-slate-400">
            {events.length} event{events.length !== 1 ? 's' : ''} found
          </p>
          {lastUpdated && !isLoading && (
            <span className="text-xs text-slate-500">
              Updated {lastUpdated.toLocaleTimeString()}
            </span>
          )}
        </div>
        <button
          onClick={() => {
            setIsManualRefresh(true)
            fetchEvents(false)
          }}
          disabled={isLoading && isManualRefresh}
          className="flex items-center gap-2 px-3 py-1.5 text-sm bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed text-slate-200 rounded-lg transition-colors"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading && isManualRefresh ? 'animate-spin' : ''}`} />
          Refresh
        </button>
      </div>

      {/* Table Container */}
      <div className="flex-1 overflow-y-auto">
        {events.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <Calendar className="w-12 h-12 text-slate-600 mx-auto mb-4" />
              <p className="text-slate-400">No events found</p>
              <p className="text-sm text-slate-500 mt-2">
                Create an event using the chat interface
              </p>
            </div>
          </div>
        ) : (
          <div className="p-6">
            <div className="space-y-3">
              {events
                .filter(event => event.status !== 'CANCELLED')
                .sort((a, b) => new Date(a.startTime) - new Date(b.startTime))
                .map((event) => (
                  <div
                    key={event.id}
                    className="bg-slate-800/50 border border-slate-700 rounded-lg p-4 hover:bg-slate-800 transition-colors"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <h3 className="text-base font-semibold text-white">{event.title}</h3>
                          {event.status && (
                            <span className={`text-xs px-2 py-0.5 rounded ${getStatusColor(event.status)} bg-slate-700`}>
                              {event.status}
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-4 text-sm text-slate-400">
                          <div className="flex items-center gap-1">
                            <Clock className="w-4 h-4" />
                            <span>
                              {formatDateTime(event.startTime)} - {formatDateTime(event.endTime)}
                            </span>
                          </div>
                          {event.location && (
                            <div className="flex items-center gap-1">
                              <MapPin className="w-4 h-4" />
                              <span>{event.location}</span>
                            </div>
                          )}
                        </div>
                        {event.description && (
                          <p className="text-sm text-slate-300 mt-2">{event.description}</p>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default EventTable

