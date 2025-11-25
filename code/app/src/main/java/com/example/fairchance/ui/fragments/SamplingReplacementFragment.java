package com.example.fairchance.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fairchance.EventRepository;
import com.example.fairchance.R;
import com.example.fairchance.ui.adapters.ReplacementPoolAdapter;
import com.example.fairchance.ui.adapters.SelectedParticipantAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen where the organizer can sample N attendees for a specific event
 * and draw replacements for cancelled entrants.
 */
public class SamplingReplacementFragment extends Fragment {

    private static final String ARG_EVENT_ID = "EVENT_ID";

    private EventRepository repository;
    private String eventId;

    private TextInputLayout tilSampleNumber;
    private TextInputEditText etSampleNumber;
    private Button btnSampleAttendees;

    private RecyclerView rvSelectedParticipants;
    private SelectedParticipantAdapter selectedAdapter;
    private final List<String> selectedIds = new ArrayList<>();

    private RecyclerView rvReplacementPool;
    private ReplacementPoolAdapter replacementAdapter;
    private final List<String> cancelledIds = new ArrayList<>();

    private TextView tvSummary;

    public SamplingReplacementFragment() {
    }

    public static SamplingReplacementFragment newInstance(String eventId) {
        SamplingReplacementFragment fragment = new SamplingReplacementFragment();
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
        return inflater.inflate(R.layout.sampling_replacement, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new EventRepository();

        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }

        tilSampleNumber = view.findViewById(R.id.tilSampleNumber);
        etSampleNumber = view.findViewById(R.id.etSampleNumber);
        btnSampleAttendees = view.findViewById(R.id.btnSampleAttendees);
        tvSummary = view.findViewById(R.id.tvSummary);

        // Selected participants list
        rvSelectedParticipants = view.findViewById(R.id.rvSelectedParticipants);
        rvSelectedParticipants.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedAdapter = new SelectedParticipantAdapter(selectedIds);
        rvSelectedParticipants.setAdapter(selectedAdapter);

        // Replacement pool list
        rvReplacementPool = view.findViewById(R.id.rvReplacementPool);
        rvReplacementPool.setLayoutManager(new LinearLayoutManager(getContext()));
        replacementAdapter = new ReplacementPoolAdapter(
                cancelledIds,
                this::onDrawReplacementClicked
        );
        rvReplacementPool.setAdapter(replacementAdapter);

        btnSampleAttendees.setOnClickListener(v -> onSampleClicked());

        if (eventId != null && !eventId.isEmpty()) {
            loadSelectedParticipants(0);
            loadReplacementPool();
        }
    }

    private void onSampleClicked() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(),
                    "No event selected for sampling.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        tilSampleNumber.setError(null);

        String text = etSampleNumber.getText() != null
                ? etSampleNumber.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(text)) {
            tilSampleNumber.setError("Please enter a number.");
            return;
        }

        int requested;
        try {
            requested = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            tilSampleNumber.setError("Invalid number.");
            return;
        }

        if (requested <= 0) {
            tilSampleNumber.setError("Number must be greater than 0.");
            return;
        }

        repository.sampleAttendees(eventId, requested,
                new EventRepository.EventTaskCallback() {
                    @Override
                    public void onSuccess() {
                        if (getContext() == null) return;
                        loadSelectedParticipants(requested);
                        loadReplacementPool();
                    }

                    @Override
                    public void onError(String message) {
                        if (getContext() == null) return;
                        Toast.makeText(getContext(),
                                "Sampling failed: " + message,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loadSelectedParticipants(int requested) {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("selected")
                .get()
                .addOnSuccessListener(query -> {
                    if (getContext() == null) return;

                    selectedIds.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        selectedIds.add(doc.getId());
                    }
                    selectedAdapter.notifyDataSetChanged();

                    int selectedCount = selectedIds.size();

                    if (requested > 0) {
                        String msg = "Selected " + selectedCount +
                                " entrant(s) (requested " + requested + ").";
                        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

                        tvSummary.setText("Last draw: requested " + requested +
                                ", selected " + selectedCount + " entrant(s).");
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Failed to load selected entrants: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadReplacementPool() {
        if (eventId == null || eventId.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("selected")
                .whereEqualTo("status", "cancelled")
                .get()
                .addOnSuccessListener(query -> {
                    if (getContext() == null) return;

                    cancelledIds.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        cancelledIds.add(doc.getId());
                    }
                    replacementAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(),
                            "Failed to load replacement pool: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void onDrawReplacementClicked(String cancelledUserId) {
        if (eventId == null || eventId.isEmpty()) return;

        repository.drawReplacement(eventId, new EventRepository.EventTaskCallback() {
            @Override
            public void onSuccess() {
                if (getContext() == null) return;

                Toast.makeText(getContext(),
                        "Replacement entrant selected.",
                        Toast.LENGTH_LONG).show();

                loadSelectedParticipants(0);
                loadReplacementPool();
            }

            @Override
            public void onError(String message) {
                if (getContext() == null) return;
                Toast.makeText(getContext(),
                        "Replacement draw failed: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
