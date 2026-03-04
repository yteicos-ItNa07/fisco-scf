-- ========================================
-- 供应链金融平台数据库初始化脚本
-- ========================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS scf_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE scf_db;

-- ========================================
-- 1. 企业表
-- ========================================
CREATE TABLE IF NOT EXISTS enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_code VARCHAR(18) NOT NULL UNIQUE COMMENT '统一社会信用代码',
    name VARCHAR(100) NOT NULL COMMENT '企业名称',
    legal_person VARCHAR(50) COMMENT '法人代表',
    phone VARCHAR(20) COMMENT '联系电话',
    address VARCHAR(200) COMMENT '企业地址',
    role VARCHAR(20) NOT NULL COMMENT '企业角色: CORE_ENTERPRISE, SUPPLIER, FINANCIAL_INST, REGULATOR',
    credit_score INT NOT NULL DEFAULT 700 COMMENT '信用评分 (0-1000)',
    blockchain_address VARCHAR(42) COMMENT '区块链地址',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    INDEX idx_role (role),
    INDEX idx_credit_code (credit_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表';

-- ========================================
-- 2. 用户表
-- ========================================
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    enterprise_id BIGINT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'ADMIN, MANAGER, USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at DATETIME,
    FOREIGN KEY (enterprise_id) REFERENCES enterprise(id),
    INDEX idx_username (username),
    INDEX idx_enterprise (enterprise_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 3. 应收账款通证表
-- ========================================
CREATE TABLE IF NOT EXISTS receivable (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_id BIGINT NOT NULL UNIQUE COMMENT '链上通证ID',
    parent_token_id BIGINT COMMENT '父通证ID（拆分时）',
    issuer_id BIGINT NOT NULL COMMENT '签发方（核心企业）',
    holder_id BIGINT NOT NULL COMMENT '当前持有方',
    face_value DECIMAL(18,2) NOT NULL COMMENT '票面金额',
    remaining_value DECIMAL(18,2) NOT NULL COMMENT '剩余可用金额',
    issue_date DATE NOT NULL COMMENT '签发日期',
    maturity_date DATE NOT NULL COMMENT '到期日期',
    annual_rate DECIMAL(6,4) NOT NULL COMMENT '年化利率',
    contract_hash VARCHAR(100) COMMENT '电子合同IPFS哈希',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED, CONFIRMED, FINANCED, PARTIALLY_FINANCED, SETTLED, EXPIRED, CANCELLED',
    is_splittable BOOLEAN NOT NULL DEFAULT TRUE,
    tx_hash VARCHAR(66) COMMENT '上链交易哈希',
    description VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    FOREIGN KEY (issuer_id) REFERENCES enterprise(id),
    FOREIGN KEY (holder_id) REFERENCES enterprise(id),
    INDEX idx_token_id (token_id),
    INDEX idx_issuer (issuer_id),
    INDEX idx_holder (holder_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应收账款通证表';

-- ========================================
-- 4. 融资记录表
-- ========================================
CREATE TABLE IF NOT EXISTS financing_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL UNIQUE COMMENT '链上记录ID',
    receivable_id BIGINT NOT NULL,
    financier_id BIGINT NOT NULL COMMENT '金融机构',
    borrower_id BIGINT NOT NULL COMMENT '借款人',
    financing_amount DECIMAL(18,2) NOT NULL COMMENT '融资金额',
    annual_rate DECIMAL(6,4) NOT NULL COMMENT '融资利率',
    discount_amount DECIMAL(18,2) NOT NULL COMMENT '折价金额',
    actual_amount DECIMAL(18,2) NOT NULL COMMENT '实际放款金额',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, DISBURSED, REPAID',
    is_repaid BOOLEAN NOT NULL DEFAULT FALSE,
    tx_hash VARCHAR(66),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disbursed_at DATETIME COMMENT '放款时间',
    repaid_at DATETIME COMMENT '还款时间',
    FOREIGN KEY (receivable_id) REFERENCES receivable(id),
    FOREIGN KEY (financier_id) REFERENCES enterprise(id),
    FOREIGN KEY (borrower_id) REFERENCES enterprise(id),
    INDEX idx_receivable (receivable_id),
    INDEX idx_financier (financier_id),
    INDEX idx_borrower (borrower_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融资记录表';

-- ========================================
-- 5. 流转记录表
-- ========================================
CREATE TABLE IF NOT EXISTS transfer_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_receivable_id BIGINT COMMENT '源通证',
    to_receivable_id BIGINT COMMENT '目标通证',
    from_enterprise_id BIGINT NOT NULL,
    to_enterprise_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'ISSUE, TRANSFER, SPLIT, FINANCING, SETTLEMENT',
    reason VARCHAR(200),
    tx_hash VARCHAR(66),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_receivable_id) REFERENCES receivable(id),
    FOREIGN KEY (to_receivable_id) REFERENCES receivable(id),
    FOREIGN KEY (from_enterprise_id) REFERENCES enterprise(id),
    FOREIGN KEY (to_enterprise_id) REFERENCES enterprise(id),
    INDEX idx_from_receivable (from_receivable_id),
    INDEX idx_to_receivable (to_receivable_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流转记录表';

-- ========================================
-- 初始数据：企业
-- 注意：blockchain_address 设为 NULL，真实地址由 DataInitializer 通过区块链生成
-- ========================================
INSERT INTO enterprise (credit_code, name, legal_person, phone, address, role, credit_score, blockchain_address, is_active, created_at) VALUES
-- 核心企业
('91110000MA01ABCD1X', '华为技术有限公司', '任正非', '13800138001', '广东省深圳市龙岗区坂田华为基地', 'CORE_ENTERPRISE', 950, NULL, TRUE, NOW()),
('91110000MA01ABCD2Y', '中国中车股份有限公司', '孙永才', '13800138002', '北京市海淀区中关村大街1号', 'CORE_ENTERPRISE', 920, NULL, TRUE, NOW()),

-- 供应商
('91110000MA01SUPP1A', '深圳市精密零件有限公司', '张明', '13900139001', '广东省深圳市宝安区工业园A区', 'SUPPLIER', 780, NULL, TRUE, NOW()),
('91110000MA01SUPP2B', '苏州电子元器件有限公司', '李华', '13900139002', '江苏省苏州市工业园区科技路88号', 'SUPPLIER', 750, NULL, TRUE, NOW()),
('91110000MA01SUPP3C', '东莞市模具制造有限公司', '王强', '13900139003', '广东省东莞市长安镇工业区', 'SUPPLIER', 720, NULL, TRUE, NOW()),
('91110000MA01SUPP4D', '杭州软件科技有限公司', '赵伟', '13900139004', '浙江省杭州市西湖区文三路100号', 'SUPPLIER', 800, NULL, TRUE, NOW()),

-- 金融机构
('91110000MA01BANK1F', '中国工商银行深圳分行', '陈行长', '13700137001', '广东省深圳市福田区深南大道1000号', 'FINANCIAL_INST', 980, NULL, TRUE, NOW()),
('91110000MA01BANK2G', '招商银行总行', '田惠宇', '13700137002', '广东省深圳市福田区招商银行大厦', 'FINANCIAL_INST', 970, NULL, TRUE, NOW()),

-- 监管机构
('91110000MA01REGU1H', '深圳市金融监督管理局', '刘局长', '13600136001', '广东省深圳市福田区市民中心', 'REGULATOR', 1000, NULL, TRUE, NOW());

-- ========================================
-- 初始数据：用户
-- 密码都是 123456，使用BCrypt加密
-- BCrypt hash for "123456": $2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa
-- ========================================
INSERT INTO user (username, password, real_name, phone, email, enterprise_id, role, is_active, created_at) VALUES
-- 系统管理员（不关联企业）
('admin', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '系统管理员', '13000000000', 'admin@scf.com', NULL, 'ADMIN', TRUE, NOW()),

-- 核心企业用户
('huawei_admin', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '华为管理员', '13800138001', 'admin@huawei.com', 1, 'MANAGER', TRUE, NOW()),
('zhongche_admin', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '中车管理员', '13800138002', 'admin@crrc.com', 2, 'MANAGER', TRUE, NOW()),

-- 供应商用户
('supplier1', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '精密零件管理员', '13900139001', 'admin@jingmi.com', 3, 'MANAGER', TRUE, NOW()),
('supplier2', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '电子元器件管理员', '13900139002', 'admin@dianzi.com', 4, 'MANAGER', TRUE, NOW()),
('supplier3', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '模具制造管理员', '13900139003', 'admin@moju.com', 5, 'MANAGER', TRUE, NOW()),
('supplier4', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '软件科技管理员', '13900139004', 'admin@ruanjian.com', 6, 'MANAGER', TRUE, NOW()),

-- 金融机构用户
('icbc_admin', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '工商银行管理员', '13700137001', 'admin@icbc.com', 7, 'MANAGER', TRUE, NOW()),
('cmb_admin', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '招商银行管理员', '13700137002', 'admin@cmb.com', 8, 'MANAGER', TRUE, NOW()),

-- 监管机构用户
('regulator', '$2a$10$EIV0hBbVjCg7wNqDGXKc4eUZxVrFMhV.0F4MFKR6L6mJHxrGpGYYa', '监管员', '13600136001', 'admin@jrjg.gov.cn', 9, 'MANAGER', TRUE, NOW());

-- ========================================
-- 初始数据：应收账款通证（示例）
-- ========================================
INSERT INTO receivable (token_id, issuer_id, holder_id, face_value, remaining_value, issue_date, maturity_date, annual_rate, status, is_splittable, description, created_at) VALUES
-- 华为签发给供应商的应收账款
(1, 1, 3, 5000000.00, 5000000.00, '2026-01-15', '2026-07-15', 0.0600, 'CONFIRMED', TRUE, '华为2026年Q1零件采购款', NOW()),
(2, 1, 4, 3000000.00, 3000000.00, '2026-01-20', '2026-06-20', 0.0580, 'CONFIRMED', TRUE, '华为2026年Q1电子元器件采购款', NOW()),
(3, 1, 6, 2000000.00, 2000000.00, '2026-02-01', '2026-08-01', 0.0550, 'CONFIRMED', TRUE, '华为软件开发服务费', NOW()),

-- 中车签发给供应商的应收账款
(4, 2, 5, 8000000.00, 8000000.00, '2026-01-10', '2026-07-10', 0.0620, 'CONFIRMED', TRUE, '中车2026年模具采购款', NOW()),
(5, 2, 3, 4500000.00, 4500000.00, '2026-02-05', '2026-08-05', 0.0600, 'CONFIRMED', TRUE, '中车精密零件采购款', NOW());

-- ========================================
-- 初始数据：流转记录
-- ========================================
INSERT INTO transfer_record (from_receivable_id, to_receivable_id, from_enterprise_id, to_enterprise_id, amount, type, reason, created_at) VALUES
(NULL, 1, 1, 3, 5000000.00, 'ISSUE', '核心企业签发应收账款', NOW()),
(NULL, 2, 1, 4, 3000000.00, 'ISSUE', '核心企业签发应收账款', NOW()),
(NULL, 3, 1, 6, 2000000.00, 'ISSUE', '核心企业签发应收账款', NOW()),
(NULL, 4, 2, 5, 8000000.00, 'ISSUE', '核心企业签发应收账款', NOW()),
(NULL, 5, 2, 3, 4500000.00, 'ISSUE', '核心企业签发应收账款', NOW());

-- ========================================
-- 查看初始化结果
-- ========================================
SELECT '企业数量' AS item, COUNT(*) AS count FROM enterprise
UNION ALL
SELECT '用户数量', COUNT(*) FROM user
UNION ALL
SELECT '应收账款数量', COUNT(*) FROM receivable
UNION ALL
SELECT '流转记录数量', COUNT(*) FROM transfer_record;
