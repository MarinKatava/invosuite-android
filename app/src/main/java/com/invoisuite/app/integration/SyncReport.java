package com.invoisuite.app.integration;

import android.app.AlertDialog;
import android.content.Context;
import com.invoisuite.app.model.SyncResult;

import java.util.List;

/** Shows the outcome of an "import time bookings" run, one block per connection. */
public final class SyncReport {

    private SyncReport() {
    }

    public static void show(Context context, List<SyncResult> results, Runnable onSetUp) {
        if (results.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Nothing to import")
                    .setMessage("There is no active time-tracking connection yet. Set one up to import your bookings.")
                    .setNegativeButton("Close", null);
            if (onSetUp != null) {
                builder.setPositiveButton("Set up", (d, w) -> onSetUp.run());
            }
            builder.show();
            return;
        }

        StringBuilder message = new StringBuilder();
        boolean allOk = true;
        for (SyncResult r : results) {
            if (message.length() > 0) message.append("\n\n");
            if (r.success()) {
                message.append("✓ ").append(r.connectionName()).append('\n')
                        .append(r.imported()).append(" new, ").append(r.updated()).append(" updated, ")
                        .append(r.removed()).append(" removed · ")
                        .append(r.totalHours().stripTrailingZeros().toPlainString()).append(" h in period");
                if (r.skippedBilled() > 0) {
                    message.append("\n").append(r.skippedBilled()).append(" already-billed entries left unchanged");
                }
            } else {
                allOk = false;
                message.append("✗ ").append(r.connectionName()).append('\n').append(r.message());
            }
        }

        new AlertDialog.Builder(context)
                .setTitle(allOk ? "Import finished" : "Import finished with problems")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}
