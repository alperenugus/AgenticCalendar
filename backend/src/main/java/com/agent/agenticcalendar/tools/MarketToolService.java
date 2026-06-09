package com.agent.agenticcalendar.tools;

import com.agent.agenticcalendar.service.MarketDataService;
import com.agent.agenticcalendar.service.MarketDataService.Quote;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent-facing wrapper around {@link MarketDataService} that returns compact JSON
 * strings the LLM can render into a natural-language answer. Mirrors the style of
 * {@link com.agent.agenticcalendar.tools.CalendarToolService}.
 */
@Service
public class MarketToolService {

    private static final Logger log = LoggerFactory.getLogger(MarketToolService.class);

    private final MarketDataService marketDataService;

    public MarketToolService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @Tool("Get the latest real-time stock quote for a single ticker symbol. Use this when the user asks about a "
            + "stock price, how a company's shares are doing, or market movement for a specific company. Convert "
            + "company names to ticker symbols (e.g. 'Apple' -> 'AAPL', 'Tesla' -> 'TSLA', 'Microsoft' -> 'MSFT'). "
            + "Requires: symbol (a ticker like AAPL, MSFT, TSLA, NVDA, or an index like ^GSPC).")
    public String getStockQuote(String symbol) {
        log.info("🟢 getStockQuote CALLED with symbol={}", symbol);
        if (symbol == null || symbol.isBlank()) {
            return "{\"error\": \"No ticker symbol provided. Ask the user which stock they mean (e.g., AAPL, TSLA).\"}";
        }
        return marketDataService.getQuote(symbol)
                .map(this::toJson)
                .orElseGet(() -> String.format(
                        "{\"error\": \"Could not retrieve a quote for '%s'. Make sure it is a valid ticker symbol "
                                + "(e.g., AAPL, MSFT, TSLA). Market data may also be temporarily unavailable.\"}",
                        escapeJson(symbol)));
    }

    @Tool("Get a summary of how the major US stock market indices are performing right now "
            + "(S&P 500, Dow Jones, and Nasdaq Composite). Use this when the user asks 'how is the market doing?', "
            + "'how are stocks today?', or for a general market overview. Takes no parameters.")
    public String getMarketSummary() {
        log.info("🟢 getMarketSummary CALLED");
        List<Quote> quotes = marketDataService.getMarketSummary();
        if (quotes.isEmpty()) {
            return "{\"error\": \"Market data is temporarily unavailable. Please try again in a moment.\"}";
        }
        StringBuilder json = new StringBuilder("{\"indices\": [");
        for (int i = 0; i < quotes.size(); i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append(toJson(quotes.get(i)));
        }
        json.append("], \"message\": \"Latest US market index levels\"}");
        return json.toString();
    }

    private String toJson(Quote q) {
        return String.format(
                "{\"symbol\": \"%s\", \"name\": \"%s\", \"price\": %.2f, \"currency\": \"%s\", "
                        + "\"change\": %.2f, \"changePercent\": %.2f, \"dayHigh\": %.2f, \"dayLow\": %.2f, "
                        + "\"previousClose\": %.2f, \"asOf\": \"%s\"}",
                escapeJson(q.symbol()), escapeJson(q.name()), q.price(), escapeJson(q.currency()),
                q.change(), q.changePercent(), q.dayHigh(), q.dayLow(), q.previousClose(), q.asOf());
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
