package com.agent.agenticcalendar.service;

import com.agent.agenticcalendar.config.RateLimitConfig;
import com.agent.agenticcalendar.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service using Bucket4j (token bucket algorithm).
 * Implements per-session rate limiting to prevent token abuse.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    
    private final RateLimitConfig config;
    
    // Per-session buckets for chat endpoint
    private final Map<String, Bucket> chatBuckets = new ConcurrentHashMap<>();
    
    // Per-session buckets for history endpoint
    private final Map<String, Bucket> historyBuckets = new ConcurrentHashMap<>();

    public RateLimitService(RateLimitConfig config) {
        this.config = config;
    }

    /**
     * Check if a request is allowed for the chat endpoint.
     * Throws RateLimitExceededException if limit is exceeded.
     */
    public void checkChatRateLimit(String sessionId) {
        Bucket bucket = chatBuckets.computeIfAbsent(sessionId, this::createChatBucket);
        
        if (!bucket.tryConsume(1)) {
            long retryAfter = calculateRetryAfter(bucket);
            int remaining = (int) bucket.getAvailableTokens();
            
            log.warn("Rate limit exceeded for session: {} (chat endpoint). Retry after: {} seconds", 
                    sessionId, retryAfter);
            
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please wait before sending another message.",
                    retryAfter,
                    remaining
            );
        }
        
        log.debug("Rate limit check passed for session: {} (chat endpoint). Remaining tokens: {}", 
                sessionId, bucket.getAvailableTokens());
    }

    /**
     * Check if a request is allowed for the history endpoint.
     * Throws RateLimitExceededException if limit is exceeded.
     */
    public void checkHistoryRateLimit(String sessionId) {
        Bucket bucket = historyBuckets.computeIfAbsent(sessionId, this::createHistoryBucket);
        
        if (!bucket.tryConsume(1)) {
            long retryAfter = calculateRetryAfter(bucket);
            int remaining = (int) bucket.getAvailableTokens();
            
            log.warn("Rate limit exceeded for session: {} (history endpoint). Retry after: {} seconds", 
                    sessionId, retryAfter);
            
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please wait before requesting history again.",
                    retryAfter,
                    remaining
            );
        }
        
        log.debug("Rate limit check passed for session: {} (history endpoint). Remaining tokens: {}", 
                sessionId, bucket.getAvailableTokens());
    }

    /**
     * Create a token bucket for chat endpoint with configured limits.
     */
    private Bucket createChatBucket(String sessionId) {
        RateLimitConfig.EndpointConfig chatConfig = config.getChat();
        
        // Use Bandwidth.builder() for the newer API (non-deprecated)
        Bandwidth limit = Bandwidth.builder()
                .capacity(chatConfig.getCapacity())
                .refillIntervally(chatConfig.getRefillTokens(), Duration.ofSeconds(chatConfig.getRefillPeriodSeconds()))
                .build();
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create a token bucket for history endpoint with configured limits.
     */
    private Bucket createHistoryBucket(String sessionId) {
        RateLimitConfig.EndpointConfig historyConfig = config.getHistory();
        
        // Use Bandwidth.builder() for the newer API (non-deprecated)
        Bandwidth limit = Bandwidth.builder()
                .capacity(historyConfig.getCapacity())
                .refillIntervally(historyConfig.getRefillTokens(), Duration.ofSeconds(historyConfig.getRefillPeriodSeconds()))
                .build();
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Calculate retry-after time in seconds.
     * This estimates when the next token will be available.
     */
    private long calculateRetryAfter(Bucket bucket) {
        // Get the time until the next token refill
        // Bucket4j doesn't directly expose this, so we estimate based on refill period
        RateLimitConfig.EndpointConfig config = this.config.getChat();
        long refillPeriodSeconds = config.getRefillPeriodSeconds();
        long tokensPerRefill = config.getRefillTokens();
        
        // Estimate: if we need tokens, wait at most one refill period
        // In practice, this will be less, but this is a safe estimate
        return Math.max(1, refillPeriodSeconds / tokensPerRefill);
    }

    /**
     * Get remaining tokens for a session (for debugging/monitoring).
     */
    public int getRemainingChatTokens(String sessionId) {
        Bucket bucket = chatBuckets.get(sessionId);
        if (bucket == null) {
            return config.getChat().getCapacity();
        }
        return (int) bucket.getAvailableTokens();
    }

    /**
     * Clean up buckets for inactive sessions (called by session cleanup).
     */
    public void cleanupSession(String sessionId) {
        chatBuckets.remove(sessionId);
        historyBuckets.remove(sessionId);
        log.debug("Cleaned up rate limit buckets for session: {}", sessionId);
    }
}

