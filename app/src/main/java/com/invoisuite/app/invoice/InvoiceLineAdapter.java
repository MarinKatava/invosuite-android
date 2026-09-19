package com.invoisuite.app.invoice;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemInvoiceLineFormBinding;
import com.invoisuite.app.model.Project;

import java.util.ArrayList;
import java.util.List;

/** Editable list of {@link InvoiceLineDraft} rows shown inside the invoice form. */
public class InvoiceLineAdapter extends RecyclerView.Adapter<InvoiceLineAdapter.ViewHolder> {

    public interface OnLinesChangedListener {
        void onLinesChanged();
    }

    private final List<InvoiceLineDraft> items;
    private final OnLinesChangedListener listener;
    private List<Project> projects = new ArrayList<>();
    private List<String> units = new ArrayList<>();

    public InvoiceLineAdapter(List<InvoiceLineDraft> items, OnLinesChangedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
        notifyDataSetChanged();
    }

    public void setUnits(List<String> units) {
        this.units = units;
        notifyDataSetChanged();
    }

    public void addLine() {
        items.add(new InvoiceLineDraft());
        notifyItemInserted(items.size() - 1);
        listener.onLinesChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemInvoiceLineFormBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InvoiceLineDraft line = items.get(position);
        ItemInvoiceLineFormBinding b = holder.binding;

        // --- project spinner ---
        List<String> projectLabels = new ArrayList<>();
        projectLabels.add("Custom line item");
        for (Project p : projects) projectLabels.add(p.name());
        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(
                b.getRoot().getContext(), android.R.layout.simple_spinner_dropdown_item, projectLabels);
        b.projectSpinner.setAdapter(projectAdapter);
        int projectIndex = 0;
        if (line.projectId != null) {
            for (int i = 0; i < projects.size(); i++) {
                if (projects.get(i).id().equals(line.projectId)) {
                    projectIndex = i + 1;
                    break;
                }
            }
        }
        b.projectSpinner.setSelection(projectIndex, false);
        b.projectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return;
                InvoiceLineDraft current = items.get(adapterPos);
                if (pos == 0) {
                    current.projectId = null;
                } else {
                    Project selected = projects.get(pos - 1);
                    current.projectId = selected.id();
                    current.projectName = selected.name();
                    current.unit = selected.defaultUnit() != null ? selected.defaultUnit() : current.unit;
                    if (selected.defaultRate() != null && current.unitPrice.isEmpty()) {
                        current.unitPrice = selected.defaultRate().stripTrailingZeros().toPlainString();
                    }
                    notifyItemChanged(adapterPos);
                    listener.onLinesChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // --- unit spinner ---
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                b.getRoot().getContext(), android.R.layout.simple_spinner_dropdown_item, units);
        b.unitSpinner.setAdapter(unitAdapter);
        int unitIndex = units.indexOf(line.unit);
        b.unitSpinner.setSelection(Math.max(unitIndex, 0), false);
        b.unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                int adapterPos = holder.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION || pos < 0 || pos >= units.size()) return;
                items.get(adapterPos).unit = units.get(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // --- text fields ---
        setText(b.descriptionInput, line.projectName);
        bindWatcher(b.descriptionInput, holder, value -> items.get(positionOf(holder)).projectName = value);

        setText(b.quantityInput, line.quantity);
        bindWatcher(b.quantityInput, holder, value -> {
            items.get(positionOf(holder)).quantity = value;
            updateLineTotal(b, items.get(positionOf(holder)));
            listener.onLinesChanged();
        });

        setText(b.unitPriceInput, line.unitPrice);
        bindWatcher(b.unitPriceInput, holder, value -> {
            items.get(positionOf(holder)).unitPrice = value;
            updateLineTotal(b, items.get(positionOf(holder)));
            listener.onLinesChanged();
        });

        updateLineTotal(b, line);

        b.removeButton.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;
            items.remove(adapterPos);
            notifyItemRemoved(adapterPos);
            listener.onLinesChanged();
        });
    }

    private int positionOf(ViewHolder holder) {
        int pos = holder.getBindingAdapterPosition();
        return pos == RecyclerView.NO_POSITION ? 0 : pos;
    }

    private void updateLineTotal(ItemInvoiceLineFormBinding b, InvoiceLineDraft line) {
        b.lineTotalText.setText(line.lineTotal().stripTrailingZeros().toPlainString());
    }

    private void setText(com.google.android.material.textfield.TextInputEditText field, String value) {
        if (!field.getText().toString().equals(value)) {
            field.setText(value);
        }
    }

    private interface TextConsumer {
        void accept(String value);
    }

    private void bindWatcher(com.google.android.material.textfield.TextInputEditText field, ViewHolder holder, TextConsumer consumer) {
        Object previous = field.getTag();
        if (previous instanceof TextWatcher) {
            field.removeTextChangedListener((TextWatcher) previous);
        }
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) return;
                consumer.accept(s.toString());
            }
        };
        field.addTextChangedListener(watcher);
        field.setTag(watcher);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemInvoiceLineFormBinding binding;

        ViewHolder(ItemInvoiceLineFormBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
