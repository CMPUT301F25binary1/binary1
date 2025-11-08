package com.example.fairchance.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.fragments.EventDetailsFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> implements Filterable {

    private List<Event> eventList;
    private List<Event> eventListFull;
    private String currentCategory = "All";
    private String currentSearchText = "";
    private String currentDateFilter = "ALL";
    private final EventRepository repo = new EventRepository();

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

    public void updateBaseEventsAndRefilter(List<Event> events) {
        this.eventListFull = new ArrayList<>(events);
        getFilter().filter(this.currentSearchText);
    }

    public void setEvents(List<Event> events) {
        this.eventList = events;
        this.eventListFull = new ArrayList<>(events);
        this.currentCategory = "All";
        this.currentSearchText = "";
        notifyDataSetChanged();
    }

    public void setCategory(String category) {
        this.currentCategory = category;
        getFilter().filter(this.currentSearchText);
    }

    public void setDateFilter(String dateFilter) {
        this.currentDateFilter = dateFilter;
        getFilter().filter(this.currentSearchText);
    }

    @Override
    public Filter getFilter() {
        return eventFilter;
    }

    private final Filter eventFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Event> filteredList = new ArrayList<>();
            currentSearchText = constraint.toString().toLowerCase().trim();

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            for (Event item : eventListFull) {
                boolean categoryMatches = currentCategory.equals("All") ||
                        (item.getCategory() != null && item.getCategory().equalsIgnoreCase(currentCategory));

                boolean searchMatches = currentSearchText.isEmpty() ||
                        item.getName().toLowerCase().contains(currentSearchText);

                boolean dateMatches = true;
                if (currentDateFilter.equals("TODAY") && item.getEventDate() != null) {
                    Calendar eventCal = Calendar.getInstance();
                    eventCal.setTime(item.getEventDate());
                    eventCal.set(Calendar.HOUR_OF_DAY, 0);
                    eventCal.set(Calendar.MINUTE, 0);
                    eventCal.set(Calendar.SECOND, 0);
                    eventCal.set(Calendar.MILLISECOND, 0);
                    dateMatches = eventCal.equals(today);
                }

                if (categoryMatches && searchMatches && dateMatches) filteredList.add(item);
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            eventList.clear();
            eventList.addAll((List<Event>) results.values);
            notifyDataSetChanged();
        }
    };

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ImageView eventImage;
        private final TextView eventTitle, eventDate, eventLocation, eventStatus;
        private final Button buttonJoin;
        private final TextView buttonDetails;

        EventViewHolder(View itemView) {
            super(itemView);
            eventImage = itemView.findViewById(R.id.event_image);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
            eventLocation = itemView.findViewById(R.id.event_location);
            eventStatus = itemView.findViewById(R.id.event_status);
            buttonJoin = itemView.findViewById(R.id.button_join);
            buttonDetails = itemView.findViewById(R.id.button_details);
        }

        void bind(Event event) {
            eventTitle.setText(event.getName());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            eventDate.setText(event.getEventDate() != null ? sdf.format(event.getEventDate()) : "Date not set");
            eventLocation.setText(event.getLocation() != null ? event.getLocation() : "Location not specified");
            eventStatus.setText("Status: Registration Open");

            Glide.with(itemView.getContext())
                    .load(event.getPosterImageUrl())
                    .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                    .error(R.drawable.fairchance_logo_with_words___transparent)
                    .into(eventImage);

            Context context = itemView.getContext();

            // 🔹 Navigate to EventDetailsFragment when clicking card or Details button
            View.OnClickListener openDetails = v -> {
                EventDetailsFragment fragment = EventDetailsFragment.newInstance(event.getEventId());
                FragmentTransaction transaction = ((AppCompatActivity) context)
                        .getSupportFragmentManager()
                        .beginTransaction();
                transaction.replace(R.id.fragment_container, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
            };

            buttonDetails.setOnClickListener(openDetails);
            itemView.setOnClickListener(openDetails);

            // 🔹 Join/Leave Waiting List handling
            repo.checkEventHistoryStatus(event.getEventId(), new EventRepository.EventHistoryCheckCallback() {
                @Override
                public void onSuccess(String status) {
                    if (status != null) {
                        buttonJoin.setText("Leave Waiting List");
                        buttonJoin.setBackgroundTintList(context.getResources().getColorStateList(R.color.FCgreen));
                    } else {
                        buttonJoin.setText("Join Waiting List");
                        buttonJoin.setBackgroundTintList(context.getResources().getColorStateList(R.color.gray));
                    }
                }

                @Override
                public void onError(String message) { }
            });

            buttonJoin.setOnClickListener(v -> {
                String label = buttonJoin.getText().toString();
                if (label.contains("Join")) {
                    repo.joinWaitingList(event.getEventId(), event, new EventRepository.EventTaskCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(context, "Joined waiting list!", Toast.LENGTH_SHORT).show();
                            buttonJoin.setText("Leave Waiting List");
                            buttonJoin.setBackgroundTintList(context.getResources().getColorStateList(R.color.FCgreen));
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    repo.leaveWaitingList(event.getEventId(), new EventRepository.EventTaskCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(context, "Left waiting list.", Toast.LENGTH_SHORT).show();
                            buttonJoin.setText("Join Waiting List");
                            buttonJoin.setBackgroundTintList(context.getResources().getColorStateList(R.color.gray));
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(context, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }
}