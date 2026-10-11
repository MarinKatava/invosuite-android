package com.invoisuite.app.integration;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.invoisuite.app.api.ApiClient;
import com.invoisuite.app.api.ApiService;
import com.invoisuite.app.common.ApiErrors;
import com.invoisuite.app.databinding.FragmentTimeEntriesBinding;
import com.invoisuite.app.model.SyncResult;
import com.invoisuite.app.model.TimeEntry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.math.BigDecimal;
import java.util.List;

/** Imported time bookings for a company, with the one-tap "Import time bookings" button. */
public class TimeEntriesFragment extends Fragment {

    private static final String ARG_COMPANY_ID = "company_id";

    private FragmentTimeEntriesBinding binding;
    private ApiService api;
    private TimeEntryAdapter adapter;
    private long companyId;

    public static TimeEntriesFragment newInstance(long companyId) {
        Bundle args = new Bundle();
        args.putLong(ARG_COMPANY_ID, companyId);
        TimeEntriesFragment fragment = new TimeEntriesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTimeEntriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        companyId = requireArguments().getLong(ARG_COMPANY_ID);
        api = ApiClient.get(requireContext());
        adapter = new TimeEntryAdapter();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.importButton.setOnClickListener(v -> importBookings());
        binding.setupButton.setOnClickListener(v -> openSettings());
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void openSettings() {
        startActivity(new Intent(getContext(), IntegrationListActivity.class));
    }

    private void load() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(true);
        api.listTimeEntries(companyId, null, null).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<TimeEntry>> call, Response<List<TimeEntry>> response) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    show(response.body());
                } else {
                    Toast.makeText(getContext(), ApiErrors.message(response, "Could not load time bookings"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<TimeEntry>> call, Throwable t) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void show(List<TimeEntry> entries) {
        adapter.submitList(entries);
        binding.emptyState.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal unbilled = BigDecimal.ZERO;
        for (TimeEntry e : entries) {
            total = total.add(e.hours());
            if (!e.invoiced()) unbilled = unbilled.add(e.hours());
        }
        binding.summaryText.setText(entries.isEmpty()
                ? "Last 90 days"
                : "Last 90 days · " + entries.size() + " bookings · " + fmt(total) + " h total · " + fmt(unbilled) + " h not billed yet");
    }

    private String fmt(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void importBookings() {
        binding.importButton.setEnabled(false);
        binding.importButton.setText("Importing…");
        api.syncAllIntegrations(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<SyncResult>> call, Response<List<SyncResult>> response) {
                if (binding == null) return;
                resetButton();
                if (response.isSuccessful() && response.body() != null) {
                    SyncReport.show(requireContext(), response.body(), TimeEntriesFragment.this::openSettings);
                    load();
                } else {
                    Toast.makeText(getContext(), ApiErrors.message(response, "Import failed"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<SyncResult>> call, Throwable t) {
                if (binding == null) return;
                resetButton();
                Toast.makeText(getContext(), "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetButton() {
        binding.importButton.setEnabled(true);
        binding.importButton.setText("Import time bookings");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
