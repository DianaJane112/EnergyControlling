-- =====================================================
-- 计费水量月报表 — 补充更多测试数据
-- 用途：为计费水量月报表提供更多月份的数据
-- 覆盖：2025年7月 ~ 2025年12月（6个月）+ 扩展 2026年6月到今天
-- 执行：source migration_billing_water_more_data.sql
-- 修改日期：2026-06-17
-- 修改内容：表从 t_water_meter/t_water_meter_reading 迁移到统一表 t_meter/t_meter_reading
-- =====================================================
USE energy_platform;

-- 清理可能存在的同名存储过程
DROP PROCEDURE IF EXISTS sp_gen_billing_water_data;

DELIMITER //

CREATE PROCEDURE sp_gen_billing_water_data()
BEGIN
    DECLARE v_date DATE;
    DECLARE v_end_date DATE DEFAULT '2026-06-11';
    DECLARE v_day_of_week INT;
    DECLARE v_is_weekend INT;
    DECLARE v_meter_id BIGINT;
    DECLARE v_customer_id BIGINT;
    DECLARE v_device_no VARCHAR(64);
    DECLARE v_meter_name VARCHAR(128);
    DECLARE v_base_usage DECIMAL(18,4);
    DECLARE v_price DECIMAL(10,4);
    DECLARE v_daily_usage DECIMAL(18,4);
    DECLARE v_hour INT;
    DECLARE v_hour_usage DECIMAL(18,4);
    DECLARE v_record_time DATETIME;
    DECLARE v_done INT DEFAULT 0;

    -- 游标：遍历所有水表
    DECLARE meter_cursor CURSOR FOR
        SELECT id, COALESCE(customer_id, 0), meteraddress, COALESCE(name, '')
        FROM t_meter WHERE meter_type = 0 AND is_deleted = 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SET v_date = '2025-07-01';

    -- 外层循环：按天
    WHILE v_date <= v_end_date DO
        SET v_day_of_week = DAYOFWEEK(v_date);
        SET v_is_weekend = IF(v_day_of_week IN (1, 7), 1, 0);

        OPEN meter_cursor;
        SET v_done = 0;

        -- 内层循环：按水表
        meter_loop: LOOP
            FETCH meter_cursor INTO v_meter_id, v_customer_id, v_device_no, v_meter_name;
            IF v_done THEN LEAVE meter_loop; END IF;

            -- 跳过已删除的水表(1999)
            IF v_meter_id = 1999 THEN ITERATE meter_loop; END IF;

            -- ── 根据商户类型确定基础用水量和单价 ──
            SET v_base_usage = 0;
            SET v_price = 3.40;

            CASE v_customer_id
                WHEN 2 THEN
                    SET v_base_usage = IF(v_meter_id = 1001, 35, 20);
                    SET v_price = 3.80;
                WHEN 1 THEN
                    SET v_base_usage = 2;
                    SET v_price = 3.20;
                WHEN 5 THEN
                    SET v_base_usage = 5;
                    SET v_price = 3.40;
                WHEN 8 THEN
                    SET v_base_usage = IF(v_meter_id = 1005, 12, 6);
                    SET v_price = 3.40;
                WHEN 12 THEN
                    SET v_base_usage = 12;
                    SET v_price = 0.00;
                WHEN 3 THEN
                    SET v_base_usage = 3;
                    SET v_price = 3.30;
                WHEN 4 THEN
                    SET v_base_usage = 1.5;
                    SET v_price = 3.20;
                WHEN 9 THEN
                    SET v_base_usage = 6;
                    SET v_price = 3.40;
                WHEN 13 THEN
                    SET v_base_usage = 15;
                    SET v_price = 3.80;
                WHEN 6 THEN
                    SET v_base_usage = IF(v_meter_id = 1201, 40, 25);
                    SET v_price = 3.80;
                WHEN 7 THEN
                    SET v_base_usage = 18;
                    SET v_price = 3.60;
                WHEN 15 THEN
                    SET v_base_usage = IF(v_meter_id = 1204, 30, 15);
                    SET v_price = 3.80;
                WHEN 10 THEN
                    SET v_base_usage = 12;
                    SET v_price = 3.40;
                WHEN 11 THEN
                    SET v_base_usage = IF(v_meter_id = 1302, 55, 25);
                    SET v_price = 3.60;
                WHEN 14 THEN
                    SET v_base_usage = 4;
                    SET v_price = 3.30;
            END CASE;

            -- ── 周末调整 ──
            IF v_is_weekend = 1 THEN
                CASE v_customer_id
                    WHEN 8 THEN SET v_base_usage = v_base_usage * 0.15;
                    WHEN 10 THEN SET v_base_usage = v_base_usage * 0.25;
                    WHEN 6 THEN SET v_base_usage = v_base_usage * 1.10;
                    WHEN 13 THEN SET v_base_usage = v_base_usage * 0.85;
                    WHEN 14 THEN SET v_base_usage = v_base_usage * 0.50;
                    WHEN 15 THEN SET v_base_usage = v_base_usage * 0.80;
                    ELSE SET v_base_usage = v_base_usage * 0.90;
                END CASE;
            END IF;

            -- ── 随机扰动（±25%） ──
            SET v_daily_usage = v_base_usage * (0.75 + RAND() * 0.50);
            IF v_daily_usage < 0.5 THEN SET v_daily_usage = 0.5 + RAND() * 1.0; END IF;

            -- ── 按小时生成24条/天读数 ──
            SET v_hour = 0;
            WHILE v_hour < 24 DO
                SET v_record_time = CONCAT(v_date, ' ', LPAD(v_hour, 2, '0'), ':00:00');

                SET v_hour_usage = CASE
                    WHEN v_hour BETWEEN 6 AND 9   THEN v_daily_usage / 24 * 1.3
                    WHEN v_hour BETWEEN 11 AND 14 THEN v_daily_usage / 24 * 1.5
                    WHEN v_hour BETWEEN 17 AND 21 THEN v_daily_usage / 24 * 1.4
                    WHEN v_hour BETWEEN 0 AND 5   THEN v_daily_usage / 24 * 0.3
                    ELSE v_daily_usage / 24 * 0.8
                END;

                INSERT IGNORE INTO t_meter_reading (meter_id, timestamp, kwh, read_val)
                VALUES (v_meter_id, v_record_time,
                        ROUND(v_hour_usage, 4),
                        ROUND(v_hour_usage * v_price, 4));

                SET v_hour = v_hour + 1;
            END WHILE;

        END LOOP meter_loop;

        CLOSE meter_cursor;
        SET v_date = DATE_ADD(v_date, INTERVAL 1 DAY);
    END WHILE;

END //

DELIMITER ;

-- ═══════════════════════════════════════════════════════
-- 执行存储过程
-- ═══════════════════════════════════════════════════════
CALL sp_gen_billing_water_data();

-- 清理
DROP PROCEDURE IF EXISTS sp_gen_billing_water_data;

-- ═══════════════════════════════════════════════════════
-- 数据验证
-- ═══════════════════════════════════════════════════════
SELECT '═══ 计费水量月报 — 月汇总验证 ═══' AS info;

SELECT
    DATE_FORMAT(timestamp, '%Y-%m') AS 月份,
    COUNT(DISTINCT DATE(timestamp)) AS 有效天数,
    COUNT(DISTINCT meter_id) AS 活跃水表数,
    ROUND(SUM(kwh), 2) AS 月用水量_m3,
    ROUND(SUM(read_val), 2) AS 月水费_元,
    ROUND(SUM(read_val) / NULLIF(SUM(kwh), 0), 2) AS 均价_元每吨
FROM t_meter_reading
WHERE timestamp >= '2025-07-01'
GROUP BY DATE_FORMAT(timestamp, '%Y-%m')
ORDER BY 月份;

SELECT '═══ 总记录数验证 ═══' AS info;
SELECT COUNT(*) AS 水表读数总条数 FROM t_meter_reading;
SELECT COUNT(DISTINCT DATE(timestamp)) AS 有数据天数 FROM t_meter_reading;
SELECT MIN(timestamp) AS 最早记录, MAX(timestamp) AS 最晚记录 FROM t_meter_reading;
