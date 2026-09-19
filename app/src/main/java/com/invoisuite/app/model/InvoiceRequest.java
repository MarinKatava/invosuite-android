package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceRequest(
        Long clientId,
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
        List<InvoiceItemRequest> items
) {
}
