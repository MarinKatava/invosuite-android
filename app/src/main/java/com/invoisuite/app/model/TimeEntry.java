package com.invoisuite.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TimeEntry(
        Long id,
        LocalDate workDate,
        Long projectId,
        String projectName,
        BigDecimal hours,
        String description,
        boolean invoiced,
        String invoiceNumber
) {
}
