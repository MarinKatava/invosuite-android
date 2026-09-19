package com.invoisuite.app.invoice;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.invoisuite.app.R;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityInvoiceDetailBinding;
import com.invoisuite.app.model.Invoice;
import com.invoisuite.app.model.InvoiceItem;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class InvoiceDetailActivity extends BaseActivity {

    public static final String EXTRA_COMPANY_ID = "company_id";
    public static final String EXTRA_INVOICE_ID = "invoice_id";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private ActivityInvoiceDetailBinding binding;
    private ApiService api;
    private long companyId;
    private long invoiceId;
    private Invoice invoice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        companyId = getIntent().getLongExtra(EXTRA_COMPANY_ID, -1);
        invoiceId = getIntent().getLongExtra(EXTRA_INVOICE_ID, -1);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
        binding.markSentButton.setOnClickListener(v -> updateStatus("SENT"));
        binding.markPaidButton.setOnClickListener(v -> updateStatus("PAID"));
        binding.downloadPdfButton.setOnClickListener(v -> download(true));
        binding.downloadXlsxButton.setOnClickListener(v -> download(false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private boolean onMenuItemClick(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            startActivity(new Intent(this, InvoiceFormActivity.class)
                    .putExtra(InvoiceFormActivity.EXTRA_COMPANY_ID, companyId)
                    .putExtra(InvoiceFormActivity.EXTRA_INVOICE_ID, invoiceId));
            return true;
        }
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return false;
    }

    private void load() {
        setLoading(true);
        api.getInvoice(companyId, invoiceId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    invoice = response.body();
                    render(invoice);
                } else {
                    Toast.makeText(InvoiceDetailActivity.this, ApiErrors.message(response, "Could not load invoice"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Invoice> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceDetailActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void render(Invoice inv) {
        binding.toolbar.setTitle(inv.invoiceNumber());
        binding.nameText.setText(inv.name());
        binding.statusText.setText(inv.status());

        String meta = "Client: " + inv.clientName()
                + "\nIssue date: " + inv.issueDate().format(DATE_FORMAT)
                + "\nPeriod: " + inv.periodStart().format(DATE_FORMAT) + " - " + inv.periodEnd().format(DATE_FORMAT)
                + (inv.deliveryDate() != null ? "\nDelivery: " + inv.deliveryDate().format(DATE_FORMAT) : "")
                + (inv.invoicingPlace() != null ? "\nPlace: " + inv.invoicingPlace() : "");
        binding.metaText.setText(meta);

        binding.linesContainer.removeAllViews();
        for (InvoiceItem item : inv.items()) {
            binding.linesContainer.addView(buildLineRow(item, inv.currency()));
        }

        binding.netTotalText.setText("Net total: " + inv.netTotal().stripTrailingZeros().toPlainString() + " " + inv.currency());
        binding.vatAmountText.setText("VAT (" + inv.vatPercent().stripTrailingZeros().toPlainString() + "%): "
                + inv.vatAmount().stripTrailingZeros().toPlainString() + " " + inv.currency());
        binding.grandTotalText.setText("Total: " + inv.total().stripTrailingZeros().toPlainString() + " " + inv.currency());

        binding.markSentButton.setEnabled(!"SENT".equals(inv.status()) && !"PAID".equals(inv.status()));
        binding.markPaidButton.setEnabled(!"PAID".equals(inv.status()));
    }

    private View buildLineRow(InvoiceItem item, String currency) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView label = new TextView(this);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        label.setText(item.projectName() + " (" + item.quantity().stripTrailingZeros().toPlainString()
                + " " + item.unit() + " x " + item.unitPrice().stripTrailingZeros().toPlainString() + ")");
        label.setTextColor(getResources().getColor(com.invoisuite.app.R.color.text_primary, getTheme()));

        TextView total = new TextView(this);
        total.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        total.setGravity(Gravity.END);
        total.setText(item.lineTotal().stripTrailingZeros().toPlainString() + " " + currency);
        total.setTextColor(getResources().getColor(com.invoisuite.app.R.color.text_primary, getTheme()));

        row.addView(label);
        row.addView(total);
        return row;
    }

    private void updateStatus(String status) {
        setLoading(true);
        api.updateInvoiceStatus(companyId, invoiceId, Map.of("status", status)).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    invoice = response.body();
                    render(invoice);
                } else {
                    Toast.makeText(InvoiceDetailActivity.this, ApiErrors.message(response, "Could not update status"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Invoice> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceDetailActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void download(boolean pdf) {
        if (invoice == null) return;
        setLoading(true);
        Call<ResponseBody> call = pdf
                ? api.downloadInvoicePdf(companyId, invoiceId)
                : api.downloadInvoiceXlsx(companyId, invoiceId);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    saveAndShare(response.body(), pdf);
                } else {
                    Toast.makeText(InvoiceDetailActivity.this, ApiErrors.message(response, "Could not generate file"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceDetailActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveAndShare(ResponseBody body, boolean pdf) {
        try {
            File dir = new File(getCacheDir(), "downloads");
            if (!dir.exists()) dir.mkdirs();

            String safeNumber = invoice.invoiceNumber().replaceAll("[^a-zA-Z0-9]", "-");
            String extension = pdf ? "pdf" : "xlsx";
            File file = new File(dir, "invoice-" + safeNumber + "." + extension);

            try (InputStream in = body.byteStream(); FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }

            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            String mimeType = pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            Intent shareIntent = new Intent(Intent.ACTION_VIEW);
            shareIntent.setDataAndType(uri, mimeType);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(Intent.createChooser(shareIntent, "Open invoice"));
            } catch (android.content.ActivityNotFoundException e) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType(mimeType);
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(sendIntent, "Share invoice"));
            }
        } catch (IOException e) {
            Toast.makeText(this, "Could not save file: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete invoice")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void delete() {
        setLoading(true);
        api.deleteInvoice(companyId, invoiceId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    Toast.makeText(InvoiceDetailActivity.this, ApiErrors.message(response, "Could not delete invoice"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceDetailActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
