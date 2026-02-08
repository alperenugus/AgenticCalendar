package com.agent.appointmentscheduler.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    // Ollama configuration (for local development)
    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.model:llama3.1}")
    private String ollamaModel;

    @Value("${langchain4j.ollama.temperature:0.7}")
    private Double ollamaTemperature;

    // Groq configuration (uses OpenAI-compatible API)
    @Value("${langchain4j.groq.api-key:}")
    private String groqApiKey;

    @Value("${langchain4j.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${langchain4j.groq.temperature:0.7}")
    private Double groqTemperature;

    // LLM Provider selection
    @Value("${langchain4j.provider:groq}")
    private String provider;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Default to Groq (works both locally and on Railway)
        // Use Ollama only if explicitly set via LANGCHAIN4J_PROVIDER=ollama
        if ("ollama".equalsIgnoreCase(provider)) {
            return OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModel)
                    .temperature(ollamaTemperature)
                    .build();
        } else {
            // Default to Groq (uses OpenAI-compatible API)
            // If API key is not set, will fail with clear error message
            return OpenAiChatModel.builder()
                    .apiKey(groqApiKey)
                    .baseUrl("https://api.groq.com/openai/v1")  // Groq's OpenAI-compatible endpoint
                    .modelName(groqModel)
                    .temperature(groqTemperature)
                    .build();
        }
    }
}

