package com.invoisuite.app.invoice;

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
import com.invoisuite.app.databinding.FragmentListBinding;
import com.invoisuite.app.model.InvoiceSummary;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class InvoicesFragment extends Fragment {

    private static final String ARG_COMPANY_ID = "company_id";

    private FragmentListBinding binding;
    private ApiService api;
    private InvoiceAdapter adapter;
    private long companyId;

    public static InvoicesFragment newInstance(long companyId) {
        Bundle args = new Bundle();
        args.putLong(ARG_COMPANY_ID, companyId);
        InvoicesFragment fragment = new InvoicesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        companyId = requireArguments().getLong(ARG_COMPANY_ID);
        api = ApiClient.get(requireContext());
        adapter = new InvoiceAdapter(this::openInvoice);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.emptyState.setText("No invoices yet.\nTap + to create the first one.");
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    public void load() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(true);
        api.listInvoices(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<InvoiceSummary>> call, Response<List<InvoiceSummary>> response) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submitList(response.body());
                    binding.emptyState.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(getContext(), ApiErrors.message(response, "Could not load invoices"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<InvoiceSummary>> call, Throwable t) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openInvoice(InvoiceSummary invoice) {
        startActivity(new Intent(getContext(), InvoiceDetailActivity.class)
                .putExtra(InvoiceDetailActivity.EXTRA_COMPANY_ID, companyId)
                .putExtra(InvoiceDetailActivity.EXTRA_INVOICE_ID, invoice.id()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
