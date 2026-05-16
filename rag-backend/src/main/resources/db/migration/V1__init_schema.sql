-- ============================================================
-- V1: 初始化数据库表结构
-- ============================================================

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

-- 文档记录表
CREATE TABLE IF NOT EXISTS document_record (
    id                BIGSERIAL PRIMARY KEY,
    file_name         VARCHAR(255)  NOT NULL,
    file_path         VARCHAR(1000),
    file_hash         VARCHAR(64)   NOT NULL UNIQUE,
    file_size         BIGINT,
    file_type         VARCHAR(50),
    chunk_count       INTEGER       DEFAULT 0,
    status            VARCHAR(50)   DEFAULT 'PROCESSING',
    error_message     TEXT,
    knowledge_base_id BIGINT        REFERENCES knowledge_base (id),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
);

-- 索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_doc_hash ON document_record (file_hash);
CREATE INDEX IF NOT EXISTS idx_doc_kb ON document_record (knowledge_base_id);
