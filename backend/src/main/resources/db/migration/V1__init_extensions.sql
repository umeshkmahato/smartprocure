-- ============================================================
-- SmartProcure – Phase 1 Foundation Migration
-- V1__init_extensions.sql
--
-- Enables PostgreSQL extensions required by the application.
-- Must run on: pgvector/pgvector:pg16 (Docker image) or any
-- PostgreSQL 16+ instance with the pgvector extension installed.
-- ============================================================

-- UUID generation (uuid_generate_v4(), etc.)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Cryptographic functions (gen_random_bytes, crypt, etc.)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Vector similarity search — required by Spring AI RAG pipeline (Phase 10).
-- Provided by the pgvector/pgvector:pg16 Docker image.
CREATE EXTENSION IF NOT EXISTS vector;
