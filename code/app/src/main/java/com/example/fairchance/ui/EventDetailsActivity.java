package com.example.fairchance.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.fragments.GuidelinesFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * This Activity displays the detailed information for a single event.
 * It is responsible for:
 * 1. Loading all event data (name, description, poster, etc.) from Firestore.
 * 2. Loading the current waitlist count for the event (now in Real-time).
 * 3. Checking the current user's status for this event (e.g., "Waiting", "Confirmed", or null).
 * 4. Providing the logic for the "Join Waitlist" / "Leave Waitlist" button, including Geolocation handling.
 * 5. Displaying the comprehensive guidelines logic.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private EventRepository eventRepository;
    private String currentEventId;
    private Event currentEvent;
    private String currentEventStatus = null;
    private ListenerRegistration waitlistListener;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView eventPosterImage;
    private ProgressBar progressBar;
    private LinearLayout eventDetailsContent;
    private TextView tvEventName, tvEventDate, tvEventLocation, tvEventDescription, tvEventGuidelines, tvEventWaitlistCount;
    private Button btnJoinWaitlist;
    private ProgressBar joinProgressBar;
    private TextView tvSeeFullGuidelines;
    private FrameLayout guidelinesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventRepository = new EventRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        currentEventId = getIntent().getStringExtra("EVENT_ID");
        if (currentEventId == null) {
            Log.e(TAG, "Event ID is null. Finishing activity.");
            Toast.makeText(this, "Error: Event not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        eventPosterImage = findViewById(R.id.event_poster_image);
        progressBar = findViewById(R.id.event_details_progress);
        eventDetailsContent = findViewById(R.id.event_details_content);
        tvEventName = findViewById(R.id.event_name_text);
        tvEventDate = findViewById(R.id.event_date_text);
        tvEventLocation = findViewById(R.id.event_location_text);
        tvEventDescription = findViewById(R.id.event_description_text);
        tvEventGuidelines = findViewById(R.id.event_guidelines_text);
        tvEventWaitlistCount = findViewById(R.id.event_waitlist_count_text);
        btnJoinWaitlist = findViewById(R.id.join_waitlist_button);
        joinProgressBar = findViewById(R.id.join_progress);
        tvSeeFullGuidelines = findViewById(R.id.tv_see_full_guidelines);
        guidelinesContainer = findViewById(R.id.guidelines_container);

        loadEventDetails();

        btnJoinWaitlist.setOnClickListener(v -> {
            if (currentEventStatus == null) {
                initiateJoinProcess();
            } else if ("Waiting".equals(currentEventStatus)) {
                leaveWaitlist();
            }
        });

        tvSeeFullGuidelines.setOnClickListener(v -> {
            if (guidelinesContainer != null) {
                // Get the actual text from the current event object (default to empty if null)
                String rules = (currentEvent != null && currentEvent.getGuidelines() != null)
                        ? currentEvent.getGuidelines()
                        : "No specific guidelines provided by the organizer.";

                guidelinesContainer.setVisibility(View.VISIBLE);

                // Use newInstance to pass the specific guidelines data
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.guidelines_container, GuidelinesFragment.newInstance(rules))
                        .addToBackStack("guidelines")
                        .commit();
            }
        });

        // Listen for back stack changes to hide the container when empty
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                if (guidelinesContainer != null) {
                    guidelinesContainer.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (waitlistListener != null) {
            waitlistListener.remove();
        }
    }

    /**
     * Kicks off the data loading chain.
     * 1. Fetches main Event data.
     * 2. On success, fetches waitlist count and user status.
     */
    private void loadEventDetails() {
        setLoading(true);
        eventRepository.getEvent(currentEventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                populateUi(event);
                loadWaitlistCount();
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

    /**
     * Fetches the total number of users on the waitlist for this event using a REAL-TIME listener.
     * Updates the UI to show spots taken/left if a limit is set.
     */
    private void loadWaitlistCount() {
        if (waitlistListener != null) {
            waitlistListener.remove();
        }

        waitlistListener = eventRepository.listenToWaitingListCount(currentEventId, new EventRepository.WaitlistCountCallback() {
            @Override
            public void onSuccess(int count) {
                if (currentEvent != null && currentEvent.getWaitingListLimit() > 0) {
                    // Limit exists: Show "Taken / Limit (Left)"
                    long limit = currentEvent.getWaitingListLimit();
                    long left = Math.max(0, limit - count);
                    tvEventWaitlistCount.setText("Waitlist: " + count + " / " + limit + " (" + left + " spots left)");
                } else {
                    // No limit: Show just the count
                    tvEventWaitlistCount.setText(count + " people on waitlist");
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error loading waitlist count: " + message);
                tvEventWaitlistCount.setText("Error loading count");
            }
        });
    }

    /**
     * Checks the current user's status for this specific event
     * (e.g., "Waiting", "Confirmed", or null).
     */
    private void checkUserStatus() {
        eventRepository.checkEventHistoryStatus(currentEventId, new EventRepository.EventHistoryCheckCallback() {
            @Override
            public void onSuccess(String status) {
                currentEventStatus = status;
                updateJoinButtonUI();
                setLoading(false); // Page is fully loaded
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error checking user status: " + message);
                setLoading(false);
            }
        });
    }

    /**
     * Starts the process to join the waitlist.
     * Checks if geolocation is required and branches accordingly.
     */
    private void initiateJoinProcess() {
        if (currentEvent == null) {
            Toast.makeText(this, "Event data not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent.isGeolocationRequired()) {
            checkLocationPermissionAndJoin();
        } else {
            performJoin(null, null);
        }
    }

    /**
     * Checks for location permissions. If granted, fetches location.
     * If denied, requests them.
     */
    private void checkLocationPermissionAndJoin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocation();
        }
    }

    /**
     * Fetches the last known location and then joins the waitlist.
     */
    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        setButtonLoading(true);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        performJoin(location.getLatitude(), location.getLongitude());
                    } else {
                        setButtonLoading(false);
                        Toast.makeText(EventDetailsActivity.this, "Unable to verify location. Ensure GPS is on.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setButtonLoading(false);
                    Log.e(TAG, "Error fetching location", e);
                    Toast.makeText(EventDetailsActivity.this, "Error fetching location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Handles the callback from the permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Location permission is required to join this event.", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Performs the actual repository call to join the waiting list.
     *
     * @param lat Latitude (nullable)
     * @param lng Longitude (nullable)
     */
    private void performJoin(@Nullable Double lat, @Nullable Double lng) {
        setButtonLoading(true);

        EventRepository.EventTaskCallback callback = new EventRepository.EventTaskCallback() {
            @Override
            public void onSuccess() {
                setButtonLoading(false);
                currentEventStatus = "Waiting";
                updateJoinButtonUI();
                Toast.makeText(EventDetailsActivity.this, "Joined waitlist!", Toast.LENGTH_SHORT).show();
                // Count updates automatically via listener
            }

            @Override
            public void onError(String message) {
                setButtonLoading(false);
                Toast.makeText(EventDetailsActivity.this, "Failed to join: " + message, Toast.LENGTH_SHORT).show();
            }
        };

        if (lat != null && lng != null) {
            eventRepository.joinWaitingListWithLocation(currentEventId, currentEvent, lat, lng, callback);
        } else {
            eventRepository.joinWaitingList(currentEventId, currentEvent, callback);
        }
    }

    /**
     * Called when the user clicks the "Leave Waitlist" button.
     * Calls the repository to perform an atomic delete.
     */
    private void leaveWaitlist() {
        setButtonLoading(true);
        eventRepository.leaveWaitingList(currentEventId, new EventRepository.EventTaskCallback() {
            @Override
            public void onSuccess() {
                setButtonLoading(false);
                currentEventStatus = null;
                updateJoinButtonUI();
                Toast.makeText(EventDetailsActivity.this, "Left waitlist.", Toast.LENGTH_SHORT).show();
                // Count updates automatically via listener
            }

            @Override
            public void onError(String message) {
                setButtonLoading(false);
                Toast.makeText(EventDetailsActivity.this, "Failed to leave: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Populates the UI fields with data from the loaded Event object.
     *
     * @param event The loaded Event object.
     */
    private void populateUi(Event event) {
        tvEventName.setText(event.getName());
        tvEventDescription.setText(event.getDescription());

        if (event.getLocation() != null && !event.getLocation().isEmpty()) {
            tvEventLocation.setText(event.getLocation());
        } else {
            tvEventLocation.setText("Location not specified");
        }

        if (event.getEventDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            tvEventDate.setText(sdf.format(event.getEventDate()));
        } else {
            tvEventDate.setText("Date not set");
        }

        if (event.getGuidelines() != null && !event.getGuidelines().isEmpty()) {
            tvEventGuidelines.setText(event.getGuidelines());
        } else {
            tvEventGuidelines.setText("No specific guidelines provided.");
        }

        Glide.with(this)
                .load(event.getPosterImageUrl())
                .placeholder(R.drawable.fairchance_logo_with_words___transparent)
                .error(R.drawable.fairchance_logo_with_words___transparent)
                .into(eventPosterImage);
    }

    /**
     * Updates the main action button based on the user's current status for this event.
     * Hides or shows the button as needed.
     */
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
            // User is "Confirmed", "Selected", "Declined", etc.
            btnJoinWaitlist.setText("You are " + currentEventStatus);
            btnJoinWaitlist.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
            btnJoinWaitlist.setEnabled(false);
        }
    }

    /**
     * Shows/hides the main page content and progress bar.
     *
     * @param isLoading True to show progress bar, false to show content.
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            eventDetailsContent.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            eventDetailsContent.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Shows/hides the progress bar inside the main action button.
     *
     * @param isLoading True to show progress bar, false to hide it.
     */
    private void setButtonLoading(boolean isLoading) {
        if (isLoading) {
            joinProgressBar.setVisibility(View.VISIBLE);
            btnJoinWaitlist.setText("");
            btnJoinWaitlist.setEnabled(false);
        } else {
            joinProgressBar.setVisibility(View.GONE);
            btnJoinWaitlist.setEnabled(true);
            // The text will be reset by updateJoinButtonUI()
        }
    }
}