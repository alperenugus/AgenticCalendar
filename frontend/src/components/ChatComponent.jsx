import { useState, useRef, useEffect } from 'react'
import { Send, Bot, User, Loader2 } from 'lucide-react'
import axios from 'axios'
import toast from 'react-hot-toast'

const API_BASE_URL = 'http://localhost:8080/api'

function ChatComponent({ onMessageSent }) {
  const [messages, setMessages] = useState([
    {
      id: 1,
      type: 'assistant',
      content: "Hello! I'm your AI appointment scheduling assistant. I can help you create, update, or cancel appointments. How can I assist you today?",
      thinking: false,
    },
  ])
  const [input, setInput] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSend = async (e) => {
    e.preventDefault()
    if (!input.trim() || isLoading) return

    const userMessage = input.trim()
    setInput('')

    // Add user message
    const userMsg = {
      id: Date.now(),
      type: 'user',
      content: userMessage,
      thinking: false,
    }
    setMessages(prev => [...prev, userMsg])

    // Add thinking indicator
    const thinkingMsg = {
      id: Date.now() + 1,
      type: 'assistant',
      content: '',
      thinking: true,
    }
    setMessages(prev => [...prev, thinkingMsg])
    setIsLoading(true)

    try {
      const response = await axios.post(`${API_BASE_URL}/agent/chat`, {
        message: userMessage,
      })

      // Remove thinking indicator and add response
      setMessages(prev => {
        const filtered = prev.filter(msg => !msg.thinking)
        return [
          ...filtered,
          {
            id: Date.now() + 2,
            type: 'assistant',
            content: response.data.response,
            thinking: false,
          },
        ]
      })

      // Trigger appointment table refresh
      onMessageSent()
      toast.success('Message processed successfully')
    } catch (error) {
      console.error('Error sending message:', error)
      
      // Remove thinking indicator
      setMessages(prev => prev.filter(msg => !msg.thinking))

      // Add error message
      setMessages(prev => [
        ...prev,
        {
          id: Date.now() + 2,
          type: 'assistant',
          content: 'Sorry, I encountered an error processing your request. Please try again.',
          thinking: false,
        },
      ])

      if (error.response) {
        toast.error(`Error: ${error.response.data?.response || error.message}`)
      } else if (error.request) {
        toast.error('Backend server is not responding. Please make sure the Spring Boot server is running on port 8080.')
      } else {
        toast.error(`Error: ${error.message}`)
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="flex flex-col h-full">
      {/* Messages Container */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex gap-3 ${
              message.type === 'user' ? 'justify-end' : 'justify-start'
            }`}
          >
            {message.type === 'assistant' && (
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center">
                {message.thinking ? (
                  <Loader2 className="w-5 h-5 text-white animate-spin" />
                ) : (
                  <Bot className="w-5 h-5 text-white" />
                )}
              </div>
            )}
            <div
              className={`max-w-[80%] rounded-lg px-4 py-2 ${
                message.type === 'user'
                  ? 'bg-blue-600 text-white'
                  : message.thinking
                  ? 'bg-slate-700 text-slate-300 border border-slate-600'
                  : 'bg-slate-700 text-slate-100'
              }`}
            >
              {message.thinking ? (
                <div className="flex items-center gap-2">
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span className="text-sm italic">Thinking...</span>
                </div>
              ) : (
                <p className="text-sm whitespace-pre-wrap">{message.content}</p>
              )}
            </div>
            {message.type === 'user' && (
              <div className="flex-shrink-0 w-8 h-8 rounded-full bg-slate-600 flex items-center justify-center">
                <User className="w-5 h-5 text-white" />
              </div>
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form */}
      <div className="border-t border-slate-700 p-4">
        <form onSubmit={handleSend} className="flex gap-2">
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your message..."
            disabled={isLoading}
            className="flex-1 px-4 py-2 bg-slate-700 border border-slate-600 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:opacity-50 disabled:cursor-not-allowed"
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 disabled:cursor-not-allowed text-white rounded-lg transition-colors flex items-center gap-2"
          >
            <Send className="w-4 h-4" />
            <span>Send</span>
          </button>
        </form>
      </div>
    </div>
  )
}

export default ChatComponent

