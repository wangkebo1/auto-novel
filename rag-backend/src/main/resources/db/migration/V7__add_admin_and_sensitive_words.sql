-- 添加管理员角色
INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', '管理员') ON CONFLICT (name) DO NOTHING;

-- 创建敏感词表
CREATE TABLE IF NOT EXISTS sensitive_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensitive_word ON sensitive_words(word);

-- 插入默认敏感词
INSERT INTO sensitive_words (word, category) VALUES
('习近平', '政治'),
('毛泽东', '政治'),
('邓小平', '政治'),
('江泽民', '政治'),
('胡锦涛', '政治'),
('杀人', '暴力'),
('自杀', '暴力'),
('色情', '色情'),
('赌博', '违法'),
('毒品', '违法'),
('法轮功', '其他'),
('六四', '其他'),
('台独', '其他'),
('藏独', '其他')
ON CONFLICT (word) DO NOTHING;
