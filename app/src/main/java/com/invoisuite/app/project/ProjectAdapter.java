package com.invoisuite.app.project;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.invoisuite.app.databinding.ItemProjectBinding;
import com.invoisuite.app.model.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    private final List<Project> items = new ArrayList<>();
    private final OnProjectClickListener listener;

    public ProjectAdapter(OnProjectClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Project> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemProjectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project project = items.get(position);
        holder.binding.nameText.setText(project.name());
        StringBuilder detail = new StringBuilder(project.defaultUnit());
        if (project.defaultRate() != null) {
            detail.append(" · ").append(project.defaultRate().stripTrailingZeros().toPlainString());
            if (project.currency() != null) detail.append(" ").append(project.currency());
        }
        if (project.clientName() != null) {
            detail.append(" · ").append(project.clientName());
        }
        if (!project.active()) {
            detail.append(" · inactive");
        }
        holder.binding.detailText.setText(detail.toString());
        holder.binding.getRoot().setOnClickListener(v -> listener.onProjectClick(project));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemProjectBinding binding;

        ViewHolder(ItemProjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
