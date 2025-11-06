package com.example.fairchance.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable; // <-- ADD THIS
import android.text.TextWatcher; // <-- ADD THIS
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText; // <-- ADD THIS
import android.widget.ImageButton; // <-- ADD THIS
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher; // <-- ADD THIS
import androidx.activity.result.contract.ActivityResultContracts; // <-- ADD THIS
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.AuthRepository; // <-- ADD THIS
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.models.User; // <-- ADD THIS
import com.example.fairchance.ui.EventDetailsActivity; // <-- ADD THIS
import com.example.fairchance.ui.adapters.EventAdapter;
import com.google.zxing.integration.android.IntentIntegrator; // <-- ADD THIS
import com.google.zxing.integration.android.IntentResult; // <-- ADD THIS

import java.util.ArrayList;
import java.util.List;

public class EntrantHomeFragment extends Fragment {

    private static final String TAG = "EntrantHomeFragment";

    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();
    private EventRepository eventRepository;
    private AuthRepository authRepository; // <-- ADD THIS

    // Add ProgressBar and EmptyText
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView tvWelcomeName; // <-- ADD THIS
    private EditText searchEditText; // <-- ADD THIS
    private ImageButton scanButton; // <-- ADD THIS

    // --- ADD THIS LAUNCHER FOR QR CODE SCANNING ---
    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());
                    if (scanResult != null) {
                        if (scanResult.getContents() == null) {
                            Toast.makeText(getContext(), "Cancelled", Toast.LENGTH_LONG).show();
                        } else {
                            String eventId = scanResult.getContents();
                            Log.d(TAG, "Scanned Event ID: " + eventId);
                            // Open EventDetailsActivity with this ID
                            Intent intent = new Intent(getContext(), EventDetailsActivity.class);
                            intent.putExtra("EVENT_ID", eventId);
                            startActivity(intent);
                        }
                    }
                }
            }
    );
    // --- END OF LAUNCHER ---

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the new dashboard layout
        return inflater.inflate(R.layout.entrant_main_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();
        authRepository = new AuthRepository(); // <-- INITIALIZE

        // Find the RecyclerView and new views
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        tvWelcomeName = view.findViewById(R.id.textView3); // <-- FIND WELCOME TEXT
        searchEditText = view.findViewById(R.id.editText); // <-- FIND SEARCH BAR
        scanButton = view.findViewById(R.id.imageButton); // <-- FIND SCAN BUTTON

        // Setup RecyclerView
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList);
        eventsRecyclerView.setAdapter(eventAdapter);

        // --- START OF NEW/MODIFIED CODE ---

        // 1. Load User's Name for Welcome Message
        loadUserProfile();

        // 2. Setup Search Bar
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                eventAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 3. Setup Scan Button
        scanButton.setOnClickListener(v -> {
            // We need to request permission at runtime, but for simplicity
            // we will just launch. The manifest permission is required.
            // For a production app, add runtime permission checks here.
            IntentIntegrator integrator = new IntentIntegrator(requireActivity());
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Event QR Code");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);

            // Use our launcher to start the activity
            qrCodeLauncher.launch(integrator.createScanIntent());
        });

        // 4. Fetch events from Firestore
        loadEvents();

        // --- END OF NEW/MODIFIED CODE ---
    }

    private void loadUserProfile() {
        authRepository.getUserProfile(new AuthRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                if (user.getName() != null && !user.getName().isEmpty()) {
                    tvWelcomeName.setText(user.getName());
                } else {
                    tvWelcomeName.setText("User");
                }
            }
            @Override
            public void onError(String message) {
                Log.e(TAG, "Failed to load user name: " + message);
                tvWelcomeName.setText("User");
            }
        });
    }

    private void loadEvents() {
        showLoading(true);
        eventRepository.getAllEvents(new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                showLoading(false);
                if (events.isEmpty()) {
                    showEmptyView(true);
                } else {
                    showEmptyView(false);
                    // Use the new method in the adapter to set both lists
                    eventAdapter.setEvents(events);
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Log.e(TAG, "Error loading events: " + message);
                Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper methods for loading/empty
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showEmptyView(boolean show) {
        if (show) {
            emptyView.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            eventsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}