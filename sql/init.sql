-- ===========================================
-- 私人聚合云盘系统 - 数据库初始化脚本
-- 数据库名: private_aggregator
-- ===========================================

CREATE DATABASE IF NOT EXISTS private_aggregator DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE private_aggregator;

-- ===========================================
-- 1. 用户表 (sys_user)
-- ===========================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username       VARCHAR(50)     NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(255)    NOT NULL COMMENT '密码(加密存储)',
    nickname        VARCHAR(50)     DEFAULT NULL COMMENT '昵称',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    avatar          VARCHAR(500)    DEFAULT NULL COMMENT '头像URL',
    total_space     BIGINT          DEFAULT 10737418240 COMMENT '总空间(字节,默认10GB)',
    used_space      BIGINT          DEFAULT 0 COMMENT '已使用空间(字节)',
    status          TINYINT         DEFAULT 1 COMMENT '状态: 0-禁用 1-正常 2-冻结',
    last_login_time DATETIME        DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip   VARCHAR(50)     DEFAULT NULL COMMENT '最后登录IP',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ===========================================
-- 2. 文件信息表 (file_info)
-- ===========================================
DROP TABLE IF EXISTS file_info;
CREATE TABLE file_info (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '文件ID',
    user_id             BIGINT          NOT NULL COMMENT '所属用户ID',
    parent_id           BIGINT          DEFAULT 0 COMMENT '父文件夹ID(0为根目录)',
    file_name           VARCHAR(255)    NOT NULL COMMENT '文件名称',
    file_path           VARCHAR(1000)   NOT NULL COMMENT '文件存储路径',
    file_size           BIGINT          DEFAULT 0 COMMENT '文件大小(字节)',
    file_type           VARCHAR(20)     DEFAULT NULL COMMENT '文件类型(扩展名)',
    storage_type        VARCHAR(20)     NOT NULL DEFAULT 'MINIO' COMMENT '存储类型: MINIO/ALIYUN/ONEDRIVE/CLOUD123',
    file_md5            VARCHAR(64)      DEFAULT NULL COMMENT '文件MD5值',
    is_folder           TINYINT         DEFAULT 0 COMMENT '是否文件夹: 0-文件 1-文件夹',
    file_status         TINYINT         DEFAULT 1 COMMENT '文件状态: 0-删除中 1-正常 2-回收站',
    download_count      INT             DEFAULT 0 COMMENT '下载次数',
    preview_count      INT             DEFAULT 0 COMMENT '预览次数',
    share_count        INT             DEFAULT 0 COMMENT '分享次数',
    version             INT             DEFAULT 1 COMMENT '文件版本号',
    is_private          TINYINT         DEFAULT 0 COMMENT '是否私密: 0-公开 1-私密',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_file_name (file_name),
    INDEX idx_storage_type (storage_type),
    INDEX idx_file_status (file_status),
    INDEX idx_created_at (created_at),
    INDEX idx_user_parent (user_id, parent_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件信息表';

-- ===========================================
-- 3. 存储绑定表 (storage_binding)
-- ===========================================
DROP TABLE IF EXISTS storage_binding;
CREATE TABLE storage_binding (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '绑定ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    storage_type        VARCHAR(20)     NOT NULL COMMENT '存储类型: ALIYUN/ONEDRIVE/CLOUD123',
    access_token        VARCHAR(500)    NOT NULL COMMENT '访问令牌(加密存储)',
    refresh_token       VARCHAR(500)    DEFAULT NULL COMMENT '刷新令牌(加密存储)',
    token_expire_time   DATETIME        DEFAULT NULL COMMENT '令牌过期时间',
    refresh_token_expire_time DATETIME   DEFAULT NULL COMMENT '刷新令牌过期时间',
    storage_user_id     VARCHAR(100)    DEFAULT NULL COMMENT '云盘用户ID',
    storage_user_name   VARCHAR(100)    DEFAULT NULL COMMENT '云盘用户名',
    total_space         BIGINT          DEFAULT 0 COMMENT '云盘总空间',
    used_space          BIGINT          DEFAULT 0 COMMENT '云盘已用空间',
    is_bind             TINYINT         DEFAULT 1 COMMENT '绑定状态: 0-解绑 1-绑定',
    bind_time           DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    last_sync_time      DATETIME        DEFAULT NULL COMMENT '最后同步时间',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    UNIQUE KEY uk_user_storage (user_id, storage_type),
    INDEX idx_user_id (user_id),
    INDEX idx_storage_type (storage_type),
    INDEX idx_is_bind (is_bind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='存储绑定表';

-- ===========================================
-- 4. 文件分享表 (file_share)
-- ===========================================
DROP TABLE IF EXISTS file_share;
CREATE TABLE file_share (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '分享ID',
    share_id            VARCHAR(32)     NOT NULL UNIQUE COMMENT '分享码(唯一标识)',
    file_id             BIGINT          NOT NULL COMMENT '被分享的文件ID',
    user_id             BIGINT          NOT NULL COMMENT '分享用户ID',
    share_password      VARCHAR(100)    DEFAULT NULL COMMENT '分享密码(为空则无需密码)',
    share_title         VARCHAR(200)    DEFAULT NULL COMMENT '分享标题',
    share_description   VARCHAR(500)    DEFAULT NULL COMMENT '分享描述',
    expire_type         TINYINT         DEFAULT 1 COMMENT '过期类型: 1-永久 2-自定义时间',
    expire_time         DATETIME        DEFAULT NULL COMMENT '过期时间',
    download_limit      INT             DEFAULT NULL COMMENT '下载次数限制(为空则不限制)',
    download_count      INT             DEFAULT 0 COMMENT '已下载次数',
    view_count          INT             DEFAULT 0 COMMENT '查看次数',
    is_public           TINYINT         DEFAULT 1 COMMENT '是否公开: 0-私密 1-公开',
    status              TINYINT         DEFAULT 1 COMMENT '状态: 0-已取消 1-有效 2-已过期',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_share_id (share_id),
    INDEX idx_file_id (file_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件分享表';

-- ===========================================
-- 5. 转存任务表 (transfer_task)
-- ===========================================
DROP TABLE IF EXISTS transfer_task;
CREATE TABLE transfer_task (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_no             VARCHAR(32)     NOT NULL UNIQUE COMMENT '任务编号',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    source_type         VARCHAR(20)     NOT NULL COMMENT '来源类型: BAIDU/ALIYUN/ONEDRIVE',
    source_url          VARCHAR(500)    NOT NULL COMMENT '来源链接',
    source_file_id      VARCHAR(100)    DEFAULT NULL COMMENT '来源文件ID',
    source_file_name    VARCHAR(255)    DEFAULT NULL COMMENT '来源文件名',
    source_file_size    BIGINT          DEFAULT 0 COMMENT '来源文件大小',
    extract_code        VARCHAR(20)     DEFAULT NULL COMMENT '提取码',
    target_path         VARCHAR(500)    DEFAULT '/import' COMMENT '目标路径',
    storage_type        VARCHAR(20)     DEFAULT 'MINIO' COMMENT '目标存储类型',
    status              TINYINT         DEFAULT 0 COMMENT '任务状态: 0-等待中 1-处理中 2-成功 3-失败 4-已取消',
    progress            INT             DEFAULT 0 COMMENT '进度百分比(0-100)',
    error_message       VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
    retry_count         INT             DEFAULT 0 COMMENT '重试次数',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    completed_at        DATETIME        DEFAULT NULL COMMENT '完成时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_task_no (task_no),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_source_type (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='转存任务表';

-- ===========================================
-- 6. 回收站表 (recycle_bin)
-- ===========================================
DROP TABLE IF EXISTS recycle_bin;
CREATE TABLE recycle_bin (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    file_id             BIGINT          NOT NULL COMMENT '原文件ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',
    file_name           VARCHAR(255)    NOT NULL COMMENT '文件名称',
    file_path           VARCHAR(1000)   NOT NULL COMMENT '文件存储路径',
    file_size           BIGINT          DEFAULT 0 COMMENT '文件大小',
    file_type           VARCHAR(20)     DEFAULT NULL COMMENT '文件类型',
    storage_type        VARCHAR(20)     NOT NULL COMMENT '存储类型',
    is_folder           TINYINT         DEFAULT 0 COMMENT '是否文件夹',
    delete_time         DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
    expire_time         DATETIME        DEFAULT NULL COMMENT '过期时间(30天后彻底删除)',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted             TINYINT         DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
    INDEX idx_user_id (user_id),
    INDEX idx_file_id (file_id),
    INDEX idx_delete_time (delete_time),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回收站表';

-- ===========================================
-- 初始化测试数据
-- ===========================================
INSERT INTO sys_user (username, password, nickname, email, total_space) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '管理员', 'admin@cloud.disk', 107374182400),
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '测试用户', 'test@cloud.disk', 10737418240);