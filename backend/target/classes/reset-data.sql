-- ========================================
-- 重置演示数据脚本
-- 执行此脚本清空业务数据，保留表结构
-- 重启后端应用后会自动重新初始化演示数据
-- ========================================

USE scf_db;

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有业务表
TRUNCATE TABLE transfer_record;
TRUNCATE TABLE financing_record;
TRUNCATE TABLE receivable;
TRUNCATE TABLE user;
TRUNCATE TABLE enterprise;

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 完成提示
SELECT '数据已清空，请重启后端应用以重新初始化演示数据' AS message;
