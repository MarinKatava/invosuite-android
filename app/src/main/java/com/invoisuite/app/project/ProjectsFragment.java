package com.invoisuite.app.project;

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
import com.invoisuite.app.model.Project;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

public class ProjectsFragment extends Fragment {

    private static final String ARG_COMPANY_ID = "company_id";

    private FragmentListBinding binding;
    private ApiService api;
    private ProjectAdapter adapter;
    private long companyId;

    public static ProjectsFragment newInstance(long companyId) {
        Bundle args = new Bundle();
        args.putLong(ARG_COMPANY_ID, companyId);
        ProjectsFragment fragment = new ProjectsFragment();
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
        adapter = new ProjectAdapter(this::openProject);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.emptyState.setText("No projects yet.\nTap + to add the first one.");
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    public void load() {
        if (binding == null) return;
        binding.swipeRefresh.setRefreshing(true);
        api.listProjects(companyId).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<Project>> call, Response<List<Project>> response) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submitList(response.body());
                    binding.emptyState.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(getContext(), ApiErrors.message(response, "Could not load projects"), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Project>> call, Throwable t) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(getContext(), "Network error: " + t.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openProject(Project project) {
        startActivity(new Intent(getContext(), ProjectFormActivity.class)
                .putExtra(ProjectFormActivity.EXTRA_COMPANY_ID, companyId)
                .putExtra(ProjectFormActivity.EXTRA_PROJECT_ID, project.id()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
