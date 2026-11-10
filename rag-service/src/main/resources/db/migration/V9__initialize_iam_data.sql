-- 初始化角色。
INSERT INTO iam_role (id, code, name) VALUES
    ('00000000-0000-0000-0001-000000000001', 'ROLE_USER', '普通用户'),
    ('00000000-0000-0000-0001-000000000002', 'ROLE_ADMIN', '管理员');

-- 初始化权限。
INSERT INTO iam_permission (id, code, name) VALUES
    ('00000000-0000-0000-0002-000000000001', 'rag:knowledge-base:create', '创建知识库'),
    ('00000000-0000-0000-0002-000000000002', 'rag:knowledge-base:read', '查看知识库'),
    ('00000000-0000-0000-0002-000000000003', 'rag:document:upload', '上传文档'),
    ('00000000-0000-0000-0002-000000000004', 'rag:document:delete', '删除文档'),
    ('00000000-0000-0000-0002-000000000005', 'rag:document:count', '查询文档数量'),
    ('00000000-0000-0000-0002-000000000006', 'rag:chat:use', '使用 RAG 问答'),
    ('00000000-0000-0000-0002-000000000007', 'identity:user:manage', '用户管理'),
    ('00000000-0000-0000-0002-000000000008', 'identity:role:manage', '角色管理'),
    ('00000000-0000-0000-0002-000000000009', 'identity:permission:manage', '权限管理');

-- 普通用户和管理员均具有知识库、文档及问答权限。
INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
CROSS JOIN iam_permission p
WHERE r.code IN ('ROLE_USER', 'ROLE_ADMIN')
  AND p.code IN (
      'rag:knowledge-base:create',
      'rag:knowledge-base:read',
      'rag:document:upload',
      'rag:document:delete',
      'rag:document:count',
      'rag:chat:use'
  );

-- 用户、角色和权限管理仅授予管理员。
INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM iam_role r
CROSS JOIN iam_permission p
WHERE r.code = 'ROLE_ADMIN'
  AND p.code IN (
      'identity:user:manage',
      'identity:role:manage',
      'identity:permission:manage'
  );
