CREATE TABLE chapter_versions (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    title VARCHAR(200),
    outline TEXT,
    content TEXT,
    summary TEXT,
    notes TEXT,
    word_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OUTLINE',
    completed_at TIMESTAMP,
    source VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (chapter_id, version_number)
);

CREATE INDEX idx_chapter_versions_chapter_id ON chapter_versions(chapter_id);
