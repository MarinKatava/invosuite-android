package com.invoisuite.app.model;

import java.math.BigDecimal;

public record CompanyRequest(
        String name,
        String addressLine,
        String city,
        String postalCode,
        String country,
        String registrationNumber,
        String vatId,
        String bankName,
        String iban,
        String bic,
        String defaultCurrency,
        BigDecimal defaultVatPercent,
        String vatExemptText,
        String invoicingPlace
) {
}
