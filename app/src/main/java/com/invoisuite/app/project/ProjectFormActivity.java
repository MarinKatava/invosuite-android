package com.invoisuite.app.project;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityProjectFormBinding;
import com.invoisuite.app.model.Client;
import com.invoisuite.app.model.CurrencyOption;
import com.invoisuite.app.model.Project;
import com.invoisuite.app.model.ProjectRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProjectFormActivity extends BaseActivity {

    public static final String EXTRA_COMPANY_ID = "company_id";
    public static final String EXTRA_PROJECT_ID = "project_id";

    private ActivityProjectFormBinding binding;
    private ApiService api;
    private long companyId;
    private long projectId = -1;

    private final List<Client> clients = new ArrayList<>();
    private final List<String> clientLabels = new ArrayList<>();
    private final List<String> currencyCodes = new ArrayList<>();
    private ArrayAdapter<String> clientAdapter;
    private ArrayAdapter<String> unitAdapter;
    private ArrayAdapter<String> currencyAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        companyId = getIntent().getLongExtra(EXTRA_COMPANY_ID, -1);
        projectId = getIntent().getLongExtra(EXTRA_PROJECT_ID, -1);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(projectId > 0 ? "Edit project" : "New project");
        binding.deleteButton.setVisibility(projectId > 0 ? android.view.View.VISIBLE : android.view.View.GONE);

        clientAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clientLabels);
        binding.clientSpinner.setAdapter(clientAdapter);
        unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        binding.unitSpinner.setAdapter(unitAdapter);
        currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencyCodes);
        binding.currencySpinner.setAdapter(currencyAdapter);

        loadReferenceData();
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadReferenceData() {
        api.listClients(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Client>> call, Response<List<Client>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    clients.clear();
                    clients.addAll(response.body());
                    clientLabels.clear();
                    clientLabels.add("No client");
                    for (Client c : clients) clientLabels.add(c.name());
                    clientAdapter.notifyDataSetChanged();
                    maybeLoadProject();
                }
            }

            @Override
            public void onFailure(Call<List<Client>> call, Throwable t) {
                Toast.makeText(ProjectFormActivity.this, "Could not load clients: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });

        api.units().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    unitAdapter.clear();
                    unitAdapter.addAll(response.body());
                    maybeLoadProject();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(ProjectFormActivity.this, "Could not load units: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });

        api.currencies().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<CurrencyOption>> call, Response<List<CurrencyOption>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currencyCodes.clear();
                    currencyCodes.add("Use company default");
                    for (CurrencyOption c : response.body()) currencyCodes.add(c.code());
                    currencyAdapter.notifyDataSetChanged();
                    maybeLoadProject();
                }
            }

            @Override
            public void onFailure(Call<List<CurrencyOption>> call, Throwable t) {
                Toast.makeText(ProjectFormActivity.this, "Could not load currencies: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean projectLoadTriggered = false;

    private void maybeLoadProject() {
        if (projectLoadTriggered || projectId <= 0) return;
        if (clientLabels.isEmpty() || unitAdapter.getCount() == 0 || currencyCodes.isEmpty()) return;
        projectLoadTriggered = true;
        loadProject();
    }

    private void loadProject() {
        setLoading(true);
        api.listProjects(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    response.body().stream().filter(p -> p.id() == projectId).findFirst().ifPresent(ProjectFormActivity.this::bind);
                } else {
                    Toast.makeText(ProjectFormActivity.this, ApiErrors.message(response, "Could not load project"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ProjectFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(Project p) {
        binding.nameInput.setText(p.name());
        if (p.defaultRate() != null) binding.rateInput.setText(p.defaultRate().toPlainString());
        binding.activeSwitch.setChecked(p.active());

        if (p.clientId() != null) {
            for (int i = 0; i < clients.size(); i++) {
                if (clients.get(i).id().equals(p.clientId())) {
                    binding.clientSpinner.setSelection(i + 1);
                    break;
                }
            }
        }
        int unitIndex = unitAdapter.getPosition(p.defaultUnit());
        if (unitIndex >= 0) binding.unitSpinner.setSelection(unitIndex);

        if (p.currency() != null) {
            int currencyIndex = currencyCodes.indexOf(p.currency());
            if (currencyIndex >= 0) binding.currencySpinner.setSelection(currencyIndex);
        }
    }

    private void save() {
        String name = text(binding.nameInput);
        if (name.isEmpty()) {
            Toast.makeText(this, "Project name is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (unitAdapter.getCount() == 0) {
            Toast.makeText(this, "Reference data is still loading, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        int clientPos = binding.clientSpinner.getSelectedItemPosition();
        Long clientId = clientPos > 0 ? clients.get(clientPos - 1).id() : null;

        String unit = (String) binding.unitSpinner.getSelectedItem();

        BigDecimal rate = null;
        String rawRate = text(binding.rateInput);
        if (!rawRate.isEmpty()) {
            try {
                rate = new BigDecimal(rawRate);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Rate must be a number", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int currencyPos = binding.currencySpinner.getSelectedItemPosition();
        String currency = currencyPos > 0 ? currencyCodes.get(currencyPos) : null;

        ProjectRequest request = new ProjectRequest(name, clientId, unit, rate, currency, binding.activeSwitch.isChecked());

        setLoading(true);
        Call<Project> call = projectId > 0 ? api.updateProject(companyId, projectId, request) : api.createProject(companyId, request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Project> call, Response<Project> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    Toast.makeText(ProjectFormActivity.this, ApiErrors.message(response, "Could not save project"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Project> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ProjectFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete project")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void delete() {
        setLoading(true);
        api.deleteProject(companyId, projectId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    Toast.makeText(ProjectFormActivity.this, ApiErrors.message(response, "Could not delete project"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ProjectFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
}
