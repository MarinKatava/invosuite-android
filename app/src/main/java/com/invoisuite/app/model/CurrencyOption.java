package com.invoisuite.app.model;

public record CurrencyOption(String code, String symbol) {
    @Override
    public String toString() {
        return code + " (" + symbol + ")";
    }
}
