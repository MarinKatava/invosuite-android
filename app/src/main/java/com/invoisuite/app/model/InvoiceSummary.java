package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceSummary(
        Long id,
        String name,
        String invoiceNumber,
        String clientName,
        LocalDate issueDate,
        String currency,
        BigDecimal total,
        String status
) {
}
