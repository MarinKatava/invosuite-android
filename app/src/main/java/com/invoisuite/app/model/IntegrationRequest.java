package com.invoisuite.app.model;

import java.math.BigDecimal;

public record IntegrationRequest(
        String provider,
        String name,
        Long companyId,
        Long clientId,
        String baseUrl,
        String dbName,
        String username,
        String apiKey,
        String apiMode,
        Boolean onlyMine,
        Integer lookbackDays,
        BigDecimal defaultHourlyRate,
        Boolean active
) {
}
