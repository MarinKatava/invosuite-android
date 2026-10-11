package com.invoisuite.app.integration;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemIntegrationBinding;
import com.invoisuite.app.model.IntegrationConnection;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class IntegrationAdapter extends RecyclerView.Adapter<IntegrationAdapter.ViewHolder> {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public interface OnClick {
        void onClick(IntegrationConnection connection);
    }

    private final List<IntegrationConnection> items = new ArrayList<>();
    private final OnClick listener;

    public IntegrationAdapter(OnClick listener) {
        this.listener = listener;
    }

    public void submitList(List<IntegrationConnection> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemIntegrationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IntegrationConnection c = items.get(position);
        holder.binding.nameText.setText(c.name());
        com.invoisuite.app.common.StatusStyle.apply(holder.binding.stateText, c.active() ? "ACTIVE" : "PAUSED");
        holder.binding.detailText.setText(host(c.baseUrl()) + " · " + c.companyName()
                + (c.clientName() != null ? " → " + c.clientName() : ""));

        String sync;
        if (c.lastSyncAt() == null) {
            sync = "Not imported yet";
        } else {
            String when = c.lastSyncAt().format(WHEN);
            sync = "FAILED".equals(c.lastSyncStatus())
                    ? "Last import failed (" + when + "): " + c.lastSyncMessage()
                    : "Last import " + when + " · " + c.lastSyncMessage();
        }
        holder.binding.syncText.setText(sync);
        holder.binding.getRoot().setOnClickListener(v -> listener.onClick(c));
    }

    private String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null ? host : url;
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemIntegrationBinding binding;

        ViewHolder(ItemIntegrationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
