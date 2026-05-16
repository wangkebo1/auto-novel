-- ============================================
-- V5: AI 视频生成功能 - 视频任务表
-- ============================================

CREATE TABLE video_tasks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    task_id VARCHAR(200) NOT NULL UNIQUE,        -- Kling API 返回的任务 ID
    model VARCHAR(50) NOT NULL DEFAULT 'kling-v2-5-turbo',
    prompt TEXT NOT NULL,                         -- 用户输入的视频描述
    status VARCHAR(30) NOT NULL DEFAULT 'queued', -- queued / in_progress / completed / failed
    duration INTEGER NOT NULL DEFAULT 5,          -- 视频时长: 5 或 10 秒
    size VARCHAR(20) NOT NULL DEFAULT '1280x720', -- 视频尺寸
    mode VARCHAR(10) NOT NULL DEFAULT 'std',      -- std / pro
    video_url TEXT,                                -- 生成完成后的视频下载 URL
    cover_url TEXT,                                -- 视频封面 URL
    error_message TEXT,                            -- 失败时的错误信息
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_video_tasks_user_id ON video_tasks(user_id);
CREATE INDEX idx_video_tasks_status ON video_tasks(status);
