-- =============================================================
-- 파티션 관리 프로시저
-- =============================================================
-- 사용법:
--   CALL create_partitions('2025-01-24', 7);  -- 2025-01-24부터 7일간 파티션 생성
--   CALL drop_old_partitions('2025-01-24');   -- 2025-01-24 기준 보관 기간 초과 파티션 삭제
-- =============================================================

DELIMITER //

-- -------------------------------------------------------------
-- 파티션 생성 프로시저
-- base_date부터 days_ahead일 뒤까지 일별 파티션을 미리 생성
-- 이미 존재하는 파티션은 건너뜀
-- 파티션명: p_YYYYMMDD (해당 날짜 데이터 포함)
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS create_partitions //

CREATE PROCEDURE create_partitions(IN base_date DATE, IN days_ahead INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE target_date DATE;
    DECLARE part_name VARCHAR(20);
    DECLARE bound_date VARCHAR(10);
    DECLARE cnt INT;

    WHILE i <= days_ahead DO
        SET target_date = base_date + INTERVAL i DAY;
        SET part_name = CONCAT('p_', DATE_FORMAT(target_date, '%Y%m%d'));
        SET bound_date = DATE_FORMAT(target_date + INTERVAL 1 DAY, '%Y-%m-%d');

        -- stats_rank_battle
        SELECT COUNT(*) INTO cnt FROM INFORMATION_SCHEMA.PARTITIONS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stats_rank_battle' AND PARTITION_NAME = part_name;
        IF cnt = 0 THEN
            SET @sql = CONCAT(
                'ALTER TABLE stats_rank_battle REORGANIZE PARTITION p_default INTO (',
                'PARTITION ', part_name, ' VALUES LESS THAN (TO_DAYS(''', bound_date, ''')), ',
                'PARTITION p_default VALUES LESS THAN MAXVALUE)'
            );
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;

        -- stats_result_battle
        SELECT COUNT(*) INTO cnt FROM INFORMATION_SCHEMA.PARTITIONS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'stats_result_battle' AND PARTITION_NAME = part_name;
        IF cnt = 0 THEN
            SET @sql = CONCAT(
                'ALTER TABLE stats_result_battle REORGANIZE PARTITION p_default INTO (',
                'PARTITION ', part_name, ' VALUES LESS THAN (TO_DAYS(''', bound_date, ''')), ',
                'PARTITION p_default VALUES LESS THAN MAXVALUE)'
            );
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;

        SET i = i + 1;
    END WHILE;
END //

-- -------------------------------------------------------------
-- 오래된 파티션 삭제 프로시저
-- 테이블별 보관 기간 (모든 테이블 battle_date 기준 파티셔닝):
--   stats_rank_battle:            7일
--   stats_result_battle:          7일
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS drop_old_partitions //

CREATE PROCEDURE drop_old_partitions(IN base_date DATE)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_partition_name VARCHAR(64);
    DECLARE cur CURSOR FOR
        SELECT p.TABLE_NAME, p.PARTITION_NAME
        FROM INFORMATION_SCHEMA.PARTITIONS p
        WHERE p.TABLE_SCHEMA = DATABASE()
          AND p.PARTITION_NAME != 'p_default'
          AND p.TABLE_NAME IN ('stats_rank_battle', 'stats_result_battle')
          AND CAST(p.PARTITION_DESCRIPTION AS UNSIGNED) < TO_DAYS(base_date - INTERVAL 7 DAY);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_table_name, v_partition_name;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET @sql = CONCAT('ALTER TABLE ', v_table_name, ' DROP PARTITION ', v_partition_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE cur;
END //

DELIMITER ;
