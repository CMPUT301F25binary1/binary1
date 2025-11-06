package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.models.EventHistoryItem;
import com.example.fairchance.ui.adapters.EventHistoryAdapter;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private RecyclerView historyRecyclerView;
    private EventHistoryAdapter historyAdapter;
    private List<EventHistoryItem> historyList = new ArrayList<>();
    private EventRepository eventRepository;

    private ProgressBar progressBar;
    private TextView emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.entrant_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();

        // Find views
        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);

        // Setup RecyclerView
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new EventHistoryAdapter(getContext(), historyList);
        historyRecyclerView.setAdapter(historyAdapter);

        // Load data from Firestore
        loadHistory();
    }

    private void loadHistory() {
        showLoading(true);
        eventRepository.getEventHistory(new EventRepository.EventHistoryListCallback() {
            @Override
            public void onSuccess(List<EventHistoryItem> items) {
                showLoading(false);
                if (items.isEmpty()) {
                    showEmptyView(true);
                } else {
                    showEmptyView(false);
                    historyList.clear();
                    historyList.addAll(items);
                    historyAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Log.e(TAG, "Error loading history: " + message);
                Toast.makeText(getContext(), "Failed to load history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void showEmptyView(boolean show) {
        if (show) {
            emptyView.setVisibility(View.VISIBLE);
            historyRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}