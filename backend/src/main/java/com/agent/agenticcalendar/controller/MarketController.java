package com.agent.agenticcalendar.controller;

import com.agent.agenticcalendar.service.MarketDataService;
import com.agent.agenticcalendar.service.MarketDataService.Quote;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public REST endpoints exposing live market data. Backs the frontend market ticker
 * and is independent of the chat agent. Data is cached upstream in {@link MarketDataService}.
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketDataService marketDataService;

    public MarketController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/summary")
    public ResponseEntity<List<Quote>> getMarketSummary() {
        return ResponseEntity.ok(marketDataService.getMarketSummary());
    }

    @GetMapping("/quote")
    public ResponseEntity<Quote> getQuote(@RequestParam String symbol) {
        return marketDataService.getQuote(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
