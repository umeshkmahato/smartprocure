-- ============================================================
-- V18 — Enable pgvector extension + vendor embeddings table
-- Phase 10: Spring AI / RAG
-- ============================================================

-- Enable pgvector (requires pgvector package installed in Postgres image)
CREATE EXTENSION IF NOT EXISTS vector;

-- Stores Ollama nomic-embed-text embeddings (768 dimensions) for each vendor
CREATE TABLE IF NOT EXISTS vendor_embeddings (
    id          BIGSERIAL PRIMARY KEY,
    vendor_id   BIGINT        NOT NULL UNIQUE REFERENCES vendors(id) ON DELETE CASCADE,
    embedding   vector(768)   NOT NULL,
    content     TEXT          NOT NULL,   -- raw text that was embedded (for audit)
    embedded_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- HNSW index for fast approximate nearest-neighbour search
CREATE INDEX IF NOT EXISTS idx_vendor_embeddings_hnsw
    ON vendor_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
