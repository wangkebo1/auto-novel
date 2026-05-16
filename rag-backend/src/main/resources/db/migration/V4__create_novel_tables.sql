-- ============================================
-- V4: AI 小说生成功能 - 数据库表
-- ============================================

-- 小说项目表
CREATE TABLE novels (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    genre VARCHAR(50) NOT NULL,           -- 类型：玄幻、都市、科幻、言情等
    style VARCHAR(50),                    -- 风格：轻松、严肃、幽默等
    description TEXT,                     -- 简介
    world_setting TEXT,                   -- 世界观设定
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT / IN_PROGRESS / COMPLETED
    total_words INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_novels_user_id ON novels(user_id);

-- 角色设定表
CREATE TABLE novel_characters (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    role_type VARCHAR(30) NOT NULL DEFAULT 'SUPPORTING', -- PROTAGONIST / ANTAGONIST / SUPPORTING
    personality TEXT,                     -- 性格描述
    background TEXT,                      -- 背景故事
    appearance TEXT,                      -- 外貌描述
    relationships TEXT,                   -- 与其他角色的关系
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_novel_characters_novel_id ON novel_characters(novel_id);

-- 章节表
CREATE TABLE chapters (
    id BIGSERIAL PRIMARY KEY,
    novel_id BIGINT NOT NULL REFERENCES novels(id) ON DELETE CASCADE,
    chapter_number INT NOT NULL,
    title VARCHAR(200),
    outline TEXT,                         -- 本章大纲
    content TEXT,                         -- 正文内容
    summary TEXT,                         -- AI 生成的本章摘要（供后续章节参考）
    word_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'OUTLINE',  -- OUTLINE / GENERATING / GENERATED / EDITED
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(novel_id, chapter_number)
);

CREATE INDEX idx_chapters_novel_id ON chapters(novel_id);
