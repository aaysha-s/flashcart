CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store (
                              id        UUID PRIMARY KEY,
                              content   TEXT,
                              metadata  JSON,
                              embedding VECTOR(768)
);

CREATE INDEX idx_vector_store_embedding
    ON vector_store USING hnsw (embedding vector_cosine_ops);