package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceItem(
        Long id,
        Long projectId,
        String projectName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        List<Long> timeEntryIds
) {
}
