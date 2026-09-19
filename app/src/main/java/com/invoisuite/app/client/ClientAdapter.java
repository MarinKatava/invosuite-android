package com.invoisuite.app.client;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemClientBinding;
import com.invoisuite.app.model.Client;

import java.util.ArrayList;
import java.util.List;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.ViewHolder> {

    public interface OnClientClickListener {
        void onClientClick(Client client);
    }

    private final List<Client> items = new ArrayList<>();
    private final OnClientClickListener listener;

    public ClientAdapter(OnClientClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Client> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<Client> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemClientBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Client client = items.get(position);
        holder.binding.nameText.setText(client.name());
        String detail = client.addressLine() + (client.vatId() != null ? " · VAT " + client.vatId() : "");
        holder.binding.detailText.setText(detail);
        holder.binding.getRoot().setOnClickListener(v -> listener.onClientClick(client));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemClientBinding binding;

        ViewHolder(ItemClientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
