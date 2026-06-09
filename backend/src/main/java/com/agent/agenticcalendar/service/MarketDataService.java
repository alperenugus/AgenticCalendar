package com.agent.agenticcalendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fetches real-time stock quotes and market index data.
 *
 * Primary source is Yahoo Finance's public v8 chart endpoint (keyless, works from
 * server/datacenter IPs). It falls back from query1 to query2, and optionally to
 * Finnhub if a {@code MARKET_FINNHUB_API_KEY} is configured.
 *
 * Quotes are cached in-memory for {@link #CACHE_TTL} so the agent, the REST API,
 * and the frontend ticker all share a single upstream call per symbol per minute.
 */
@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    // Major US indices surfaced in the market summary (Yahoo symbols -> display names).
    private static final Map<String, String> INDEX_NAMES = new LinkedHashMap<>();
    static {
        INDEX_NAMES.put("^GSPC", "S&P 500");
        INDEX_NAMES.put("^DJI", "Dow Jones");
        INDEX_NAMES.put("^IXIC", "Nasdaq Composite");
    }

    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(8);
    private static final List<String> YAHOO_HOSTS = List.of("query1.finance.yahoo.com", "query2.finance.yahoo.com");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${market.finnhub-api-key:}")
    private String finnhubApiKey;

    private final Map<String, CachedQuote> cache = new ConcurrentHashMap<>();

    public MarketDataService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** A point-in-time market quote for a single instrument. */
    public record Quote(
            String symbol,
            String name,
            double price,
            double previousClose,
            double change,
            double changePercent,
            double dayHigh,
            double dayLow,
            long volume,
            String currency,
            Instant asOf
    ) {}

    private record CachedQuote(Quote quote, Instant fetchedAt) {}

    /**
     * Returns a quote for the given ticker symbol (e.g. "AAPL", "MSFT", "^GSPC").
     * Results are cached for {@link #CACHE_TTL}. Returns empty if all sources fail.
     */
    public Optional<Quote> getQuote(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return Optional.empty();
        }
        String symbol = rawSymbol.trim().toUpperCase();

        CachedQuote cached = cache.get(symbol);
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(CACHE_TTL) < 0) {
            return Optional.of(cached.quote());
        }

        Optional<Quote> quote = fetchFromYahoo(symbol);
        if (quote.isEmpty() && finnhubApiKey != null && !finnhubApiKey.isBlank()) {
            quote = fetchFromFinnhub(symbol);
        }
        quote.ifPresent(q -> cache.put(symbol, new CachedQuote(q, Instant.now())));
        return quote;
    }

    /** Returns quotes for the major tracked indices (best effort; skips any that fail). */
    public List<Quote> getMarketSummary() {
        List<Quote> quotes = new ArrayList<>();
        for (String symbol : INDEX_NAMES.keySet()) {
            getQuote(symbol).ifPresent(q -> quotes.add(withDisplayName(q)));
        }
        return quotes;
    }

    /** Replaces an index's raw name with a friendly display name when we have one. */
    private Quote withDisplayName(Quote q) {
        String friendly = INDEX_NAMES.get(q.symbol());
        if (friendly == null) {
            return q;
        }
        return new Quote(q.symbol(), friendly, q.price(), q.previousClose(), q.change(),
                q.changePercent(), q.dayHigh(), q.dayLow(), q.volume(), q.currency(), q.asOf());
    }

    private Optional<Quote> fetchFromYahoo(String symbol) {
        String encoded = URLEncoder.encode(symbol, StandardCharsets.UTF_8);
        for (String host : YAHOO_HOSTS) {
            try {
                String url = "https://" + host + "/v8/finance/chart/" + encoded + "?range=2d&interval=1d";
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0 (compatible; AgenticCalendar/1.0)")
                        .header("Accept", "application/json")
                        .timeout(HTTP_TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.warn("Yahoo {} returned HTTP {} for {}", host, response.statusCode(), symbol);
                    continue;
                }
                JsonNode meta = objectMapper.readTree(response.body())
                        .path("chart").path("result").path(0).path("meta");
                if (meta.isMissingNode() || !meta.has("regularMarketPrice")) {
                    continue;
                }

                double price = meta.path("regularMarketPrice").asDouble();
                double prevClose = meta.has("chartPreviousClose")
                        ? meta.path("chartPreviousClose").asDouble()
                        : meta.path("previousClose").asDouble(price);
                double change = price - prevClose;
                double changePct = prevClose != 0 ? (change / prevClose) * 100.0 : 0.0;
                String name = meta.has("longName") ? meta.path("longName").asText()
                        : meta.path("shortName").asText(symbol);

                Quote quote = new Quote(
                        meta.path("symbol").asText(symbol),
                        name,
                        round2(price),
                        round2(prevClose),
                        round2(change),
                        round2(changePct),
                        round2(meta.path("regularMarketDayHigh").asDouble(price)),
                        round2(meta.path("regularMarketDayLow").asDouble(price)),
                        meta.path("regularMarketVolume").asLong(0L),
                        meta.path("currency").asText("USD"),
                        Instant.ofEpochSecond(meta.path("regularMarketTime").asLong(Instant.now().getEpochSecond()))
                );
                return Optional.of(quote);
            } catch (Exception e) {
                log.warn("Yahoo fetch failed on {} for {}: {}", host, symbol, e.getMessage());
            }
        }
        return Optional.empty();
    }

    /** Optional fallback for individual stocks when a Finnhub key is configured. */
    private Optional<Quote> fetchFromFinnhub(String symbol) {
        try {
            String url = "https://finnhub.io/api/v1/quote?symbol="
                    + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
                    + "&token=" + finnhubApiKey;
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(HTTP_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode node = objectMapper.readTree(response.body());
            if (!node.has("c") || node.path("c").asDouble() == 0.0) {
                return Optional.empty();
            }
            double price = node.path("c").asDouble();
            double prevClose = node.path("pc").asDouble(price);
            double change = price - prevClose;
            double changePct = prevClose != 0 ? (change / prevClose) * 100.0 : 0.0;
            Quote quote = new Quote(symbol, symbol, round2(price), round2(prevClose), round2(change),
                    round2(changePct), round2(node.path("h").asDouble(price)), round2(node.path("l").asDouble(price)),
                    0L, "USD", Instant.now());
            return Optional.of(quote);
        } catch (Exception e) {
            log.warn("Finnhub fetch failed for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
