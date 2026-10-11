package com.invoisuite.app.integration;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemTimeEntryBinding;
import com.invoisuite.app.model.TimeEntry;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.ViewHolder> {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");

    private final List<TimeEntry> items = new ArrayList<>();

    public void submitList(List<TimeEntry> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTimeEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeEntry e = items.get(position);
        holder.binding.projectText.setText(e.projectName());
        holder.binding.hoursText.setText(e.hours().stripTrailingZeros().toPlainString() + " h");
        holder.binding.dateText.setText(e.workDate().format(DATE));

        boolean hasDescription = e.description() != null && !e.description().isEmpty();
        holder.binding.descriptionText.setVisibility(hasDescription ? View.VISIBLE : View.GONE);
        if (hasDescription) holder.binding.descriptionText.setText(e.description());

        holder.binding.billedText.setVisibility(e.invoiced() ? View.VISIBLE : View.GONE);
        if (e.invoiced()) holder.binding.billedText.setText("BILLED ON INVOICE " + e.invoiceNumber());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemTimeEntryBinding binding;

        ViewHolder(ItemTimeEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
