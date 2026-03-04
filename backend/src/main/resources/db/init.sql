-- 供应链金融平台数据库初始化脚本
-- 数据库: scf_db

CREATE DATABASE IF NOT EXISTS scf_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE scf_db;

-- 初始化演示数据（可选）

-- 1. 插入演示企业（blockchain_address 设为 NULL，真实地址由 DataInitializer 通过区块链生成）
INSERT INTO enterprise (credit_code, name, legal_person, phone, address, role, credit_score, blockchain_address, is_active, created_at) VALUES
('91110000000000001A', '汽车制造有限公司', '张三', '13800138001', '北京市朝阳区xxx路1号', 'CORE_ENTERPRISE', 850, NULL, 1, NOW()),
('91110000000000002B', '零部件供应商A', '李四', '13800138002', '上海市浦东新区xxx路2号', 'SUPPLIER', 720, NULL, 1, NOW()),
('91110000000000003C', '二级供应商B', '王五', '13800138003', '广州市天河区xxx路3号', 'SUPPLIER', 680, NULL, 1, NOW()),
('91110000000000004D', '某商业银行', '赵六', '13800138004', '深圳市南山区xxx路4号', 'FINANCIAL_INST', 900, NULL, 1, NOW());

-- 2. 插入演示用户（密码均为123456的BCrypt加密值）
INSERT INTO user (username, password, real_name, phone, email, enterprise_id, role, is_active, created_at) VALUES
('core_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '核心企业管理员', '13800138001', 'core@example.com', 1, 'MANAGER', 1, NOW()),
('supplier_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '供应商管理员', '13800138002', 'supplier@example.com', 2, 'MANAGER', 1, NOW()),
('bank_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '银行管理员', '13800138004', 'bank@example.com', 4, 'MANAGER', 1, NOW()),
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', '13800138000', 'admin@example.com', NULL, 'ADMIN', 1, NOW());
