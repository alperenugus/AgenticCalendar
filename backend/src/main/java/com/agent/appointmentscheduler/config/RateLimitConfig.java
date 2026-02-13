package com.agent.appointmentscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    private EndpointConfig chat = new EndpointConfig();
    private EndpointConfig history = new EndpointConfig();

    public EndpointConfig getChat() {
        return chat;
    }

    public void setChat(EndpointConfig chat) {
        this.chat = chat;
    }

    public EndpointConfig getHistory() {
        return history;
    }

    public void setHistory(EndpointConfig history) {
        this.history = history;
    }

    public static class EndpointConfig {
        private int capacity = 20;
        private int refillTokens = 10;
        private int refillPeriodSeconds = 60;

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public int getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }

        public void setRefillPeriodSeconds(int refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}

