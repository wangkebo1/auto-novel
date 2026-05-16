-- 修复 email 允许为空
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- 修复 admin 密码 (password: 123456, BCrypt hash)
UPDATE users SET password = '$2a$10$aSPXMI.x1M0OFV1E22x3o.0tJwsW2cLxy285u48gPYTs3IZ5QCVkq'
WHERE username = 'admin';
