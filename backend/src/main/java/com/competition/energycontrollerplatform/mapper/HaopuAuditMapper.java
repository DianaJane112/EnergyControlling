package com.competition.energycontrollerplatform.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HaopuAuditMapper {
    @Insert("INSERT INTO t_haopu_meter_receive_stat "
            + "(collector_code,meter_code,first_received_at,last_received_at,receive_count,last_source_timestamp,last_report) "
            + "VALUES (#{collectorCode},#{meterCode},NOW(),NOW(),1,#{sourceTimestamp},#{report}) "
            + "ON DUPLICATE KEY UPDATE last_received_at=NOW(),receive_count=receive_count+1,"
            + "last_source_timestamp=VALUES(last_source_timestamp),last_report=VALUES(last_report)")
    int upsertReceiveStat(@Param("collectorCode") Long collectorCode,
                          @Param("meterCode") Long meterCode,
                          @Param("sourceTimestamp") Long sourceTimestamp,
                          @Param("report") Boolean report);

    @Insert("INSERT INTO t_haopu_archive_sync_log "
            + "(collector_code,response_count,valid_count,created_count,updated_count,rejected_count,error_message,synced_at) "
            + "VALUES (#{collectorCode},#{responseCount},#{validCount},#{createdCount},#{updatedCount},#{rejectedCount},#{errorMessage},NOW())")
    int insertArchiveSyncLog(@Param("collectorCode") Long collectorCode,
                             @Param("responseCount") int responseCount,
                             @Param("validCount") int validCount,
                             @Param("createdCount") int createdCount,
                             @Param("updatedCount") int updatedCount,
                             @Param("rejectedCount") int rejectedCount,
                             @Param("errorMessage") String errorMessage);
}
