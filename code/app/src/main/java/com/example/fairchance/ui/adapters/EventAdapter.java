package com.example.fairchance.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // <-- ADD THIS IMPORT
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.EventDetailsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
        this.eventListFull = new ArrayList<>(eventList);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public void setEvents(List<Event> events) {
        this.eventList = events;
        this.eventListFull = new ArrayList<>(events);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return eventFilter;
    }

    private Filter eventFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Event> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(eventListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Event item : eventListFull) {
                    if (item.getName().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            eventList.clear();
            eventList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };


    // ViewHolder class
    class EventViewHolder extends RecyclerView.ViewHolder {
        private ImageView eventImage;
        private TextView eventTitle, eventDate;
        private Button buttonJoin;
        private TextView buttonDetails;

        EventViewHolder(View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_image);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
            buttonJoin = itemView.findViewById(R.id.button_join);
            buttonDetails = itemView.findViewById(R.id.button_details);
        }

        void bind(Event event) {
            eventTitle.setText(event.getName());

            // Format the date
            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyY 'at' hh:mm a", Locale.getDefault());
                eventDate.setText(sdf.format(event.getEventDate()));
            } else {
                eventDate.setText("Date not set");
            }

            // --- START: MODIFIED CODE ---
            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.fairchance_logo_with_words___transparent) // Optional: show logo while loading
                    .error(R.drawable.fairchance_logo_with_words___transparent) // Optional: show logo if it fails
                    .into(eventImage);
            // --- END: MODIFIED CODE ---

            // Create the click listener to navigate to details
            View.OnClickListener detailsClickListener = v -> {
                Context context = itemView.getContext();
                Intent intent = new Intent(context, EventDetailsActivity.class);
                intent.putExtra("EVENT_ID", event.getEventId());
                context.startActivity(intent);
            };

            // Set click listeners for Join, Details, and the whole card
            buttonJoin.setOnClickListener(detailsClickListener);
            buttonDetails.setOnClickListener(detailsClickListener);
            itemView.setOnClickListener(detailsClickListener);
        }
    }
}