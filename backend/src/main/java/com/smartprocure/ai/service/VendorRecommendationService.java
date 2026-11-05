package com.smartprocure.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG pipeline: retrieves the most relevant vendor documents from pgvector,
 * then passes them as context to the LLM to generate a ranked recommendation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorRecommendationService {

    private final VectorStore  vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    private static final String SYSTEM_PROMPT = """
            You are SmartProcure, an AI procurement assistant.
            Your job is to recommend the best vendors based on the user's requirements.
            Use ONLY the vendor context provided — do not invent data.
            Respond with a concise ranked list (1-5 vendors) explaining why each is a good fit.
            Format: "1. VendorName — reason". Keep each reason under 2 sentences.
            """;

    /**
     * Full RAG pipeline:
     * 1. Embed the user query and search pgvector for similar vendor profiles.
     * 2. Build a prompt with the retrieved vendors as context.
     * 3. Call the LLM and return its recommendation text.
     *
     * @param query        natural-language requirement (e.g. "reliable IT hardware supplier in Asia")
     * @param topK         number of similar vendors to retrieve (default 5)
     * @param filterStatus optional vendor status filter ("APPROVED", etc.)
     */
    public String recommend(String query, int topK, String filterStatus) {
        // ── Step 1: Semantic search in pgvector ───────────────────────────────
        SearchRequest searchRequest;
        if (filterStatus != null && !filterStatus.isBlank()) {
            var filter = new FilterExpressionBuilder().eq("status", filterStatus).build();
            searchRequest = SearchRequest.query(query).withTopK(topK).withFilterExpression(filter);
        } else {
            searchRequest = SearchRequest.query(query).withTopK(topK);
        }

        var docs = vectorStore.similaritySearch(searchRequest);

        if (docs == null || docs.isEmpty()) {
            return "No matching vendors found in the database. Please ensure vendors have been embedded first.";
        }

        // ── Step 2: Build context from retrieved docs ─────────────────────────
        String vendorContext = docs.stream()
            .map(doc -> "• " + doc.getText())
            .collect(Collectors.joining("\n"));

        // ── Step 3: Call LLM with RAG context ────────────────────────────────
        String userMessage = String.format("""
                Requirement: %s

                Available vendors:
                %s

                Please recommend the best vendors for this requirement.
                """, query, vendorContext);

        try {
            return chatClientBuilder.build()
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(userMessage)
                .call()
                .content();
        } catch (Exception ex) {
            log.error("LLM call failed: {}", ex.getMessage());
            return "AI recommendation is temporarily unavailable. Retrieved vendors: \n" + vendorContext;
        }
    }

    /**
     * Simple vendor similarity search without LLM summarisation.
     * Returns the raw matched vendor names + metadata for display in the UI.
     */
    public List<SimilarVendorResult> similarVendors(String query, int topK) {
        var docs = vectorStore.similaritySearch(
            SearchRequest.query(query).withTopK(topK)
        );
        if (docs == null) return List.of();

        return docs.stream()
            .map(doc -> new SimilarVendorResult(
                Long.parseLong(doc.getMetadata().getOrDefault("vendorId", "0").toString()),
                doc.getMetadata().getOrDefault("vendorName", "").toString(),
                doc.getMetadata().getOrDefault("category",   "").toString(),
                doc.getMetadata().getOrDefault("country",    "").toString(),
                doc.getScore() != null ? doc.getScore() : 0.0
            ))
            .collect(Collectors.toList());
    }

    /** Lightweight DTO for similar vendor results (no LLM call needed). */
    public record SimilarVendorResult(
        long   vendorId,
        String vendorName,
        String category,
        String country,
        double score
    ) {}
}
