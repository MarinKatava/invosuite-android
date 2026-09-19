package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record Invoice(
        Long id,
        Long companyId,
        Long clientId,
        String clientName,
        String name,
        String invoiceNumber,
        LocalDate issueDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate deliveryDate,
        String invoicingPlace,
        String currency,
        BigDecimal vatPercent,
        String vatExemptText,
        String notes,
        String status,
        BigDecimal netTotal,
        BigDecimal vatAmount,
        BigDecimal total,
        List<InvoiceItem> items
) {
}
