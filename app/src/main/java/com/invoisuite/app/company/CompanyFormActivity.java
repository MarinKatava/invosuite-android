package com.invoisuite.app.company;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityCompanyFormBinding;
import com.invoisuite.app.model.Company;
import com.invoisuite.app.model.CompanyRequest;
import com.invoisuite.app.model.CurrencyOption;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;

public class CompanyFormActivity extends BaseActivity {

    public static final String EXTRA_COMPANY_ID = "company_id";

    private ActivityCompanyFormBinding binding;
    private ApiService api;
    private long companyId = -1;
    private List<CurrencyOption> currencies;
    private ArrayAdapter<CurrencyOption> currencyAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        companyId = getIntent().getLongExtra(EXTRA_COMPANY_ID, -1);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(companyId > 0 ? "Edit company" : "New company");
        binding.deleteButton.setVisibility(companyId > 0 ? android.view.View.VISIBLE : android.view.View.GONE);

        currencyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        binding.currencySpinner.setAdapter(currencyAdapter);

        loadCurrencies();
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadCurrencies() {
        api.currencies().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<CurrencyOption>> call, Response<List<CurrencyOption>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currencies = response.body();
                    currencyAdapter.clear();
                    currencyAdapter.addAll(currencies);
                    if (companyId > 0) {
                        loadCompany();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<CurrencyOption>> call, Throwable t) {
                Toast.makeText(CompanyFormActivity.this, "Could not load currencies: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCompany() {
        setLoading(true);
        api.getCompany(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Company> call, Response<Company> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    bind(response.body());
                } else {
                    Toast.makeText(CompanyFormActivity.this, ApiErrors.message(response, "Could not load company"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Company> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CompanyFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bind(Company c) {
        binding.nameInput.setText(c.name());
        binding.registrationNumberInput.setText(c.registrationNumber());
        binding.vatIdInput.setText(c.vatId());
        binding.addressLineInput.setText(c.addressLine());
        binding.cityInput.setText(c.city());
        binding.postalCodeInput.setText(c.postalCode());
        binding.countryInput.setText(c.country());
        binding.invoicingPlaceInput.setText(c.invoicingPlace());
        binding.bankNameInput.setText(c.bankName());
        binding.ibanInput.setText(c.iban());
        binding.bicInput.setText(c.bic());
        binding.vatPercentInput.setText(c.defaultVatPercent() != null ? c.defaultVatPercent().toPlainString() : "0");
        binding.vatExemptTextInput.setText(c.vatExemptText());

        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).code().equals(c.defaultCurrency())) {
                binding.currencySpinner.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        String name = text(binding.nameInput);
        String addressLine = text(binding.addressLineInput);
        String city = text(binding.cityInput);
        String postalCode = text(binding.postalCodeInput);
        String country = text(binding.countryInput);

        if (name.isEmpty() || addressLine.isEmpty() || city.isEmpty() || postalCode.isEmpty() || country.isEmpty()) {
            Toast.makeText(this, "Fill in the required fields (marked *)", Toast.LENGTH_LONG).show();
            return;
        }
        if (currencies == null || currencies.isEmpty() || binding.currencySpinner.getSelectedItem() == null) {
            Toast.makeText(this, "Currencies are still loading, try again in a moment", Toast.LENGTH_SHORT).show();
            return;
        }

        BigDecimal vatPercent;
        try {
            String raw = text(binding.vatPercentInput);
            vatPercent = raw.isEmpty() ? BigDecimal.ZERO : new BigDecimal(raw);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "VAT % must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        String currencyCode = ((CurrencyOption) binding.currencySpinner.getSelectedItem()).code();
        CompanyRequest request = new CompanyRequest(
                name, addressLine, city, postalCode, country,
                emptyToNull(text(binding.registrationNumberInput)),
                emptyToNull(text(binding.vatIdInput)),
                emptyToNull(text(binding.bankNameInput)),
                emptyToNull(text(binding.ibanInput)),
                emptyToNull(text(binding.bicInput)),
                currencyCode,
                vatPercent,
                emptyToNull(text(binding.vatExemptTextInput)),
                emptyToNull(text(binding.invoicingPlaceInput)));

        setLoading(true);
        Call<Company> call = companyId > 0 ? api.updateCompany(companyId, request) : api.createCompany(request);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Company> call, Response<Company> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(CompanyFormActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(CompanyFormActivity.this, ApiErrors.message(response, "Could not save company"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Company> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CompanyFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete company")
                .setMessage("This will remove the company along with its clients, projects and invoices. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> delete())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void delete() {
        setLoading(true);
        api.deleteCompany(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    finish();
                } else {
                    Toast.makeText(CompanyFormActivity.this, ApiErrors.message(response, "Could not delete company"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(CompanyFormActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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
