package com.example.fairchance.ui.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.fragments.EventDetailsFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Admin Event Management list.
 * Shows each event with its poster, name, description,
 * and lets the admin view details or remove the event.
 */
public class AdminEventAdapter extends RecyclerView.Adapter<AdminEventAdapter.AdminEventViewHolder> {

    private final List<Event> events = new ArrayList<>();
    private final EventRepository repo = new EventRepository();

    /** Replace the entire list of events. */
    public void setEvents(List<Event> newEvents) {
        events.clear();
        if (newEvents != null) {
            events.addAll(newEvents);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new AdminEventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminEventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class AdminEventViewHolder extends RecyclerView.ViewHolder {

        ImageView ivEventImage;
        TextView tvEventName, tvEventDescription;
        Button btnViewDetails, btnRemoveEvent;

        AdminEventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventImage = itemView.findViewById(R.id.ivEventImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDescription = itemView.findViewById(R.id.tvEventDescription);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnRemoveEvent = itemView.findViewById(R.id.btnRemoveEvent);
        }

        void bind(Event event) {
            Context ctx = itemView.getContext();

            // Basic text fields
            tvEventName.setText(
                    event.getName() != null ? event.getName() : "Untitled Event"
            );
            tvEventDescription.setText(
                    event.getDescription() != null
                            ? event.getDescription()
                            : "No description provided"
            );

            // Load poster image from posterImageUrl (can be null)
            Glide.with(ctx)
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                    .error(R.drawable.fairchance_logo_with_words___transparent)
                    .into(ivEventImage);

            // Open EventDetailsFragment
            View.OnClickListener openDetails = v -> {
                if (!(ctx instanceof FragmentActivity)) return;
                FragmentActivity act = (FragmentActivity) ctx;
                EventDetailsFragment frag = EventDetailsFragment.newInstance(event.getEventId());
                FragmentTransaction ft = act.getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, frag);
                ft.addToBackStack(null);
                ft.commit();
            };

            btnViewDetails.setOnClickListener(openDetails);
            itemView.setOnClickListener(openDetails);

            // Remove event (and associated data via repository)
            btnRemoveEvent.setOnClickListener(v -> {
                new AlertDialog.Builder(ctx)
                        .setTitle("Remove Event")
                        .setMessage("Are you sure you want to remove this event and its associated data?")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            repo.deleteEvent(event.getEventId(), new EventRepository.EventTaskCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(ctx, "Event removed.", Toast.LENGTH_SHORT).show();
                                    int pos = getBindingAdapterPosition();
                                    if (pos != RecyclerView.NO_POSITION) {
                                        events.remove(pos);
                                        notifyItemRemoved(pos);
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(ctx, "Error: " + message, Toast.LENGTH_LONG).show();
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }
}
