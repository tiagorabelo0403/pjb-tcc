ALTER TABLE tb_perfil_behavior_baseline
    ALTER COLUMN alert_threshold_ratio TYPE DOUBLE PRECISION
    USING alert_threshold_ratio::DOUBLE PRECISION;

ALTER TABLE tb_perfil_behavior_baseline
    ALTER COLUMN anomaly_threshold_ratio TYPE DOUBLE PRECISION
    USING anomaly_threshold_ratio::DOUBLE PRECISION;
