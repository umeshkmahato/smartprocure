package com.smartprocure.ai.controller;

import com.smartprocure.ai.dto.ChatRequest;
import com.smartprocure.ai.dto.RecommendRequest;
import com.smartprocure.ai.service.AiChatService;
import com.smartprocure.ai.service.VendorEmbeddingService;
import com.smartprocure.ai.service.VendorRecommendationService;
import com.smartprocure.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API for Spring AI features (Phase 10).
 *
 * <pre>
 * POST /api/v1/ai/chat      → general procurement assistant (authenticated)
 * POST /api/v1/ai/recommend → RAG-powered vendor recommendations (authenticated)
 * GET  /api/v1/ai/similar   → vector similarity search, no LLM (authenticated)
 * POST /api/v1/ai/embed     → trigger bulk vendor embedding (ADMIN only)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService              chatService;
    private final VendorRecommendationService recommendationService;
    private final VendorEmbeddingService      embeddingService;

    // ── Chat ──────────────────────────────────────────────────────────────────

    /**
     * General-purpose procurement AI assistant.
     * Answers questions about procurement processes, policies, and best practices.
     */
    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> chat(
            @Valid @RequestBody ChatRequest req) {

        String reply = chatService.chat(req.message());
        return ResponseEntity.ok(ApiResponse.success(reply, "Chat response generated"));
    }

    // ── Vendor Recommendations ────────────────────────────────────────────────

    /**
     * Full RAG pipeline: embed the query → pgvector similarity search →
     * LLM-ranked recommendation text.
     *
     * <p>Defaults: topK=5, filterStatus="APPROVED".
     */
    @PostMapping("/recommend")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> recommend(
            @Valid @RequestBody RecommendRequest req) {

        int    topK         = req.topK()         != null ? req.topK()         : 5;
        String filterStatus = req.filterStatus() != null ? req.filterStatus() : "APPROVED";

        String result = recommendationService.recommend(req.query(), topK, filterStatus);
        return ResponseEntity.ok(ApiResponse.success(result, "Recommendation generated"));
    }

    /**
     * Fast vector-only similarity search — no LLM call.
     * Returns structured vendor results with cosine similarity scores.
     */
    @GetMapping("/similar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<VendorRecommendationService.SimilarVendorResult>>> similar(
            @RequestParam                              String query,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int topK) {

        List<VendorRecommendationService.SimilarVendorResult> results =
                recommendationService.similarVendors(query, topK);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ── Embedding Management ──────────────────────────────────────────────────

    /**
     * Admin-only: triggers asynchronous bulk embedding of all APPROVED vendors
     * into the pgvector store. Returns immediately; embedding runs in background.
     */
    @PostMapping("/embed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> embedAll() {
        embeddingService.embedAllApprovedVendors();
        return ResponseEntity.ok(
                ApiResponse.success("Bulk embedding triggered asynchronously.", "Embedding started"));
    }
}
