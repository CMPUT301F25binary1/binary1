package com.example.fairchance.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide; // <-- ADD THIS IMPORT
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";

    private EventRepository eventRepository;
    private String currentEventId;
    private Event currentEvent;
    private String currentEventStatus = null;

    // UI Components
    private ImageView eventPosterImage;
    private ProgressBar progressBar;
    private LinearLayout eventDetailsContent;
    private TextView tvEventName, tvEventDate, tvEventDescription, tvEventGuidelines, tvEventWaitlistCount;
    private Button btnJoinWaitlist;
    private ProgressBar joinProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventRepository = new EventRepository();

        // Get Event ID from Intent
        currentEventId = getIntent().getStringExtra("EVENT_ID");
        if (currentEventId == null) {
            Log.e(TAG, "Event ID is null. Finishing activity.");
            Toast.makeText(this, "Error: Event not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Find Views
        eventPosterImage = findViewById(R.id.event_poster_image);
        progressBar = findViewById(R.id.event_details_progress);
        eventDetailsContent = findViewById(R.id.event_details_content);
        tvEventName = findViewById(R.id.event_name_text);
        tvEventDate = findViewById(R.id.event_date_text);
        tvEventDescription = findViewById(R.id.event_description_text);
        tvEventGuidelines = findViewById(R.id.event_guidelines_text);
        tvEventWaitlistCount = findViewById(R.id.event_waitlist_count_text);
        btnJoinWaitlist = findViewById(R.id.join_waitlist_button);
        joinProgressBar = findViewById(R.id.join_progress);

        // Load data
        loadEventDetails();

        btnJoinWaitlist.setOnClickListener(v -> {
            if (currentEventStatus == null) {
                joinWaitlist();
            } else if ("Waiting".equals(currentEventStatus)) {
                leaveWaitlist();
            }
        });
    }

    private void loadEventDetails() {
        setLoading(true);
        // 1. Load main event data
        eventRepository.getEvent(currentEventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                populateUi(event);

                // 2. After getting event, load waitlist count
                loadWaitlistCount();

                // 3. Check the user's status for this event
                checkUserStatus();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Log.e(TAG, "Error loading event: " + message);
                Toast.makeText(EventDetailsActivity.this, "Failed to load event.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWaitlistCount() {
        eventRepository.getWaitingListCount(currentEventId, new EventRepository.WaitlistCountCallback() {
            @Override
            public void onSuccess(int count) {
                tvEventWaitlistCount.setText(count + " people");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading waitlist count: " + message);
                tvEventWaitlistCount.setText("Error loading count");
            }
        });
    }

    private void checkUserStatus() {
        eventRepository.checkEventHistoryStatus(currentEventId, new EventRepository.EventHistoryCheckCallback() {
            @Override
            public void onSuccess(String status) {
                currentEventStatus = status;
                updateJoinButtonUI();
                setLoading(false);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error checking user status: " + message);
                setLoading(false);
            }
        });
    }

    private void joinWaitlist() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        setButtonLoading(true);
        eventRepository.joinWaitingList(currentEventId, currentEvent, new EventRepository.EventTaskCallback() {
            @Override
            public void onSuccess() {
                setButtonLoading(false);
                currentEventStatus = "Waiting";
                updateJoinButtonUI();
                Toast.makeText(EventDetailsActivity.this, "Joined waitlist!", Toast.LENGTH_SHORT).show();
                loadWaitlistCount();
            }

            @Override
            public void onError(String message) {
                setButtonLoading(false);
                Toast.makeText(EventDetailsActivity.this, "Failed to join: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveWaitlist() {
        setButtonLoading(true);
        eventRepository.leaveWaitingList(currentEventId, new EventRepository.EventTaskCallback() {
            @Override
            public void onSuccess() {
                setButtonLoading(false);
                currentEventStatus = null;
                updateJoinButtonUI();
                Toast.makeText(EventDetailsActivity.this, "Left waitlist.", Toast.LENGTH_SHORT).show();
                loadWaitlistCount();
            }

            @Override
            public void onError(String message) {
                setButtonLoading(false);
                Toast.makeText(EventDetailsActivity.this, "Failed to leave: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUi(Event event) {
        // Populate basic info
        tvEventName.setText(event.getName());
        tvEventDescription.setText(event.getDescription());

        // Format the date
        if (event.getEventDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyY 'at' hh:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getEventDate()));
        } else {
            tvEventDate.setText("Date not set");
        }

        // Populate guidelines
        if (event.getGuidelines() != null && !event.getGuidelines().isEmpty()) {
            tvEventGuidelines.setText(event.getGuidelines());
        } else {
            tvEventGuidelines.setText("No specific guidelines provided.");
        }

        // --- START: MODIFIED CODE ---
        // Load poster image with Glide
        Glide.with(this)
                .load(event.getPosterImageUrl())
                .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                .error(R.drawable.fairchance_logo_with_words___transparent)
                .into(eventPosterImage);
        // --- END: MODIFIED CODE ---
    }

    private void updateJoinButtonUI() {
        if ("Waiting".equals(currentEventStatus)) {
            btnJoinWaitlist.setText("Leave Waitlist");
            btnJoinWaitlist.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
            btnJoinWaitlist.setEnabled(true);
        } else if (currentEventStatus == null) {
            btnJoinWaitlist.setText("Join Waitlist");
            btnJoinWaitlist.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.FCgreen)));
            btnJoinWaitlist.setEnabled(true);
        } else {
            // User is "Confirmed", "Selected", etc.
            btnJoinWaitlist.setText("You are " + currentEventStatus);
            btnJoinWaitlist.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            btnJoinWaitlist.setEnabled(false);
        }
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            eventDetailsContent.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            eventDetailsContent.setVisibility(View.VISIBLE);
        }
    }

    private void setButtonLoading(boolean isLoading) {
        if (isLoading) {
            joinProgressBar.setVisibility(View.VISIBLE);
            btnJoinWaitlist.setText("");
            btnJoinWaitlist.setEnabled(false);
        } else {
            joinProgressBar.setVisibility(View.GONE);
            // The text will be reset by updateJoinButtonUI()
            btnJoinWaitlist.setEnabled(true);
        }
    }
}