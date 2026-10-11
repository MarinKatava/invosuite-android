package com.invoisuite.app.invoice;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityInvoiceFormBinding;
import com.invoisuite.app.model.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InvoiceFormActivity extends BaseActivity {

    public static final String EXTRA_COMPANY_ID = "company_id";
    public static final String EXTRA_INVOICE_ID = "invoice_id";

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private ActivityInvoiceFormBinding binding;
    private ApiService api;
    private long companyId;
    private long invoiceId = -1;

    private final List<Client> clients = new ArrayList<>();
    private final List<CurrencyOption> currencies = new ArrayList<>();
    private final List<InvoiceLineDraft> lines = new ArrayList<>();
    private InvoiceLineAdapter lineAdapter;
    private ArrayAdapter<String> clientAdapter;
    private final List<String> clientLabels = new ArrayList<>();

    private LocalDate issueDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate deliveryDate;

    private int pendingLoads = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInvoiceFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        companyId = getIntent().getLongExtra(EXTRA_COMPANY_ID, -1);
        invoiceId = getIntent().getLongExtra(EXTRA_INVOICE_ID, -1);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(invoiceId > 0 ? "Edit invoice" : "New invoice");

        clientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clientLabels);
        binding.clientSpinner.setAdapter(clientAdapter);

        lineAdapter = new InvoiceLineAdapter(lines, this::recomputeTotals);
        binding.linesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.linesRecyclerView.setAdapter(lineAdapter);
        binding.addLineButton.setOnClickListener(v -> lineAdapter.addLine());
        binding.importHoursButton.setOnClickListener(v -> importHours());

        binding.issueDateInput.setOnClickListener(v -> pickDate(issueDate, d -> {
            issueDate = d;
            binding.issueDateInput.setText(d.format(DISPLAY_FORMAT));
            if (invoiceId <= 0 && text(binding.invoiceNumberInput).isEmpty()) {
                suggestInvoiceNumber();
            }
        }));
        binding.periodStartInput.setOnClickListener(v -> pickDate(periodStart, d -> {
            periodStart = d;
            binding.periodStartInput.setText(d.format(DISPLAY_FORMAT));
        }));
        binding.periodEndInput.setOnClickListener(v -> pickDate(periodEnd, d -> {
            periodEnd = d;
            binding.periodEndInput.setText(d.format(DISPLAY_FORMAT));
        }));
        binding.deliveryDateInput.setOnClickListener(v -> pickDate(deliveryDate, d -> {
            deliveryDate = d;
            binding.deliveryDateInput.setText(d.format(DISPLAY_FORMAT));
        }));

        binding.vatPercentInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable s) { recomputeTotals(); }
        });

        binding.saveButton.setOnClickListener(v -> save());

        loadReferenceData();
    }

    private void pickDate(@Nullable LocalDate initial, java.util.function.Consumer<LocalDate> onPicked) {
        LocalDate base = initial != null ? initial : LocalDate.now();
        new DatePickerDialog(this, (view, year, month, day) -> onPicked.accept(LocalDate.of(year, month + 1, day)),
                base.getYear(), base.getMonthValue() - 1, base.getDayOfMonth()).show();
    }

    private void loadReferenceData() {
        pendingLoads = 3;

        api.listClients(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Client>> call, Response<List<Client>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    clients.clear();
                    clients.addAll(response.body());
                    clientLabels.clear();
                    for (Client c : clients) clientLabels.add(c.name());
                    clientAdapter.notifyDataSetChanged();
                }
                onLoaded();
            }

            @Override
            public void onFailure(Call<List<Client>> call, Throwable t) {
                Toast.makeText(InvoiceFormActivity.this, "Could not load clients: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                onLoaded();
            }
        });

        api.listProjects(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lineAdapter.setProjects(response.body());
                }
                onLoaded();
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                onLoaded();
            }
        });

        api.units().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    lineAdapter.setUnits(response.body());
                }
                loadCurrencies();
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                loadCurrencies();
            }
        });
    }

    private void loadCurrencies() {
        api.currencies().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<CurrencyOption>> call, Response<List<CurrencyOption>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currencies.clear();
                    currencies.addAll(response.body());
                    ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                            InvoiceFormActivity.this, android.R.layout.simple_spinner_dropdown_item, currencyLabels());
                    binding.currencySpinner.setAdapter(currencyAdapter);
                }
                onLoaded();
            }

            @Override
            public void onFailure(Call<List<CurrencyOption>> call, Throwable t) {
                onLoaded();
            }
        });
    }

    private List<String> currencyLabels() {
        List<String> labels = new ArrayList<>();
        for (CurrencyOption c : currencies) labels.add(c.code());
        return labels;
    }

    private void onLoaded() {
        pendingLoads--;
        if (pendingLoads == 0) {
            if (invoiceId > 0) {
                loadInvoice();
            } else {
                issueDate = LocalDate.now();
                periodStart = LocalDate.now().withDayOfMonth(1);
                periodEnd = LocalDate.now();
                binding.issueDateInput.setText(issueDate.format(DISPLAY_FORMAT));
                binding.periodStartInput.setText(periodStart.format(DISPLAY_FORMAT));
                binding.periodEndInput.setText(periodEnd.format(DISPLAY_FORMAT));
                suggestInvoiceNumber();
                lineAdapter.addLine();
            }
        }
    }

    private void suggestInvoiceNumber() {
        api.nextInvoiceNumber(companyId, issueDate.toString()).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful() && response.body() != null && text(binding.invoiceNumberInput).isEmpty()) {
                    binding.invoiceNumberInput.setText(response.body().get("invoiceNumber"));
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
            }
        });
    }

    private void loadInvoice() {
        setLoading(true);
        api.getInvoice(companyId, invoiceId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    bind(response.body());
                } else {
                    Toast.makeText(InvoiceFormActivity.this, ApiErrors.message(response, "Could not load invoice"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Invoice> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(Invoice inv) {
        binding.nameInput.setText(inv.name());
        binding.invoiceNumberInput.setText(inv.invoiceNumber());
        binding.invoicingPlaceInput.setText(inv.invoicingPlace());
        binding.vatPercentInput.setText(inv.vatPercent().stripTrailingZeros().toPlainString());
        binding.vatExemptTextInput.setText(inv.vatExemptText());
        binding.notesInput.setText(inv.notes());

        issueDate = inv.issueDate();
        periodStart = inv.periodStart();
        periodEnd = inv.periodEnd();
        deliveryDate = inv.deliveryDate();
        binding.issueDateInput.setText(issueDate.format(DISPLAY_FORMAT));
        binding.periodStartInput.setText(periodStart.format(DISPLAY_FORMAT));
        binding.periodEndInput.setText(periodEnd.format(DISPLAY_FORMAT));
        if (deliveryDate != null) binding.deliveryDateInput.setText(deliveryDate.format(DISPLAY_FORMAT));

        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).id().equals(inv.clientId())) {
                binding.clientSpinner.setSelection(i);
                break;
            }
        }
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).code().equals(inv.currency())) {
                binding.currencySpinner.setSelection(i);
                break;
            }
        }

        lines.clear();
        for (InvoiceItem item : inv.items()) {
            InvoiceLineDraft draft = new InvoiceLineDraft();
            draft.projectId = item.projectId();
            draft.projectName = item.projectName();
            draft.unit = item.unit();
            draft.quantity = item.quantity().stripTrailingZeros().toPlainString();
            draft.unitPrice = item.unitPrice().stripTrailingZeros().toPlainString();
            if (item.timeEntryIds() != null) {
                draft.timeEntryIds = new ArrayList<>(item.timeEntryIds());
            }
            lines.add(draft);
        }
        lineAdapter.notifyDataSetChanged();
        recomputeTotals();
    }

    /** Pulls the chosen client's unbilled imported hours for the invoice period in as ready-made lines. */
    private void importHours() {
        if (clients.isEmpty() || binding.clientSpinner.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "Pick the client first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (periodStart == null || periodEnd == null) {
            Toast.makeText(this, "Pick the invoicing period first", Toast.LENGTH_SHORT).show();
            return;
        }
        long clientId = clients.get(binding.clientSpinner.getSelectedItemPosition()).id();

        setLoading(true);
        api.timeSummary(companyId, clientId, periodStart.toString(), periodEnd.toString()).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<TimeSummaryLine>> call, Response<List<TimeSummaryLine>> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(InvoiceFormActivity.this, ApiErrors.message(response, "Could not load time bookings"), Toast.LENGTH_LONG).show();
                    return;
                }
                addImportedLines(response.body());
            }

            @Override
            public void onFailure(Call<List<TimeSummaryLine>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void addImportedLines(List<TimeSummaryLine> summary) {
        // Drop untouched blank rows (e.g. the one a new invoice starts with) to make room for the imported lines.
        lines.removeIf(l -> l.projectName.isEmpty() && l.quantity.isEmpty() && l.unitPrice.isEmpty());

        int added = 0;
        java.math.BigDecimal totalHours = BigDecimal.ZERO;
        for (TimeSummaryLine s : summary) {
            boolean alreadyOnInvoice = lines.stream().anyMatch(l -> !java.util.Collections.disjoint(l.timeEntryIds, s.timeEntryIds()));
            if (alreadyOnInvoice) {
                continue;
            }
            InvoiceLineDraft draft = new InvoiceLineDraft();
            draft.projectId = s.projectId();
            draft.projectName = s.projectName();
            draft.unit = s.unit() != null ? s.unit() : "Hour";
            draft.quantity = s.hours().stripTrailingZeros().toPlainString();
            draft.unitPrice = s.unitPrice() != null ? s.unitPrice().stripTrailingZeros().toPlainString() : "";
            draft.timeEntryIds = new ArrayList<>(s.timeEntryIds());
            lines.add(draft);
            added++;
            totalHours = totalHours.add(s.hours());
        }
        if (lines.isEmpty()) {
            lineAdapter.addLine();
        }
        lineAdapter.notifyDataSetChanged();
        recomputeTotals();

        if (added == 0) {
            Toast.makeText(this, "No unbilled hours for this client in the period. Import time bookings first, or check the period.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Added " + added + " line(s), " + totalHours.stripTrailingZeros().toPlainString() + " hours", Toast.LENGTH_LONG).show();
        }
    }

    private void recomputeTotals() {
        BigDecimal net = BigDecimal.ZERO;
        for (InvoiceLineDraft line : lines) {
            net = net.add(line.lineTotal());
        }
        BigDecimal vatPercent;
        try {
            String raw = text(binding.vatPercentInput);
            vatPercent = raw.isEmpty() ? BigDecimal.ZERO : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            vatPercent = BigDecimal.ZERO;
        }
        BigDecimal vatAmount = net.multiply(vatPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = net.add(vatAmount);

        binding.netTotalText.setText("Net total: " + net.setScale(2, RoundingMode.HALF_UP));
        binding.vatAmountText.setText("VAT (" + vatPercent.stripTrailingZeros().toPlainString() + "%): " + vatAmount);
        binding.grandTotalText.setText("Total: " + total);
    }

    private void save() {
        String name = text(binding.nameInput);
        if (name.isEmpty()) {
            Toast.makeText(this, "Invoice name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (clients.isEmpty() || binding.clientSpinner.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "Add a client first", Toast.LENGTH_LONG).show();
            return;
        }
        if (issueDate == null || periodStart == null || periodEnd == null) {
            Toast.makeText(this, "Pick the issue date and invoicing period", Toast.LENGTH_LONG).show();
            return;
        }
        if (lines.isEmpty()) {
            Toast.makeText(this, "Add at least one line item", Toast.LENGTH_LONG).show();
            return;
        }
        if (currencies.isEmpty() || binding.currencySpinner.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "Currencies are still loading, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        List<InvoiceItemRequest> itemRequests = new ArrayList<>();
        for (InvoiceLineDraft line : lines) {
            if (line.projectName.isEmpty() || line.quantity.isEmpty() || line.unitPrice.isEmpty()) {
                Toast.makeText(this, "Fill in every line item (description, quantity, unit price)", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                itemRequests.add(new InvoiceItemRequest(
                        line.projectId, line.projectName, line.unit,
                        new BigDecimal(line.quantity), new BigDecimal(line.unitPrice),
                        line.timeEntryIds));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Quantity and unit price must be numbers", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Long clientId = clients.get(binding.clientSpinner.getSelectedItemPosition()).id();
        String currencyCode = currencies.get(binding.currencySpinner.getSelectedItemPosition()).code();
        BigDecimal vatPercent;
        try {
            String raw = text(binding.vatPercentInput);
            vatPercent = raw.isEmpty() ? BigDecimal.ZERO : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "VAT % must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        InvoiceRequest request = new InvoiceRequest(
                clientId, name, emptyToNull(text(binding.invoiceNumberInput)),
                issueDate, periodStart, periodEnd, deliveryDate,
                emptyToNull(text(binding.invoicingPlaceInput)), currencyCode, vatPercent,
                emptyToNull(text(binding.vatExemptTextInput)), emptyToNull(text(binding.notesInput)),
                itemRequests);

        setLoading(true);
        Call<Invoice> call = invoiceId > 0 ? api.updateInvoice(companyId, invoiceId, request) : api.createInvoice(companyId, request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Invoice> call, Response<Invoice> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(InvoiceFormActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(InvoiceFormActivity.this, ApiErrors.message(response, "Could not save invoice"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Invoice> call, Throwable t) {
                setLoading(false);
                Toast.makeText(InvoiceFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.saveButton.setEnabled(!loading);
    }

    private String text(com.google.android.material.textfield.TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Nullable
    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
