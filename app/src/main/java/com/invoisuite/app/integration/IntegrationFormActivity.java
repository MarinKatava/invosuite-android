package com.invoisuite.app.integration;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityIntegrationFormBinding;
import com.invoisuite.app.model.Client;
import com.invoisuite.app.model.Company;
import com.invoisuite.app.model.ConnectionTestResult;
import com.invoisuite.app.model.IntegrationConnection;
import com.invoisuite.app.model.IntegrationRequest;
import com.invoisuite.app.model.SyncResult;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Create/edit a connection to an external time-tracking platform (Odoo for now). */
public class IntegrationFormActivity extends BaseActivity {

    public static final String EXTRA_INTEGRATION_ID = "integration_id";

    private static final String[] API_MODE_CODES = {"SESSION", "JSON2", "JSONRPC"};
    private static final String[] API_MODE_LABELS = {
            "Login and password (any Odoo version)",
            "API key (Odoo 19 or newer, JSON-2)",
            "Older Odoo (JSON-RPC, password or API key)"};
    private static final String[] SECRET_NAMES = {"Password", "API key", "Password or API key"};
    private static final String[] SECRET_HELP = {
            "Your normal Odoo login password. It is stored encrypted on the server and never sent back to this app. "
                    + "Accounts with two-factor authentication need an API key instead.",
            "Create an API key in Odoo under Preferences > Account Security. It is stored encrypted on the server "
                    + "and never sent back to this app.",
            "Your Odoo password or an API key. It is stored encrypted on the server and never sent back to this app."};

    private ActivityIntegrationFormBinding binding;
    private ApiService api;
    private long integrationId = -1;
    private boolean hasStoredKey;

    private final List<Company> companies = new ArrayList<>();
    private final List<Client> clients = new ArrayList<>();
    private final List<String> companyLabels = new ArrayList<>();
    private final List<String> clientLabels = new ArrayList<>();
    private ArrayAdapter<String> companyAdapter;
    private ArrayAdapter<String> clientAdapter;

    private Long clientsLoadedForCompany;
    private Long pendingClientId;
    private int clientsRequestSeq = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntegrationFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        integrationId = getIntent().getLongExtra(EXTRA_INTEGRATION_ID, -1);
        boolean editing = integrationId > 0;

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(editing ? "Edit connection" : "New connection");
        binding.deleteButton.setVisibility(editing ? View.VISIBLE : View.GONE);
        binding.importButton.setVisibility(editing ? View.VISIBLE : View.GONE);

        binding.apiModeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, API_MODE_LABELS));
        binding.apiModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSecretTexts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        updateSecretTexts();
        companyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, companyLabels);
        binding.companySpinner.setAdapter(companyAdapter);
        clientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clientLabels);
        binding.clientSpinner.setAdapter(clientAdapter);

        binding.companySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < companies.size() && !companies.get(position).id().equals(clientsLoadedForCompany)) {
                    loadClients(companies.get(position).id());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.testButton.setOnClickListener(v -> testConnection());
        binding.saveButton.setOnClickListener(v -> save());
        binding.importButton.setOnClickListener(v -> importNow());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());

        loadCompanies();
    }

    private void loadCompanies() {
        api.listCompanies().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Company>> call, Response<List<Company>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    toast(ApiErrors.message(response, "Could not load companies"));
                    return;
                }
                companies.clear();
                companies.addAll(response.body());
                companyLabels.clear();
                for (Company c : companies) companyLabels.add(c.name());
                companyAdapter.notifyDataSetChanged();

                if (integrationId > 0) {
                    loadConnection();
                } else if (!companies.isEmpty()) {
                    loadClients(companies.get(0).id());
                } else {
                    toast("Create a company first");
                }
            }

            @Override
            public void onFailure(Call<List<Company>> call, Throwable t) {
                toast("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void loadClients(long companyId) {
        clientsLoadedForCompany = companyId;
        int seq = ++clientsRequestSeq;
        api.listClients(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Client>> call, Response<List<Client>> response) {
                if (seq != clientsRequestSeq || !response.isSuccessful() || response.body() == null) return;
                clients.clear();
                clients.addAll(response.body());
                clientLabels.clear();
                clientLabels.add("No client (assign later)");
                for (Client c : clients) clientLabels.add(c.name());
                clientAdapter.notifyDataSetChanged();

                if (pendingClientId != null) {
                    for (int i = 0; i < clients.size(); i++) {
                        if (clients.get(i).id().equals(pendingClientId)) {
                            binding.clientSpinner.setSelection(i + 1);
                            break;
                        }
                    }
                    pendingClientId = null;
                }
            }

            @Override
            public void onFailure(Call<List<Client>> call, Throwable t) {
                toast("Could not load clients: " + t.getLocalizedMessage());
            }
        });
    }

    private void loadConnection() {
        setLoading(true);
        api.getIntegration(integrationId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<IntegrationConnection> call, Response<IntegrationConnection> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    bind(response.body());
                } else {
                    toast(ApiErrors.message(response, "Could not load connection"));
                }
            }

            @Override
            public void onFailure(Call<IntegrationConnection> call, Throwable t) {
                setLoading(false);
                toast("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void bind(IntegrationConnection c) {
        binding.nameInput.setText(c.name());
        binding.urlInput.setText(c.baseUrl());
        binding.dbInput.setText(c.dbName());
        binding.usernameInput.setText(c.username());
        binding.onlyMineSwitch.setChecked(c.onlyMine());
        binding.lookbackInput.setText(String.valueOf(c.lookbackDays()));
        binding.rateInput.setText(c.defaultHourlyRate() != null ? c.defaultHourlyRate().toPlainString() : "");
        binding.activeSwitch.setChecked(c.active());

        hasStoredKey = c.hasApiKey();

        for (int i = 0; i < API_MODE_CODES.length; i++) {
            if (API_MODE_CODES[i].equals(c.apiMode())) binding.apiModeSpinner.setSelection(i);
        }
        updateSecretTexts();

        pendingClientId = c.clientId();
        for (int i = 0; i < companies.size(); i++) {
            if (companies.get(i).id().equals(c.companyId())) {
                binding.companySpinner.setSelection(i);
                break;
            }
        }
        loadClients(c.companyId());
    }

    /** Labels the secret field (password vs API key) to match the selected login method. */
    private void updateSecretTexts() {
        int mode = Math.max(0, binding.apiModeSpinner.getSelectedItemPosition());
        String name = SECRET_NAMES[mode];
        binding.apiKeyLayout.setHint(hasStoredKey ? name + " (leave blank to keep the saved one)" : name + " *");
        binding.apiKeyHelp.setText(SECRET_HELP[mode]);
    }

    /** Builds the request from the form, or returns null after telling the user what is missing. */
    @Nullable
    private IntegrationRequest buildRequest(boolean forSave) {
        String name = text(binding.nameInput);
        String url = text(binding.urlInput);
        String username = text(binding.usernameInput);
        String apiKey = text(binding.apiKeyInput);

        if (forSave && name.isEmpty()) {
            toast("Give the connection a name");
            return null;
        }
        if (url.isEmpty() || username.isEmpty()) {
            toast("Server URL and login are required");
            return null;
        }
        if (companies.isEmpty() || binding.companySpinner.getSelectedItemPosition() < 0) {
            toast("Pick the company the hours are booked to");
            return null;
        }
        if (apiKey.isEmpty() && !hasStoredKey) {
            toast("Enter the " + SECRET_NAMES[binding.apiModeSpinner.getSelectedItemPosition()].toLowerCase());
            return null;
        }

        int lookback;
        try {
            lookback = Integer.parseInt(text(binding.lookbackInput));
        } catch (NumberFormatException e) {
            lookback = -1;
        }
        if (lookback < 1 || lookback > 730) {
            toast("Import period must be between 1 and 730 days");
            return null;
        }

        BigDecimal rate = null;
        String rawRate = text(binding.rateInput);
        if (!rawRate.isEmpty()) {
            try {
                rate = new BigDecimal(rawRate);
            } catch (NumberFormatException e) {
                toast("Hourly rate must be a number");
                return null;
            }
        }

        int clientPos = binding.clientSpinner.getSelectedItemPosition();
        Long clientId = clientPos > 0 && clientPos - 1 < clients.size() ? clients.get(clientPos - 1).id() : null;

        return new IntegrationRequest(
                "ODOO",
                name.isEmpty() ? "Odoo" : name,
                companies.get(binding.companySpinner.getSelectedItemPosition()).id(),
                clientId,
                url,
                emptyToNull(text(binding.dbInput)),
                username,
                emptyToNull(apiKey),
                API_MODE_CODES[binding.apiModeSpinner.getSelectedItemPosition()],
                binding.onlyMineSwitch.isChecked(),
                lookback,
                rate,
                binding.activeSwitch.isChecked());
    }

    private void testConnection() {
        IntegrationRequest request = buildRequest(false);
        if (request == null) return;
        setLoading(true);
        api.testIntegration(request, integrationId > 0 ? integrationId : null).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ConnectionTestResult> call, Response<ConnectionTestResult> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ConnectionTestResult r = response.body();
                    new AlertDialog.Builder(IntegrationFormActivity.this)
                            .setTitle(r.ok() ? "Connection works" : "Connection failed")
                            .setMessage(r.message())
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    toast(ApiErrors.message(response, "Could not test the connection"));
                }
            }

            @Override
            public void onFailure(Call<ConnectionTestResult> call, Throwable t) {
                setLoading(false);
                toast("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void save() {
        IntegrationRequest request = buildRequest(true);
        if (request == null) return;
        setLoading(true);
        Call<IntegrationConnection> call = integrationId > 0
                ? api.updateIntegration(integrationId, request) : api.createIntegration(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<IntegrationConnection> call, Response<IntegrationConnection> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    toast(ApiErrors.message(response, "Could not save the connection"));
                }
            }

            @Override
            public void onFailure(Call<IntegrationConnection> call, Throwable t) {
                setLoading(false);
                toast("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void importNow() {
        setLoading(true);
        api.syncIntegration(integrationId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<SyncResult> call, Response<SyncResult> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    SyncReport.show(IntegrationFormActivity.this, List.of(response.body()), null);
                } else {
                    toast(ApiErrors.message(response, "Import failed"));
                }
            }

            @Override
            public void onFailure(Call<SyncResult> call, Throwable t) {
                setLoading(false);
                toast("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete connection")
                .setMessage("Imported hours that are not billed yet will be removed. Hours already billed on invoices are kept.")
                .setPositiveButton("Delete", (d, w) -> delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void delete() {
        setLoading(true);
        api.deleteIntegration(integrationId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    toast(ApiErrors.message(response, "Could not delete the connection"));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                toast("Network error: " + t.getLocalizedMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.saveButton.setEnabled(!loading);
        binding.testButton.setEnabled(!loading);
        binding.importButton.setEnabled(!loading);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Nullable
    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
