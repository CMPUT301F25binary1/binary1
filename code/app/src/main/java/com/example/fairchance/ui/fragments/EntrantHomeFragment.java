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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.AuthRepository;
import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.models.User;
import com.example.fairchance.ui.AuthActivity;
import com.example.fairchance.ui.EventDetailsActivity;
import com.example.fairchance.ui.adapters.EventAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
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
    private ListenerRegistration eventListenerRegistration;

    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView tvWelcomeName;
    private EditText searchEditText;
    private ImageButton scanButton;
    private Button btnAll, btnMusic, btnDance, btnArt, btnTech, btnSports; // ADDED btnSports
    private Button btnHowItWorks;
    private Button btnFilterToday;
    private Button currentCategoryButton;
    private boolean isTodayFilterActive = false;

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

        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        tvWelcomeName = view.findViewById(R.id.textView3);
        searchEditText = view.findViewById(R.id.editText);
        scanButton = view.findViewById(R.id.imageButton);
        btnHowItWorks = view.findViewById(R.id.btn_how_it_works);
        btnFilterToday = view.findViewById(R.id.btn_filter_today);

        btnAll = view.findViewById(R.id.button);
        btnMusic = view.findViewById(R.id.button2);
        btnDance = view.findViewById(R.id.button3);
        btnArt = view.findViewById(R.id.button4);
        btnTech = view.findViewById(R.id.button5);
        btnSports = view.findViewById(R.id.button_sports);

        currentCategoryButton = btnAll;
        updateCategoryButtonAppearance(currentCategoryButton, true);

        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList);
        eventsRecyclerView.setAdapter(eventAdapter);

        loadUserProfile();

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
            Button clickedButton = (Button) v;
            String category = clickedButton.getText().toString();

            if (currentCategoryButton != null) {
                updateCategoryButtonAppearance(currentCategoryButton, false);
            }

            currentCategoryButton = clickedButton;
            updateCategoryButtonAppearance(currentCategoryButton, true);

            eventAdapter.setCategory(category);
        };
        btnAll.setOnClickListener(categoryClickListener);
        btnMusic.setOnClickListener(categoryClickListener);
        btnDance.setOnClickListener(categoryClickListener);
        btnArt.setOnClickListener(categoryClickListener);
        btnTech.setOnClickListener(categoryClickListener);
        btnSports.setOnClickListener(categoryClickListener);

        btnHowItWorks.setOnClickListener(v -> navigateToGuidelines());

        btnFilterToday.setOnClickListener(v -> toggleDateFilter());

        scanButton.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(requireActivity());
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Scan Event QR Code");
            integrator.setCameraId(0);
            integrator.setBeepEnabled(true);
            integrator.setBarcodeImageEnabled(true);

            qrCodeLauncher.launch(integrator.createScanIntent());
        });

        loadEvents();
    }

    /**
     * Called when the fragment is visible to the user.
     * Refreshing the adapter here ensures that if the user joined a waitlist
     * in the EventDetailsActivity and then pressed "Back", the button state
     * (Green/Red) on the home screen updates immediately.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (eventAdapter != null) {
            eventAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Implements the toggle logic for the "Today's Events" date filter.
     * Toggles the filter state and updates the button's appearance.
     */
    private void toggleDateFilter() {
        if (getContext() == null) return;
        isTodayFilterActive = !isTodayFilterActive;

        if (isTodayFilterActive) {
            btnFilterToday.setText("Show All Dates");
            btnFilterToday.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            btnFilterToday.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.FCblue));
            eventAdapter.setDateFilter("TODAY");
        } else {
            btnFilterToday.setText("Today's Events");
            btnFilterToday.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            btnFilterToday.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.transparent));
            eventAdapter.setDateFilter("ALL");
        }
    }

    /**
     * Helper method to update the appearance of a category button for visual feedback.
     */
    private void updateCategoryButtonAppearance(Button button, boolean isSelected) {
        if (getContext() == null || button == null) return;

        if (isSelected) {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), R.color.FCblue));
        } else {
            button.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
            button.setBackgroundTintList(ContextCompat.getColorStateList(getContext(), android.R.color.transparent));
        }
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
     * and populates the RecyclerView. (Now uses a real-time listener for US 01.01.03)
     */
    private void loadEvents() {
        showLoading(true);

        eventListenerRegistration = eventRepository.getAllEvents(new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                showLoading(false);
                if (events.isEmpty()) {
                    showEmptyView(true);
                } else {
                    showEmptyView(false);
                    eventAdapter.updateBaseEventsAndRefilter(events);
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
     * Lifecycle method to remove the real-time listener when the view is destroyed.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventListenerRegistration != null) {
            eventListenerRegistration.remove();
            Log.d(TAG, "Firestore listener removed.");
        }
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