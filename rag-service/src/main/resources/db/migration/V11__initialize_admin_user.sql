-- 初始化管理员，仅保存 BCrypt（强度 12）密码哈希。
-- 已存在同名用户时跳过，不重置其密码、状态或权限。密码为：qwe123
INSERT INTO iam_user (id, username, password_hash, nickname, status)
VALUES (
    '00000000-0000-0000-0003-000000000001',
    'admin',
    '$2a$12$OtgmBta8SiCImuFuM.6JWek/OLvUwYrFcudG5EHzePORDwsGk2Arq',
    '管理员',
    'ENABLED'
)
ON CONFLICT (LOWER(username)) DO NOTHING;

-- 仅为本脚本初始化的用户分配管理员角色，不提升其他同名账号的权限。
INSERT INTO iam_user_role (user_id, role_id)
SELECT u.id, r.id
FROM iam_user u
CROSS JOIN iam_role r
WHERE u.id = '00000000-0000-0000-0003-000000000001'
  AND LOWER(u.username) = 'admin'
  AND r.code = 'ROLE_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
