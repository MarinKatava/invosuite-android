package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SyncResult(
        Long connectionId,
        String connectionName,
        boolean success,
        String message,
        LocalDate from,
        LocalDate to,
        int imported,
        int updated,
        int removed,
        int skippedBilled,
        BigDecimal totalHours
) {
}
