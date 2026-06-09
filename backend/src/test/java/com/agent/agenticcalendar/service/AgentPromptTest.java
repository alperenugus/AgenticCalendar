package com.agent.agenticcalendar.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Pure unit tests for the agent system prompt.
 *
 * Regression guard: the prompt is a text block passed through {@code .formatted(...)},
 * so any literal '%' must be escaped as '%%'. An unescaped '%' (e.g. in a "-5.46%"
 * example) throws a FormatException at runtime for EVERY chat message.
 */
class AgentPromptTest {

    @Test
    void buildSystemPrompt_doesNotThrow_andContainsToolsAndDate() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 9, 14, 30);

        assertThatCode(() -> AgentService.buildSystemPrompt(now)).doesNotThrowAnyException();

        String prompt = AgentService.buildSystemPrompt(now);
        // Calendar + markets tools are advertised to the model.
        assertThat(prompt).contains("getStockQuote");
        assertThat(prompt).contains("getMarketSummary");
        assertThat(prompt).contains("createEvent");
        // The current date is injected (relative-date resolution depends on it).
        assertThat(prompt).contains("2026");
        // The escaped percent renders as a single '%' in the final prompt.
        assertThat(prompt).contains("-5.46%");
    }
}
