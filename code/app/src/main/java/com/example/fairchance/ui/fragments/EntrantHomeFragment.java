package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar; // Import ProgressBar
import android.widget.TextView; // Import TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.adapters.EventAdapter;

import java.util.ArrayList;
import java.util.List;

public class EntrantHomeFragment extends Fragment {

    private static final String TAG = "EntrantHomeFragment";

    private RecyclerView eventsRecyclerView;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();
    private EventRepository eventRepository;

    // Add ProgressBar and EmptyText
    private ProgressBar progressBar;
    private TextView emptyView;

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

        // Find the RecyclerView and new views
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);

        // Find ProgressBar and EmptyView from the layout (We need to add them)
        // progressBar = view.findViewById(R.id.progress_bar);
        // emptyView = view.findViewById(R.id.empty_view);

        // Setup RecyclerView
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(eventList);
        eventsRecyclerView.setAdapter(eventAdapter);

        // Fetch events from Firestore
        loadEvents();
    }

    private void loadEvents() {
        // showLoading(true);
        eventRepository.getAllEvents(new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                // showLoading(false);
                if (events.isEmpty()) {
                    // showEmptyView(true);
                } else {
                    // showEmptyView(false);
                    eventList.clear();
                    eventList.addAll(events);
                    eventAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String message) {
                // showLoading(false);
                Log.e(TAG, "Error loading events: " + message);
                Toast.makeText(getContext(), "Failed to load events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // TODO: Add showLoading() and showEmptyView() helper methods
    // private void showLoading(boolean isLoading) { ... }
    // private void showEmptyView(boolean show) { ... }
}