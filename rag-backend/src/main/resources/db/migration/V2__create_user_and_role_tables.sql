-- 创建用户表
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_email ON users(email);

-- 创建角色表
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200)
);

-- 创建用户角色关联表
CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- 插入默认角色
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', '系统管理员，拥有所有权限'),
    ('ROLE_USER', '普通用户，可管理自己的知识库');

-- 插入默认管理员账户（密码: admin123, BCrypt加密）
INSERT INTO users (username, email, password, nickname, enabled) VALUES
    ('admin', 'admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', TRUE);

-- 关联管理员角色
INSERT INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

-- 添加知识库表的用户关联字段（先添加可空列）
ALTER TABLE knowledge_base ADD COLUMN user_id BIGINT;

-- 将现有知识库分配给管理员
UPDATE knowledge_base SET user_id = (SELECT id FROM users WHERE username = 'admin');

-- 设置为非空约束
ALTER TABLE knowledge_base ALTER COLUMN user_id SET NOT NULL;

-- 添加外键约束
ALTER TABLE knowledge_base ADD CONSTRAINT fk_kb_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_kb_user_id ON knowledge_base(user_id);
