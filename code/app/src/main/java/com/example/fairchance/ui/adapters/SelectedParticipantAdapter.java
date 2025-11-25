package com.example.fairchance.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.R;

import java.util.List;

/**
 * Adapter to display selected entrants in the Sampling and Replacement screen.
 * For US 02.05.02 we only need to show the user ID and event name.
 */
public class SelectedParticipantAdapter
        extends RecyclerView.Adapter<SelectedParticipantAdapter.SelectedViewHolder> {

    private final List<String> selectedIds;
    private String eventName;   // will be set from the fragment

    public SelectedParticipantAdapter(List<String> selectedIds) {
        this.selectedIds = selectedIds;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SelectedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_participant, parent, false);
        return new SelectedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedViewHolder holder, int position) {
        String userId = selectedIds.get(position);

        holder.tvParticipantName.setText("User ID: " + userId);

        if (eventName != null && !eventName.isEmpty()) {
            holder.tvEventName.setText(eventName);
        } else {
            holder.tvEventName.setText("Name of Event");
        }

        // Notify button will be wired for US 02.05.01, so no logic yet.
        holder.btnNotifyEntrant.setOnClickListener(v -> {
            // TODO for US 02.05.01 – Send notification to chosen entrants
        });
    }

    @Override
    public int getItemCount() {
        return selectedIds.size();
    }

    static class SelectedViewHolder extends RecyclerView.ViewHolder {
        ImageView ivParticipantIcon;
        TextView tvParticipantName;
        TextView tvEventName;
        Button btnNotifyEntrant;

        SelectedViewHolder(@NonNull View itemView) {
            super(itemView);
            ivParticipantIcon = itemView.findViewById(R.id.ivParticipantIcon);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            btnNotifyEntrant = itemView.findViewById(R.id.btnNotifyEntrant);
        }
    }
}
