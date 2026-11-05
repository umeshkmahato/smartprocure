package com.smartprocure.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * General-purpose procurement AI assistant.
 * Answers questions about procurement processes, policies, and best practices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient.Builder chatClientBuilder;

    private static final String SYSTEM_PROMPT = """
            You are SmartProcure Assistant, an expert AI for procurement and vendor management.
            You help users with:
            - Procurement processes and best practices
            - Vendor evaluation criteria
            - Purchase order management
            - Invoice validation
            - Approval workflows
            Keep answers concise, professional, and actionable.
            If a question is outside procurement scope, politely redirect.
            """;

    /**
     * Sends a user message to the LLM and returns the assistant's reply.
     *
     * @param userMessage the user's question or request
     * @return the assistant's response text
     */
    public String chat(String userMessage) {
        try {
            return chatClientBuilder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("AI chat error: {}", ex.getMessage());
            return "The AI assistant is temporarily unavailable. Please try again later.";
        }
    }
}
