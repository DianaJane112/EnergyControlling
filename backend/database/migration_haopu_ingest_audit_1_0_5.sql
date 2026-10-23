-- Haopu ingest audit tables for 1.0.5. Non-billing diagnostics only.
CREATE TABLE IF NOT EXISTS t_haopu_meter_receive_stat (
  id BIGINT NOT NULL AUTO_INCREMENT,
  collector_code BIGINT NOT NULL,
  meter_code BIGINT NOT NULL,
  first_received_at DATETIME NOT NULL,
  last_received_at DATETIME NOT NULL,
  receive_count BIGINT NOT NULL DEFAULT 1,
  last_source_timestamp BIGINT NULL,
  last_report TINYINT NULL,
  PRIMARY KEY (id), UNIQUE KEY uk_haopu_receive_device (collector_code, meter_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_haopu_archive_sync_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  collector_code BIGINT NOT NULL,
  response_count INT NOT NULL DEFAULT 0,
  valid_count INT NOT NULL DEFAULT 0,
  created_count INT NOT NULL DEFAULT 0,
  updated_count INT NOT NULL DEFAULT 0,
  rejected_count INT NOT NULL DEFAULT 0,
  error_message VARCHAR(1000) NULL,
  synced_at DATETIME NOT NULL,
  PRIMARY KEY (id), KEY idx_haopu_archive_sync_collector (collector_code, synced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
