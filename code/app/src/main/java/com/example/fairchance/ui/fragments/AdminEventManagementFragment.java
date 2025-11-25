package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

public class AdminEventManagementFragment extends Fragment {

    private AdminEventAdapter adapter;
    private EventRepository repo;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.admin_event_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repo = new EventRepository();

        RecyclerView rv = view.findViewById(R.id.rvAdminEvents);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminEventAdapter();
        rv.setAdapter(adapter);

        // Back button
        View btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Listen to all events in real time
        registration = repo.listenToAllEventsForAdmin(new EventRepository.EventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                adapter.setEvents(events);
            }

            @Override
            public void onError(String message) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading events: " + message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
        }
    }
}
