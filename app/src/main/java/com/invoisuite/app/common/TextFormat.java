package com.invoisuite.app.common;

import java.util.Locale;

/** Display-only text helpers; they never change what is stored or printed on invoices. */
public final class TextFormat {

    private TextFormat() {
    }

    /** Up to two initials for an avatar badge: "Obrt Inherit" -> "OI", "ABF GmbH" -> "AG", "Acme" -> "A". */
    public static String initials(String name) {
        if (name == null) {
            return "?";
        }
        StringBuilder out = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            for (int i = 0; i < word.length(); i++) {
                if (Character.isLetterOrDigit(word.charAt(i))) {
                    out.append(word.substring(i, i + 1).toUpperCase(Locale.ROOT));
                    break;
                }
            }
            if (out.length() == 2) {
                break;
            }
        }
        return out.length() == 0 ? "?" : out.toString();
    }
}
