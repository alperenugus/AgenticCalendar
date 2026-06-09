import { useState, useEffect } from 'react'
import { TrendingUp, TrendingDown } from 'lucide-react'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

/**
 * Slim live market bar showing the major US indices. Backed by the public
 * /api/market/summary endpoint; refreshes every 60s (data is cached server-side).
 */
function MarketTicker() {
  const [indices, setIndices] = useState([])

  useEffect(() => {
    let cancelled = false

    const fetchSummary = async () => {
      try {
        const res = await axios.get(`${API_BASE_URL}/market/summary`)
        if (!cancelled) setIndices(res.data || [])
      } catch {
        // Market data is best-effort; stay silent on failure.
      }
    }

    fetchSummary()
    const interval = setInterval(fetchSummary, 60000)
    return () => {
      cancelled = true
      clearInterval(interval)
    }
  }, [])

  if (!indices.length) return null

  return (
    <div className="border-b border-slate-700 bg-slate-800/30">
      <div className="container mx-auto px-6 py-2 flex items-center gap-6 overflow-x-auto">
        <span className="text-xs font-semibold text-slate-500 uppercase tracking-wide flex-shrink-0">
          Markets
        </span>
        {indices.map((idx) => {
          const up = (idx.change ?? 0) >= 0
          return (
            <div key={idx.symbol} className="flex items-center gap-2 flex-shrink-0" title={`${idx.symbol} • prev close ${idx.previousClose}`}>
              <span className="text-sm font-medium text-slate-300">{idx.name}</span>
              <span className="text-sm text-white tabular-nums">
                {Number(idx.price).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </span>
              <span className={`text-xs flex items-center gap-0.5 tabular-nums ${up ? 'text-green-400' : 'text-red-400'}`}>
                {up ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                {up ? '+' : ''}{Number(idx.changePercent).toFixed(2)}%
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

export default MarketTicker
