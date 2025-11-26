package com.example.fairchance.ui.fragments;

import android.app.AlertDialog;
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

import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class EntrantsWaitingListFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";
    private static final String ARG_EVENT_NAME = "EVENT_NAME";

    private String eventId;
    private String eventName;

    private RecyclerView rvWaitingList;
    private Button btnSendNotification;
    private Button btnViewWaitingListMap;   // for later user story

    private SelectedParticipantAdapter adapter;
    private final List<String> waitingIds = new ArrayList<>();

    private EventRepository repository;

    public EntrantsWaitingListFragment() { }

    public static EntrantsWaitingListFragment newInstance(String eventId, String eventName) {
        EntrantsWaitingListFragment fragment = new EntrantsWaitingListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        args.putString(ARG_EVENT_NAME, eventName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.entrants_waiting_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
            eventName = getArguments().getString(ARG_EVENT_NAME, "");
        }

        repository = new EventRepository();

        rvWaitingList = view.findViewById(R.id.rvWaitingList);
        btnSendNotification = view.findViewById(R.id.btnSendNotification);
        btnViewWaitingListMap = view.findViewById(R.id.btnViewWaitingListMap);

        rvWaitingList.setLayoutManager(new LinearLayoutManager(getContext()));

        // Reuse SelectedParticipantAdapter, with no per-entrant button
        adapter = new SelectedParticipantAdapter(
                waitingIds,
                eventName,
                "",          // button text (unused)
                false,       // showButton flag – adapter hides it when listener == null
                null         // listener == null → hide button
        );
        rvWaitingList.setAdapter(adapter);

        loadWaitingList();

        btnSendNotification.setOnClickListener(v -> showWaitingNotificationDialog());

        // btnViewWaitingListMap will be wired up for the map user story later
    }

    /**
     * Loads all entrants from events/{eventId}/waitingList and shows their IDs.
     */
    private void loadWaitingList() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event id provided for waiting list.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(snapshot -> {
                    waitingIds.clear();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        // waitingList doc ID is the userId
                        waitingIds.add(doc.getId());
                    }
                    adapter.setEventName(eventName);  // in case it was empty earlier
                    adapter.notifyDataSetChanged();

                    if (waitingIds.isEmpty()) {
                        Toast.makeText(getContext(),
                                "No entrants on the waiting list yet.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Failed to load waiting list: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Shows dialog_send_notif_waiting so the organizer can type a custom message,
     * then calls EventRepository.sendWaitingListNotifications.
     */
    private void showWaitingNotificationDialog() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "Event not loaded.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_send_notif_waiting, null, false);

        TextInputEditText etMessage = dialogView.findViewById(R.id.etNotificationText);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String message = etMessage.getText() != null
                    ? etMessage.getText().toString().trim()
                    : "";

            if (message.isEmpty()) {
                etMessage.setError("Message cannot be empty");
                return;
            }

            dialog.dismiss();

            repository.sendWaitingListNotifications(
                    eventId,
                    message,
                    new EventRepository.NotificationCallback() {
                        @Override
                        public void onSuccess(int sentCount, int failureCount) {
                            Toast.makeText(
                                    getContext(),
                                    "Notifications sent: " + sentCount +
                                            " (failures: " + failureCount + ")",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(
                                    getContext(),
                                    "Error sending notifications: " + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        });

        dialog.show();
    }
}
