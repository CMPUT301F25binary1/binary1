package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.adapters.AdminEventAdapter;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for administrators to browse and delete events.
 * Supports searching/filtering events.
 */
public class AdminEventManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private EditText etSearch;

    private AdminEventAdapter adapter;
    private final List<Event> fullList = new ArrayList<>();
    private final EventRepository repo = new EventRepository();
    private ListenerRegistration eventsRegistration;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_event_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.rvAdminEvents);
        progressBar = view.findViewById(R.id.progressBarEvents);
        tvEmpty = view.findViewById(R.id.tvEmptyEvents);
        etSearch = view.findViewById(R.id.etSearchEvents);

        adapter = new AdminEventAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        startListeningForEvents();
    }

    private void startListeningForEvents() {
        setLoading(true);
        eventsRegistration = repo.listenToAllEvents(new EventRepository.AdminEventsListener() {
            @Override
            public void onEventsChanged(List<Event> events) {
                setLoading(false);

                fullList.clear();
                if (events != null) {
                    fullList.addAll(events);
                }

                applyFilter(etSearch.getText().toString());
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(requireContext(),
                        "Error loading events: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyFilter(String query) {
        List<Event> filtered = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());

        for (Event e : fullList) {
            String name = safe(e.getName());
            String organizer = safe(e.getOrganizerId());
            String dateStr = "";

            if (e.getEventDate() != null) {
                dateStr = dateFormat.format(e.getEventDate());
            }

            boolean matches =
                    name.contains(q) ||
                            organizer.contains(q) ||
                            dateStr.contains(q);

            if (q.isEmpty() || matches) {
                filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
        }

        adapter.setEvents(filtered);
    }

    private String safe(String s) {
        return s == null ? "" : s.toLowerCase(Locale.getDefault());
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsRegistration != null) {
            eventsRegistration.remove();
            eventsRegistration = null;
        }
    }
}
