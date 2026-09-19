package com.invoisuite.app.invoice;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemInvoiceBinding;
import com.invoisuite.app.model.InvoiceSummary;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceAdapter.ViewHolder> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public interface OnInvoiceClickListener {
        void onInvoiceClick(InvoiceSummary invoice);
    }

    private final List<InvoiceSummary> items = new ArrayList<>();
    private final OnInvoiceClickListener listener;

    public InvoiceAdapter(OnInvoiceClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<InvoiceSummary> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemInvoiceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvoiceSummary invoice = items.get(position);
        holder.binding.nameText.setText(invoice.name());
        holder.binding.statusText.setText(invoice.status());
        holder.binding.detailText.setText(
                invoice.invoiceNumber() + " · " + invoice.clientName() + " · " + invoice.issueDate().format(DATE_FORMAT));
        holder.binding.totalText.setText(
                invoice.total().stripTrailingZeros().toPlainString() + " " + invoice.currency());
        holder.binding.getRoot().setOnClickListener(v -> listener.onInvoiceClick(invoice));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemInvoiceBinding binding;

        ViewHolder(ItemInvoiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
