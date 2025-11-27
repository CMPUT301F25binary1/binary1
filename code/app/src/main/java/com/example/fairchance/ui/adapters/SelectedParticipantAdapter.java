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
 * Re-usable card for:
 *  - Selected participants on Sampling & Replacement screen
 *  - Replacement pool entries on Sampling & Replacement screen
 *  - Chosen entrants list (with button hidden if listener == null)
 */
public class SelectedParticipantAdapter
        extends RecyclerView.Adapter<SelectedParticipantAdapter.ViewHolder> {

    public interface OnParticipantButtonClickListener {
        void onParticipantButtonClick(String entrantId);
    }

    private final List<String> participantIds;
    private String eventName;
    private String buttonText = "";
    private final OnParticipantButtonClickListener listener;

    public SelectedParticipantAdapter(List<String> participantIds,
                                      String eventName,
                                      String buttonText,
                                      boolean showButton,
                                      OnParticipantButtonClickListener listener)
    {
        this.participantIds = participantIds;
        this.eventName = eventName;
        this.buttonText = buttonText;
        this.listener = listener;
    }

    /** Allow parent fragment to update the event name after it loads. */
    public void setEventName(String eventName) {
        this.eventName = eventName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_participant, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String id = participantIds.get(position);

        // Simple display:
        holder.tvParticipantName.setText("User ID: " + id);

        if (eventName != null && !eventName.isEmpty()) {
            holder.tvEventName.setText(eventName);
            holder.tvEventName.setVisibility(View.VISIBLE);
        } else {
            holder.tvEventName.setText("");
            holder.tvEventName.setVisibility(View.GONE);
        }

        if (listener != null) {
            holder.btnNotifyEntrant.setVisibility(View.VISIBLE);
            holder.btnNotifyEntrant.setText(buttonText);
            holder.btnNotifyEntrant.setOnClickListener(v ->
                    listener.onParticipantButtonClick(id));
        } else {
            // Used e.g. on ChosenEntrants screen where there is
            // only the big "Send Notifications" button.
            holder.btnNotifyEntrant.setVisibility(View.GONE);
            holder.btnNotifyEntrant.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return participantIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivParticipantIcon;
        TextView tvParticipantName;
        TextView tvEventName;
        Button btnNotifyEntrant;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivParticipantIcon = itemView.findViewById(R.id.ivParticipantIcon);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            btnNotifyEntrant = itemView.findViewById(R.id.btnNotifyEntrant);
        }
    }
}
