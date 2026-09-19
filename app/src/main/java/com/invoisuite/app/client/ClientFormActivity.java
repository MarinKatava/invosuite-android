package com.invoisuite.app.client;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityClientFormBinding;
import com.invoisuite.app.model.Client;
import com.invoisuite.app.model.ClientRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientFormActivity extends BaseActivity {

    public static final String EXTRA_COMPANY_ID = "company_id";
    public static final String EXTRA_CLIENT_ID = "client_id";

    private ActivityClientFormBinding binding;
    private ApiService api;
    private long companyId;
    private long clientId = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityClientFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        companyId = getIntent().getLongExtra(EXTRA_COMPANY_ID, -1);
        clientId = getIntent().getLongExtra(EXTRA_CLIENT_ID, -1);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(clientId > 0 ? "Edit client" : "New client");
        binding.deleteButton.setVisibility(clientId > 0 ? android.view.View.VISIBLE : android.view.View.GONE);

        if (clientId > 0) {
            loadClient();
        }
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadClient() {
        setLoading(true);
        api.listClients(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<java.util.List<Client>> call, Response<java.util.List<Client>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    response.body().stream().filter(c -> c.id() == clientId).findFirst().ifPresent(c -> {
                        binding.nameInput.setText(c.name());
                        binding.addressLineInput.setText(c.addressLine());
                        binding.vatIdInput.setText(c.vatId());
                        binding.emailInput.setText(c.email());
                    });
                } else {
                    Toast.makeText(ClientFormActivity.this, ApiErrors.message(response, "Could not load client"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<java.util.List<Client>> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ClientFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void save() {
        String name = text(binding.nameInput);
        String address = text(binding.addressLineInput);
        if (name.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Name and address are required", Toast.LENGTH_SHORT).show();
            return;
        }

        ClientRequest request = new ClientRequest(name, address, emptyToNull(text(binding.vatIdInput)), emptyToNull(text(binding.emailInput)));
        setLoading(true);
        Call<Client> call = clientId > 0 ? api.updateClient(companyId, clientId, request) : api.createClient(companyId, request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Client> call, Response<Client> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    Toast.makeText(ClientFormActivity.this, ApiErrors.message(response, "Could not save client"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Client> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ClientFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete client")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void delete() {
        setLoading(true);
        api.deleteClient(companyId, clientId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    Toast.makeText(ClientFormActivity.this, ApiErrors.message(response, "Could not delete client"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(ClientFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
