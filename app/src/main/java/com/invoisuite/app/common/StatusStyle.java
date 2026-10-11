package com.invoisuite.app.common;

import android.content.res.ColorStateList;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.invoisuite.app.R;

/** Colours a status chip by meaning: neutral (draft/paused), info (sent) or success (paid/active). */
public final class StatusStyle {

    private StatusStyle() {
    }

    public static void apply(TextView view, String status) {
        int bg;
        int fg;
        switch (status == null ? "" : status) {
            case "PAID":
            case "ACTIVE":
                bg = R.color.status_success_bg;
                fg = R.color.status_success_text;
                break;
            case "SENT":
            case "FINALIZED":
                bg = R.color.status_info_bg;
                fg = R.color.status_info_text;
                break;
            default:
                bg = R.color.status_neutral_bg;
                fg = R.color.status_neutral_text;
        }
        view.setText(status);
        view.setTextColor(ContextCompat.getColor(view.getContext(), fg));
        view.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(view.getContext(), bg)));
    }
}
