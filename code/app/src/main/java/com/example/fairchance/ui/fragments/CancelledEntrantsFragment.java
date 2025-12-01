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
import com.example.fairchance.models.Event;
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CancelledEntrantsFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";

    private String eventId;
    private EventRepository repository;

    private RecyclerView rvCancelledEntrants;
    private SelectedParticipantAdapter adapter;
    private final List<String> cancelledIds = new ArrayList<>();

    public CancelledEntrantsFragment() {
        // Required empty constructor
    }

    public static CancelledEntrantsFragment newInstance(String eventId) {
        CancelledEntrantsFragment fragment = new CancelledEntrantsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cancelled_entrants_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        rvCancelledEntrants = view.findViewById(R.id.rvCancelledEntrants);
        rvCancelledEntrants.setLayoutManager(new LinearLayoutManager(getContext()));

        // Re-use SelectedParticipantAdapter with no per-row button
        adapter = new SelectedParticipantAdapter(
                cancelledIds,
                "",          // event name not needed here
                "",          // button text unused when listener == null
                false,
                null         // listener == null → button hidden
        );
        rvCancelledEntrants.setAdapter(adapter);

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Missing event id.", Toast.LENGTH_SHORT).show();
        } else {
            loadEventMeta();          // NEW – loads event name
            loadCancelledEntrants();  // already there
        }

        // Make sure your layout has a Button with this id:
        // @+id/btnNotifyCancelledEntrants
        Button btnNotify = view.findViewById(R.id.btnNotifyCancelledEntrants);
        if (btnNotify != null) {
            btnNotify.setOnClickListener(v -> {
                if (eventId == null || eventId.isEmpty()) {
                    Toast.makeText(getContext(), "Event not loaded.", Toast.LENGTH_SHORT).show();
                    return;
                }
                showSendNotificationDialog();
            });
        }
    }

    private void loadEventMeta() {
        repository.getEvent(eventId, new EventRepository.EventCallback() {
            @Override
            public void onSuccess(Event event) {
                // Set list title to include event name (optional)
                View root = getView();
                if (root != null) {
                    android.widget.TextView title = root.findViewById(R.id.tvCancelledEntrantsTitle);
                    if (title != null && event.getName() != null) {
                        title.setText(event.getName() + " – Cancelled Entrants' List");
                    }
                }

                // Tell the adapter so each card shows the event name
                if (event.getName() != null) {
                    adapter.setEventName(event.getName());
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(getContext(),
                        "Failed to load event: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadCancelledEntrants() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .document(eventId)
                .collection("cancelled")
                .get()
                .addOnSuccessListener(snapshot -> {
                    cancelledIds.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        // doc ID is the userId
                        cancelledIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                getContext(),
                                "Failed to load cancelled entrants: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showSendNotificationDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_send_notif_cancelled, null, false);

        TextInputEditText etMessage = dialogView.findViewById(R.id.etNotificationText);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnConfirm.setOnClickListener(v -> {
            String message = etMessage != null ? etMessage.getText().toString().trim() : "";
            if (message.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a message.", Toast.LENGTH_SHORT).show();
                return;
            }

            repository.sendCancelledNotifications(
                    eventId,
                    message,
                    new EventRepository.NotificationCallback() {
                        @Override
                        public void onSuccess(int sentCount, int failureCount) {
                            Toast.makeText(
                                    getContext(),
                                    "Sent to " + sentCount + " entrant(s). Failures: " + failureCount,
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(
                                    getContext(),
                                    "Error: " + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });

            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
