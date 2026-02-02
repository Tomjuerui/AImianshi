-- 数据库初始化脚本（兼容 H2 和 MySQL）

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100),
    `password_hash` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_username` ON `user` (`username`);

-- 简历表
CREATE TABLE IF NOT EXISTS `resume` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `file_url` VARCHAR(500),
    `raw_text` TEXT,
    `extracted_json` VARCHAR(5000),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_user_id` ON `resume` (`user_id`);

-- 面试会话表
CREATE TABLE IF NOT EXISTS `interview_session` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT,
    `resume_id` BIGINT,
    `duration_minutes` INT NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `current_stage` VARCHAR(20),
    `stage_plan_json` VARCHAR(5000),
    `started_at` DATETIME,
    `ended_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_user_id` ON `interview_session` (`user_id`);
CREATE INDEX IF NOT EXISTS `idx_resume_id` ON `interview_session` (`resume_id`);
CREATE INDEX IF NOT EXISTS `idx_status` ON `interview_session` (`status`);

-- 面试对话轮次表
CREATE TABLE IF NOT EXISTS `interview_turn` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content_text` TEXT NOT NULL,
    `audio_url` VARCHAR(500),
    `token_usage` VARCHAR(100),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_session_id` ON `interview_turn` (`session_id`);
CREATE INDEX IF NOT EXISTS `idx_created_at` ON `interview_turn` (`created_at`);

-- 报告表
CREATE TABLE IF NOT EXISTS `report` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `session_id` BIGINT NOT NULL UNIQUE,
    `overall_score` INT NOT NULL,
    `summary` TEXT,
    `strengths` TEXT,
    `weaknesses` TEXT,
    `suggestions` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS `idx_session_id` ON `report` (`session_id`);
