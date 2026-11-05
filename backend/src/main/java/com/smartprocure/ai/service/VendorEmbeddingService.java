package com.smartprocure.ai.service;

import com.smartprocure.vendor.entity.Vendor;
import com.smartprocure.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Embeds vendor profiles into the pgvector store so they can be retrieved
 * via semantic similarity search during AI-powered recommendations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorEmbeddingService {

    private final VectorStore   vectorStore;
    private final EmbeddingModel embeddingModel;
    private final VendorRepository vendorRepository;

    /**
     * Builds a plain-text representation of a vendor suitable for embedding.
     */
    public static String buildVendorContent(Vendor v) {
        return String.format(
            "Vendor: %s. Category: %s. Country: %s. Status: %s. " +
            "Rating: %s. Currency: %s. Tax ID: %s.",
            v.getName(),
            v.getCategory()  != null ? v.getCategory()  : "N/A",
            v.getCountry()   != null ? v.getCountry()   : "N/A",
            v.getStatus(),
            v.getRating()    != null ? v.getRating()    : "N/A",
            v.getCurrency()  != null ? v.getCurrency()  : "N/A",
            v.getTaxId()     != null ? v.getTaxId()     : "N/A"
        );
    }

    /**
     * Embeds a single vendor and upserts it in the vector store.
     * Called asynchronously when a vendor is created or updated.
     */
    @Async
    public void embedVendor(Vendor vendor) {
        try {
            String content = buildVendorContent(vendor);
            Document doc = new Document(
                content,
                Map.of("vendorId",   String.valueOf(vendor.getId()),
                       "vendorName", vendor.getName(),
                       "category",   vendor.getCategory()  != null ? vendor.getCategory()  : "",
                       "country",    vendor.getCountry()   != null ? vendor.getCountry()   : "",
                       "status",     vendor.getStatus().name())
            );
            vectorStore.add(List.of(doc));
            log.debug("Embedded vendor id={} name={}", vendor.getId(), vendor.getName());
        } catch (Exception ex) {
            log.warn("Failed to embed vendor id={}: {}", vendor.getId(), ex.getMessage());
        }
    }

    /**
     * Bulk-embeds all APPROVED vendors. Run once on demand via the AI controller.
     */
    @Async
    @Transactional(readOnly = true)
    public void embedAllApprovedVendors() {
        log.info("Starting bulk vendor embedding…");
        var vendors = vendorRepository.findAll().stream()
            .filter(v -> "APPROVED".equals(v.getStatus().name()))
            .toList();

        vendors.forEach(this::embedVendor);
        log.info("Bulk embedding triggered for {} vendors", vendors.size());
    }
}
