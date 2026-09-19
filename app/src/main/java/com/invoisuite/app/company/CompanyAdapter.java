package com.invoisuite.app.company;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemCompanyBinding;
import com.invoisuite.app.model.Company;

import java.util.ArrayList;
import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.ViewHolder> {

    public interface OnCompanyClickListener {
        void onCompanyClick(Company company);
    }

    private final List<Company> items = new ArrayList<>();
    private final OnCompanyClickListener listener;

    public CompanyAdapter(OnCompanyClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Company> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCompanyBinding binding = ItemCompanyBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Company company = items.get(position);
        holder.binding.nameText.setText(company.name());
        holder.binding.detailText.setText(company.city() + ", " + company.country());
        holder.binding.getRoot().setOnClickListener(v -> listener.onCompanyClick(company));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCompanyBinding binding;

        ViewHolder(ItemCompanyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
