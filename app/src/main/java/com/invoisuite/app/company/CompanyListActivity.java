package com.invoisuite.app.company;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.auth.LoginActivity;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityCompanyListBinding;
import com.invoisuite.app.model.Company;
import com.invoisuite.app.session.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class CompanyListActivity extends BaseActivity {

    private ActivityCompanyListBinding binding;
    private ApiService api;
    private CompanyAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompanyListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        adapter = new CompanyAdapter(this::openCompany);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadCompanies);
        binding.addButton.setOnClickListener(v -> startActivity(new Intent(this, CompanyFormActivity.class)));
        binding.toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCompanies();
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == com.invoisuite.app.R.id.action_time_tracking) {
            startActivity(new Intent(this, com.invoisuite.app.integration.IntegrationListActivity.class));
            return true;
        }
        if (item.getItemId() == com.invoisuite.app.R.id.action_sign_out) {
            SessionManager.getInstance(this).clear();
            startActivity(new Intent(this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return true;
        }
        return false;
    }

    private void loadCompanies() {
        binding.swipeRefresh.setRefreshing(true);
        api.listCompanies().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Company>> call, Response<List<Company>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submitList(response.body());
                    binding.emptyState.setVisibility(response.body().isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                } else {
                    Toast.makeText(CompanyListActivity.this, ApiErrors.message(response, "Could not load companies"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Company>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(CompanyListActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openCompany(Company company) {
        startActivity(new Intent(this, CompanyHomeActivity.class)
                .putExtra(CompanyHomeActivity.EXTRA_COMPANY_ID, company.id())
                .putExtra(CompanyHomeActivity.EXTRA_COMPANY_NAME, company.name())
                .putExtra(CompanyHomeActivity.EXTRA_COMPANY_SUBTITLE, company.city() + ", " + company.country()));
    }
}
