package com.invoisuite.app.company;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.invoisuite.app.R;
import com.invoisuite.app.client.ClientFormActivity;
import com.invoisuite.app.client.ClientsFragment;
import com.invoisuite.app.common.BaseActivity;
import com.invoisuite.app.databinding.ActivityCompanyHomeBinding;
import com.invoisuite.app.invoice.InvoiceFormActivity;
import com.invoisuite.app.invoice.InvoicesFragment;
import com.invoisuite.app.project.ProjectFormActivity;
import com.invoisuite.app.project.ProjectsFragment;

public class CompanyHomeActivity extends BaseActivity {

    public static final String EXTRA_COMPANY_ID = "company_id";
    public static final String EXTRA_COMPANY_NAME = "company_name";

    private long companyId;
    private int selectedTab = R.id.nav_clients;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompanyHomeBinding binding = ActivityCompanyHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        companyId = getIntent().getLongExtra(EXTRA_COMPANY_ID, -1);
        String companyName = getIntent().getStringExtra(EXTRA_COMPANY_NAME);
        binding.toolbar.setTitle(companyName != null ? companyName : "Company");

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit_company) {
                startActivity(new Intent(this, CompanyFormActivity.class)
                        .putExtra(CompanyFormActivity.EXTRA_COMPANY_ID, companyId));
                return true;
            }
            return false;
        });

        binding.bottomNav.setOnItemSelectedListener(item -> {
            selectedTab = item.getItemId();
            showFragmentFor(selectedTab);
            return true;
        });

        binding.addButton.setOnClickListener(v -> {
            if (selectedTab == R.id.nav_clients) {
                startActivity(new Intent(this, ClientFormActivity.class).putExtra(ClientFormActivity.EXTRA_COMPANY_ID, companyId));
            } else if (selectedTab == R.id.nav_projects) {
                startActivity(new Intent(this, ProjectFormActivity.class).putExtra(ProjectFormActivity.EXTRA_COMPANY_ID, companyId));
            } else {
                startActivity(new Intent(this, InvoiceFormActivity.class).putExtra(InvoiceFormActivity.EXTRA_COMPANY_ID, companyId));
            }
        });

        showFragmentFor(selectedTab);
    }

    private void showFragmentFor(int tabId) {
        Fragment fragment;
        if (tabId == R.id.nav_clients) {
            fragment = ClientsFragment.newInstance(companyId);
        } else if (tabId == R.id.nav_projects) {
            fragment = ProjectsFragment.newInstance(companyId);
        } else {
            fragment = InvoicesFragment.newInstance(companyId);
        }
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragmentContainer, fragment);
        tx.commit();
    }
}
