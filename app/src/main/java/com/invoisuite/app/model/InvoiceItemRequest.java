package com.invoisuite.app.model;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        Long projectId,
        String projectName,
        String unit,
        BigDecimal quantity,
        BigDecimal unitPrice
) {
}
