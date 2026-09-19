package com.invoisuite.app.model;

import java.math.BigDecimal;

public record Project(
        Long id,
        String name,
        Long clientId,
        String clientName,
        String defaultUnit,
        BigDecimal defaultRate,
        String currency,
        boolean active
) {
}
