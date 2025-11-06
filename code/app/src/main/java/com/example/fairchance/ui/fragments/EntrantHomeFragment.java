package com.example.fairchance.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.AuthRepository;
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.models.User;
import com.example.fairchance.ui.EventDetailsActivity;
import com.example.fairchance.ui.adapters.EventAdapter;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

/**
 * The main home screen for the 'Entrant' role.
 * This fragment displays a welcome message, a QR code scanner button, a search bar,
 * and a list of all upcoming events that the entrant can join.
 */
public class EntrantHomeFragment extends Fragment {

    private static final String TAG = "EntrantHomeFragment";

    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();
    private EventRepository eventRepository;
    private AuthRepository authRepository;

    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView tvWelcomeName;
    private EditText searchEditText;
    private ImageButton scanButton;
    private Button btnAll, btnMusic, btnDance, btnArt, btnTech;
    private Button btnHowItWorks;

    /**
     * ActivityResultLauncher for the QR code scanning intent.
     * Handles the result from the scanner.
     */
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entrant_main_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();
        authRepository = new AuthRepository();

        // Find views
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        tvWelcomeName = view.findViewById(R.id.textView3);
        searchEditText = view.findViewById(R.id.editText);
        scanButton = view.findViewById(R.id.imageButton);
        btnHowItWorks = view.findViewById(R.id.btn_how_it_works);

        btnAll = view.findViewById(R.id.button);
        btnMusic = view.findViewById(R.id.button2);
        btnDance = view.findViewById(R.id.button3);
        btnArt = view.findViewById(R.id.button4);
        btnTech = view.findViewById(R.id.button5);

        // Setup RecyclerView
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList);
        eventsRecyclerView.setAdapter(eventAdapter);

        // Load User's Name for Welcome Message
        loadUserProfile();

        // Setup Search Bar
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

        View.OnClickListener categoryClickListener = v -> {
            String category = "All"; // Default to "All"
            int id = v.getId();

            if (id == R.id.button2) {
                category = "Music";
            } else if (id == R.id.button3) {
                category = "Dance";
            } else if (id == R.id.button4) {
                category = "Art";
            } else if (id == R.id.button5) {
                category = "Tech";
            }

            eventAdapter.setCategory(category);
        };
        btnAll.setOnClickListener(categoryClickListener);
        btnMusic.setOnClickListener(categoryClickListener);
        btnDance.setOnClickListener(categoryClickListener);
        btnArt.setOnClickListener(categoryClickListener);
        btnTech.setOnClickListener(categoryClickListener);

        btnHowItWorks.setOnClickListener(v -> navigateToGuidelines());

        // Setup Scan Button
        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(requireActivity());
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Event QR Code");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);

            qrCodeLauncher.launch(integrator.createScanIntent());
        });

        // Fetch events from Firestore
        loadEvents();

    }

    /**
     * Navigates to the GuidelinesFragment when the "How it Works" button is clicked.
     */
    private void navigateToGuidelines() {
        if (getFragmentManager() != null) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            // Replace the main dashboard container with the guidelines fragment
            transaction.replace(R.id.dashboard_container, new GuidelinesFragment());
            // Add to back stack so the user can press 'Back' to return to EntrantHomeFragment
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    /**
     * Loads the current user's profile to display their name in the welcome message.
     */
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

    /**
     * Fetches the list of all available events from the EventRepository
     * and populates the RecyclerView.
     */
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

    /**
     * Helper method to show/hide the main progress bar.
     * @param isLoading True to show loading state, false otherwise.
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            eventsRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Helper method to show/hide the "No events found" text.
     * @param show True to show the empty view, false to show the RecyclerView.
     */
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