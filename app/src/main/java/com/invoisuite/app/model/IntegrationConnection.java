package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record IntegrationConnection(
        Long id,
        String provider,
        String name,
        Long companyId,
        String companyName,
        Long clientId,
        String clientName,
        String baseUrl,
        String dbName,
        String username,
        boolean hasApiKey,
        String apiMode,
        boolean onlyMine,
        int lookbackDays,
        BigDecimal defaultHourlyRate,
        boolean active,
        LocalDateTime lastSyncAt,
        String lastSyncStatus,
        String lastSyncMessage
) {
}
