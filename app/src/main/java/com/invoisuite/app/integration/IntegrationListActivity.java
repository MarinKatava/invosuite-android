package com.invoisuite.app.integration;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.invoisuite.app.R;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityIntegrationListBinding;
import com.invoisuite.app.model.IntegrationConnection;
import com.invoisuite.app.model.SyncResult;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/** The signed-in user's time-tracking connections, plus the "import now" action for all of them. */
public class IntegrationListActivity extends BaseActivity {

    private ActivityIntegrationListBinding binding;
    private ApiService api;
    private IntegrationAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntegrationListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        api = ApiClient.get(this);
        adapter = new IntegrationAdapter(c -> startActivity(new Intent(this, IntegrationFormActivity.class)
                .putExtra(IntegrationFormActivity.EXTRA_INTEGRATION_ID, c.id())));

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.addButton.setOnClickListener(v -> startActivity(new Intent(this, IntegrationFormActivity.class)));
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_import_all) {
                importAll();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        binding.swipeRefresh.setRefreshing(true);
        api.listIntegrations().enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<IntegrationConnection>> call, Response<List<IntegrationConnection>> response) {
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submitList(response.body());
                    binding.emptyState.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(IntegrationListActivity.this, ApiErrors.message(response, "Could not load connections"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<IntegrationConnection>> call, Throwable t) {
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(IntegrationListActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void importAll() {
        binding.progressBar.setVisibility(View.VISIBLE);
        api.syncAllIntegrations(null).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<SyncResult>> call, Response<List<SyncResult>> response) {
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    SyncReport.show(IntegrationListActivity.this, response.body(), null);
                    load();
                } else {
                    Toast.makeText(IntegrationListActivity.this, ApiErrors.message(response, "Import failed"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<SyncResult>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(IntegrationListActivity.this, "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
