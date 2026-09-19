package com.invoisuite.app.invoice;

/** Mutable, in-memory line item being edited in {@link InvoiceFormActivity} before it is submitted. */
public class InvoiceLineDraft {
    public Long projectId;
    public String projectName = "";
    public String unit = "Hour";
    public String quantity = "";
    public String unitPrice = "";

    public java.math.BigDecimal lineTotal() {
        try {
            java.math.BigDecimal qty = new java.math.BigDecimal(quantity.isEmpty() ? "0" : quantity);
            java.math.BigDecimal price = new java.math.BigDecimal(unitPrice.isEmpty() ? "0" : unitPrice);
            return qty.multiply(price);
        } catch (NumberFormatException e) {
            return java.math.BigDecimal.ZERO;
        }
    }
}
