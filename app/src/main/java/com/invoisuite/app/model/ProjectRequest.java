package com.invoisuite.app.model;

import java.math.BigDecimal;

public record ProjectRequest(
        String name,
        Long clientId,
        String defaultUnit,
        BigDecimal defaultRate,
        String currency,
        Boolean active
) {
}
