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

import com.bumptech.glide.Glide;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.EventDetailsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;

/**
 * A RecyclerView.Adapter for displaying a filterable list of Event objects.
 * This adapter is used in the EntrantHomeFragment to show all available events.
 * It implements Filterable to allow for real-time searching.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull; // A copy of the full list for filtering
    private String currentCategory = "All";
    private String currentSearchText = "";
    private String currentDateFilter = "ALL";

    /**
     * Constructs a new EventAdapter.
     *
     * @param eventList The initial list of events to display.
     */
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

    /**
     * Updates the adapter's base data source and re-applies current filters.
     * Called when real-time updates are received (US 01.01.03 Criterion 6).
     *
     * @param events The new list of events.
     */
    public void updateBaseEventsAndRefilter(List<Event> events) {
        this.eventListFull = new ArrayList<>(events);
        getFilter().filter(this.currentSearchText);
    }

    /**
     * Updates the adapter's data source and refreshes the full list for filtering.
     *
     * @param events The new list of events.
     */
    public void setEvents(List<Event> events) {
        this.eventList = events;
        this.eventListFull = new ArrayList<>(events);
        this.currentCategory = "All";
        this.currentSearchText = "";
        notifyDataSetChanged();
    }

    /**
     * Sets the category to filter by and applies the filter.
     *
     * @param category The category to filter (e.g., "All", "Sports", "Music", etc.).
     */
    public void setCategory(String category) {
        this.currentCategory = category;
        getFilter().filter(this.currentSearchText);
    }

    /**
     * Sets the date filter and applies the combined filter (US 01.01.04).
     *
     * @param dateFilter The date filter to apply (e.g., "ALL", "TODAY").
     */
    public void setDateFilter(String dateFilter) {
        this.currentDateFilter = dateFilter;
        getFilter().filter(this.currentSearchText); // Use the existing search constraint
    }

    /**
     * Returns the Filter object that can be used to filter the list.
     *
     * @return A Filter for performing searches.
     */
    @Override
    public Filter getFilter() {
        return eventFilter;
    }

    private Filter eventFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Event> filteredList = new ArrayList<>();

            currentSearchText = constraint.toString().toLowerCase().trim();
            String currentFilterCategory = currentCategory;
            String currentFilterDate = currentDateFilter;

            // Prepare for date comparison (for simplicity, only checking TODAY)
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            for (Event item : eventListFull) {
                boolean categoryMatches = currentFilterCategory.equals("All") || (item.getCategory() != null && item.getCategory().equalsIgnoreCase(currentFilterCategory));
                boolean searchMatches = currentSearchText.isEmpty() || item.getName().toLowerCase().contains(currentSearchText);

                // Date Filtering Logic (New for US 01.01.04)
                boolean dateMatches = true;
                if (currentFilterDate.equals("TODAY") && item.getEventDate() != null) {
                    Calendar eventCal = Calendar.getInstance();
                    eventCal.setTime(item.getEventDate());
                    eventCal.set(Calendar.HOUR_OF_DAY, 0);
                    eventCal.set(Calendar.MINUTE, 0);
                    eventCal.set(Calendar.SECOND, 0);
                    eventCal.set(Calendar.MILLISECOND, 0);

                    dateMatches = eventCal.equals(today);
                }

                if (categoryMatches && searchMatches && dateMatches) {
                    filteredList.add(item);
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


    /**
     * ViewHolder class for an individual event card.
     */
    class EventViewHolder extends RecyclerView.ViewHolder {
        private ImageView eventImage;
        // Updated for US 01.01.03
        private TextView eventTitle, eventDate, eventLocation, eventStatus;
        private Button buttonJoin;
        private TextView buttonDetails;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The root view of the item_event_card layout.
         */
        EventViewHolder(View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_image);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
            // Updated for US 01.01.03
            eventLocation = itemView.findViewById(R.id.event_location);
            eventStatus = itemView.findViewById(R.id.event_status);
            buttonJoin = itemView.findViewById(R.id.button_join);
            buttonDetails = itemView.findViewById(R.id.button_details);
        }

        /**
         * Binds an Event object to the views in the ViewHolder.
         *
         * @param event The Event object to display.
         */
        void bind(Event event) {
            eventTitle.setText(event.getName());

            if (event.getEventDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyY 'at' hh:mm a", Locale.getDefault());
                eventDate.setText(sdf.format(event.getEventDate()));
            } else {
                eventDate.setText("Date not set");
            }

            // Updated for US 01.01.03: Populate location
            if (event.getLocation() != null && !event.getLocation().isEmpty()) {
                eventLocation.setText(event.getLocation());
            } else {
                eventLocation.setText("Location not specified");
            }

            // Updated for US 01.01.03: Populate registration status
            eventStatus.setText("Status: Registration Open");


            // Load image with Glide
            Glide.with(itemView.getContext())
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                    .error(R.drawable.fairchance_logo_with_words___transparent)
                    .into(eventImage);

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