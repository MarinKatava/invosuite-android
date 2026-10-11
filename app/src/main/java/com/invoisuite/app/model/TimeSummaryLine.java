package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.util.List;

public record TimeSummaryLine(
        Long projectId,
        String projectName,
        String unit,
        BigDecimal hours,
        BigDecimal unitPrice,
        int entryCount,
        List<Long> timeEntryIds
) {
}
