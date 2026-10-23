package com.competition.energycontrollerplatform.integration.dto;

public record HaopuArchiveSyncResult(int responseCount, int validCount, int createdCount,
                                     int updatedCount, int rejectedCount) {
    public int syncedCount() {
        return createdCount + updatedCount;
    }
}
