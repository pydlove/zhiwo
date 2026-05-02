-- 兼容 MySQL 5.7 和 8.0，动态删除列
SET @dbname = DATABASE();

-- db_host
SELECT COUNT(*) INTO @exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'tu_server_config' AND COLUMN_NAME = 'db_host';
SET @sql = IF(@exists > 0, 'ALTER TABLE tu_server_config DROP COLUMN db_host', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- db_port
SELECT COUNT(*) INTO @exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'tu_server_config' AND COLUMN_NAME = 'db_port';
SET @sql = IF(@exists > 0, 'ALTER TABLE tu_server_config DROP COLUMN db_port', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- db_name
SELECT COUNT(*) INTO @exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'tu_server_config' AND COLUMN_NAME = 'db_name';
SET @sql = IF(@exists > 0, 'ALTER TABLE tu_server_config DROP COLUMN db_name', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- db_username
SELECT COUNT(*) INTO @exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'tu_server_config' AND COLUMN_NAME = 'db_username';
SET @sql = IF(@exists > 0, 'ALTER TABLE tu_server_config DROP COLUMN db_username', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- db_password
SELECT COUNT(*) INTO @exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'tu_server_config' AND COLUMN_NAME = 'db_password';
SET @sql = IF(@exists > 0, 'ALTER TABLE tu_server_config DROP COLUMN db_password', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- api_base_url
SELECT COUNT(*) INTO @exists FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = 'tu_server_config' AND COLUMN_NAME = 'api_base_url';
SET @sql = IF(@exists > 0, 'ALTER TABLE tu_server_config DROP COLUMN api_base_url', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
