package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.R;
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChosenEntrantsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";

    private String eventId;
    private FirebaseFunctions functions;

    private RecyclerView rvChosenEntrants;
    private SelectedParticipantAdapter adapter;

    private final List<String> chosenIds = new ArrayList<>();

    public ChosenEntrantsFragment() {}

    public static ChosenEntrantsFragment newInstance(String eventId) {
        ChosenEntrantsFragment f = new ChosenEntrantsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chosen_entrants_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        functions = FirebaseFunctions.getInstance();

        rvChosenEntrants = view.findViewById(R.id.rvChosenEntrants);
        rvChosenEntrants.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SelectedParticipantAdapter(chosenIds);
        rvChosenEntrants.setAdapter(adapter);

        Button btnSendNotification = view.findViewById(R.id.btnSendNotification);
        btnSendNotification.setOnClickListener(v -> onSendNotificationsClicked());

        loadChosenEntrants();
    }

    private void loadChosenEntrants() {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(this::onChosenLoaded)
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Failed to load chosen entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void onChosenLoaded(QuerySnapshot snap) {
        chosenIds.clear();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            chosenIds.add(doc.getId());
        }
        adapter.notifyDataSetChanged();
    }

    private void onSendNotificationsClicked() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event ID found for notifications.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);

        Task<Object> task = functions
                .getHttpsCallable("sendChosenNotifications")
                .call(data)
                .continueWith(t -> t.getResult() != null ? t.getResult().getData() : null);

        task.addOnSuccessListener(resultObj -> {
            if (getContext() == null) return;

            long sent = 0;
            long failed = 0;

            if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> res = (Map<String, Object>) resultObj;
                if (res.get("sentCount") instanceof Number)
                    sent = ((Number) res.get("sentCount")).longValue();
                if (res.get("failureCount") instanceof Number)
                    failed = ((Number) res.get("failureCount")).longValue();
            }

            String msg = "Notifications sent: " + sent + " success, " + failed + " failed.";
            Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

            loadChosenEntrants();
        });

        task.addOnFailureListener(e -> {
            if (getContext() == null) return;
            Toast.makeText(getContext(),
                    "Failed to send notifications: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        });
    }
}
