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
import com.example.fairchance.models.Event;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> eventList;

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
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
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                eventDate.setText(sdf.format(event.getEventDate()));
            } else {
                eventDate.setText("Date not set");
            }

            // TODO: Load image with Glide or Picasso
            // Example: Glide.with(itemView.getContext()).load(event.getPosterImageUrl()).into(eventImage);

            // TODO: Set click listeners for Join and Details
            // buttonJoin.setOnClickListener(v -> ... );
            // buttonDetails.setOnClickListener(v -> ... );
        }
    }
}